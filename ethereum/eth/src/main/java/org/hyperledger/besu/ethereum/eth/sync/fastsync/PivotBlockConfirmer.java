/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.idnecology.idn.ethereum.eth.sync.fastsync;

import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.manager.peertask.task.GetHeadersFromPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.EthTask;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.tasks.RetryingGetHeaderFromPeerByNumberTask;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.util.FutureUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task will query {@code numberOfPeersToQuery} peers for a particular block number. If any
 * peers disagree on the block at this number, the task fails with a {@code
 * ContestedPivotBlockException}. The task will succeed only if {@code numberOfPeersToQuery}
 * distinct peers all return matching block headers for the specified block number.
 */
class PivotBlockConfirmer {
  private static final Logger LOG = LoggerFactory.getLogger(PivotBlockConfirmer.class);

  private final EthContext ethContext;
  private final MetricsSystem metricsSystem;
  private final ProtocolSchedule protocolSchedule;
  private final SynchronizerConfiguration synchronizerConfiguration;

  // The number of peers we need to query to confirm our pivot block
  private final int numberOfPeersToQuery;
  // The current pivot block number, gets pushed back if peers disagree on the pivot block
  private final long pivotBlockNumber;
  // The number of times to retry if a peer fails to return an answer to our query
  private final int numberOfRetriesPerPeer;

  private final CompletableFuture<FastSyncState> result = new CompletableFuture<>();
  private final Collection<CompletableFuture<?>> runningQueries = new ConcurrentLinkedQueue<>();
  private final Map<Bytes, RetryingGetHeaderFromPeerByNumberTask> pivotBlockQueriesByPeerId =
      new ConcurrentHashMap<>();
  private final Set<Bytes> peerIdsUsed = Collections.synchronizedSet(new HashSet<>());
  private final Map<BlockHeader, AtomicInteger> pivotBlockVotes = new ConcurrentHashMap<>();

  private final AtomicBoolean isStarted = new AtomicBoolean(false);
  private final AtomicBoolean isCancelled = new AtomicBoolean(false);

  PivotBlockConfirmer(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final MetricsSystem metricsSystem,
      final SynchronizerConfiguration synchronizerConfiguration,
      final long pivotBlockNumber,
      final int numberOfPeersToQuery,
      final int numberOfRetriesPerPeer) {
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.metricsSystem = metricsSystem;
    this.synchronizerConfiguration = synchronizerConfiguration;
    this.pivotBlockNumber = pivotBlockNumber;
    this.numberOfPeersToQuery = numberOfPeersToQuery;
    this.numberOfRetriesPerPeer = numberOfRetriesPerPeer;
  }

  public CompletableFuture<FastSyncState> confirmPivotBlock() {
    if (isStarted.compareAndSet(false, true)) {
      LOG.info(
          "Confirm pivot block {} with at least {} peers.", pivotBlockNumber, numberOfPeersToQuery);
      queryPeers(pivotBlockNumber);
    }

    return result;
  }

  private void queryPeers(final long blockNumber) {
    synchronized (runningQueries) {
      for (int i = 0; i < numberOfPeersToQuery; i++) {
        final CompletableFuture<?> query;
        if (synchronizerConfiguration.isPeerTaskSystemEnabled()) {
          query =
              executePivotQueryWithPeerTaskSystem(blockNumber)
                  .whenComplete(this::processReceivedHeader);
        } else {
          query = executePivotQuery(blockNumber).whenComplete(this::processReceivedHeader);
        }
        runningQueries.add(query);
      }
    }
  }

  private void processReceivedHeader(final BlockHeader blockHeader, final Throwable throwable) {
    if (throwable != null) {
      cancelQueries();
      LOG.error("Encountered error while requesting pivot block header", throwable);
      result.completeExceptionally(throwable);
      return;
    }

    // Update votes
    pivotBlockVotes.putIfAbsent(blockHeader, new AtomicInteger(0));
    final int votes = pivotBlockVotes.get(blockHeader).incrementAndGet();

    if (pivotBlockVotes.keySet().size() > 1) {
      // We've received conflicting signals for the target block, confirmation has failed
      cancelQueries();
      LOG.info(
          "Failed to confirm pivot block {}. Received conflicting headers: {}",
          pivotBlockNumber,
          votesToString());
      result.completeExceptionally(
          new ContestedPivotBlockException(pivotBlockNumber, votesToString()));
    } else if (votes >= numberOfPeersToQuery) {
      // We've received the required number of votes and have selected our pivot block
      LOG.info("Confirmed pivot block at {}: {}", pivotBlockNumber, blockHeader.getHash());
      result.complete(new FastSyncState(blockHeader));
    } else {
      LOG.info(
          "Received {} confirmation(s) for pivot block header {}: {}",
          votes,
          pivotBlockNumber,
          blockHeader.getHash());
    }
  }

  private void cancelQueries() {
    synchronized (runningQueries) {
      isCancelled.set(true);
      runningQueries.forEach(f -> f.cancel(true));
      pivotBlockQueriesByPeerId.values().forEach(EthTask::cancel);
    }
  }

  private String votesToString() {
    return pivotBlockVotes.entrySet().stream()
        .map(e -> e.getKey().getHash() + " (" + e.getValue().get() + ")")
        .collect(Collectors.joining(","));
  }

  private CompletableFuture<BlockHeader> executePivotQuery(final long blockNumber) {
    if (isCancelled.get() || result.isDone()) {
      // Stop loop if this task is done
      return CompletableFuture.failedFuture(new CancellationException());
    }
    final Optional<RetryingGetHeaderFromPeerByNumberTask> query = createPivotQuery(blockNumber);
    final CompletableFuture<BlockHeader> pivotHeaderFuture;
    if (query.isPresent()) {
      final CompletableFuture<BlockHeader> headerQuery = query.get().getHeader();
      pivotHeaderFuture =
          FutureUtils.exceptionallyCompose(headerQuery, (error) -> executePivotQuery(blockNumber));
    } else {
      // We were unable to find a peer to query, wait and try again
      LOG.debug("No peer currently available to query for block {}.", blockNumber);
      pivotHeaderFuture =
          ethContext
              .getScheduler()
              .scheduleFutureTask(
                  () ->
                      ethContext
                          .getEthPeers()
                          .waitForPeer(
                              (peer) -> !pivotBlockQueriesByPeerId.containsKey(peer.nodeId()))
                          // Ignore result, ensure even a timeout will result in calling
                          // executePivotQuery
                          .handle((r, e) -> null)
                          .thenCompose(res -> executePivotQuery(blockNumber)),
                  Duration.ofSeconds(5));
    }

    return pivotHeaderFuture;
  }

  private CompletableFuture<BlockHeader> executePivotQueryWithPeerTaskSystem(
      final long blockNumber) {
    GetHeadersFromPeerTask task =
        new GetHeadersFromPeerTask(
            blockNumber, 1, 0, GetHeadersFromPeerTask.Direction.FORWARD, protocolSchedule);
    Optional<EthPeer> maybeEthPeer;
    final EthPeer ethPeer;
    try {
      do {
        if (isCancelled.get()) {
          return CompletableFuture.failedFuture(
              new CancellationException("Pivot block confirmation has been cancelled"));
        }
        maybeEthPeer =
            ethContext
                .getEthPeers()
                .getPeer(
                    (p) ->
                        task.getPeerRequirementFilter().test(p)
                            && !peerIdsUsed.contains(p.nodeId()));
      } while (maybeEthPeer.isEmpty() && waitForPeer());
      ethPeer = maybeEthPeer.get();
      peerIdsUsed.add(ethPeer.nodeId());
    } catch (InterruptedException e) {
      return CompletableFuture.failedFuture(e);
    }

    return ethContext
        .getScheduler()
        .scheduleServiceTask(
            () -> {
              PeerTaskExecutorResult<List<BlockHeader>> taskResult =
                  ethContext.getPeerTaskExecutor().executeAgainstPeer(task, ethPeer);
              if (taskResult.responseCode() == PeerTaskExecutorResponseCode.INTERNAL_SERVER_ERROR) {
                // something is probably wrong with the request, so we won't retry as below
                return CompletableFuture.failedFuture(
                    new RuntimeException("Unexpected internal issue"));
              } else if (taskResult.responseCode() != PeerTaskExecutorResponseCode.SUCCESS
                  || taskResult.result().isEmpty()) {
                // recursively call executePivotQueryWithPeerTaskSystem to retry with a different
                // peer.
                return executePivotQueryWithPeerTaskSystem(blockNumber);
              }
              return CompletableFuture.completedFuture(taskResult.result().get().getFirst());
            });
  }

  private boolean waitForPeer() throws InterruptedException {
    Thread.sleep(Duration.ofSeconds(5));
    return true;
  }

  private Optional<RetryingGetHeaderFromPeerByNumberTask> createPivotQuery(final long blockNumber) {
    return ethContext
        .getEthPeers()
        .streamBestPeers()
        .filter(p -> p.chainState().getEstimatedHeight() >= blockNumber)
        .filter(EthPeer::isFullyValidated)
        .filter(p -> !pivotBlockQueriesByPeerId.keySet().contains(p.nodeId()))
        .findFirst()
        .flatMap((peer) -> createGetHeaderTask(peer, blockNumber));
  }

  Optional<RetryingGetHeaderFromPeerByNumberTask> createGetHeaderTask(
      final EthPeer peer, final long blockNumber) {
    final RetryingGetHeaderFromPeerByNumberTask task =
        RetryingGetHeaderFromPeerByNumberTask.forSingleNumber(
            protocolSchedule, ethContext, metricsSystem, blockNumber, numberOfRetriesPerPeer);
    task.assignPeer(peer);

    // Try adding our task
    synchronized (runningQueries) {
      if (isCancelled.get()) {
        // Don't run a new query if this task is already cancelled
        return Optional.empty();
      }
      final RetryingGetHeaderFromPeerByNumberTask preexistingTask =
          pivotBlockQueriesByPeerId.putIfAbsent(peer.nodeId(), task);
      if (preexistingTask != null) {
        // We already have a task for this peer, try again later
        return Optional.empty();
      }
    }

    LOG.debug("Query peer {} for block {}.", peer.getLoggableId(), blockNumber);
    return Optional.of(task);
  }

  public static class ContestedPivotBlockException extends RuntimeException {

    private final long blockNumber;

    public ContestedPivotBlockException(final long blockNumber, final String message) {
      super(message);
      this.blockNumber = blockNumber;
    }

    public long getBlockNumber() {
      return blockNumber;
    }
  }
}
