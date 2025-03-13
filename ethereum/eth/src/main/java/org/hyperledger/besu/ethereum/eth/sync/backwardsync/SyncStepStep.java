/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.eth.sync.backwardsync;

import static org.slf4j.LoggerFactory.getLogger;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.eth.manager.task.AbstractPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.RetryingGetBlockFromPeersTask;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

public class SyncStepStep {
  private static final Logger LOG = getLogger(SyncStepStep.class);

  public static final int UNUSED = -1;
  private final BackwardSyncContext context;
  private final BackwardChain backwardChain;

  public SyncStepStep(final BackwardSyncContext context, final BackwardChain backwardChain) {
    this.context = context;
    this.backwardChain = backwardChain;
  }

  public CompletableFuture<Block> executeAsync(final Hash hash) {
    return CompletableFuture.supplyAsync(() -> hash)
        .thenCompose(this::requestBlock)
        .thenApply(this::saveBlock);
  }

  private CompletableFuture<Block> requestBlock(final Hash targetHash) {
    LOG.atDebug().setMessage("Fetching block by hash {} from peers").addArgument(targetHash).log();
    final RetryingGetBlockFromPeersTask getBlockTask =
        RetryingGetBlockFromPeersTask.create(
            context.getProtocolSchedule(),
            context.getEthContext(),
            context.getSynchronizerConfiguration(),
            context.getMetricsSystem(),
            context.getEthContext().getEthPeers().peerCount(),
            Optional.of(targetHash),
            UNUSED);
    return context
        .getEthContext()
        .getScheduler()
        .scheduleSyncWorkerTask(getBlockTask::run)
        .thenApply(AbstractPeerTask.PeerTaskResult::getResult);
  }

  private Block saveBlock(final Block block) {
    LOG.atDebug().setMessage("Appending fetched block {}").addArgument(block::toLogString).log();
    backwardChain.appendTrustedBlock(block);
    return block;
  }
}
