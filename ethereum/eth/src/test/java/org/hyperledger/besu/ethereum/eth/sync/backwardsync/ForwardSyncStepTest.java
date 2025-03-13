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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.BlockProcessingOutputs;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.manager.peertask.task.GetBodiesFromPeerTask;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.referencetests.ForestReferenceTestWorldState;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForwardSyncStepTest {

  public static final int REMOTE_HEIGHT = 50;
  public static final int LOCAL_HEIGHT = 25;
  private static final BlockDataGenerator blockDataGenerator = new BlockDataGenerator();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private BackwardSyncContext context;

  @Mock private SynchronizerConfiguration syncConfig;
  @Mock private PeerTaskExecutor peerTaskExecutor;

  private MutableBlockchain remoteBlockchain;
  private RespondingEthPeer peer;

  private final ProtocolSchedule protocolSchedule =
      MainnetProtocolSchedule.fromConfig(
          new StubGenesisConfigOptions(),
          MiningConfiguration.MINING_DISABLED,
          new BadBlockManager(),
          false,
          new NoOpMetricsSystem());
  private MutableBlockchain localBlockchain;
  GenericKeyValueStorageFacade<Hash, BlockHeader> headersStorage;
  GenericKeyValueStorageFacade<Hash, Block> blocksStorage;
  GenericKeyValueStorageFacade<Hash, Hash> chainStorage;
  GenericKeyValueStorageFacade<String, BlockHeader> sessionDataStorage;

  @BeforeEach
  public void setup() {
    headersStorage =
        new GenericKeyValueStorageFacade<>(
            Hash::toArrayUnsafe,
            new BlocksHeadersConvertor(new MainnetBlockHeaderFunctions()),
            new InMemoryKeyValueStorage());
    blocksStorage =
        new GenericKeyValueStorageFacade<>(
            Hash::toArrayUnsafe,
            new BlocksConvertor(new MainnetBlockHeaderFunctions()),
            new InMemoryKeyValueStorage());
    chainStorage =
        new GenericKeyValueStorageFacade<>(
            Hash::toArrayUnsafe, new HashConvertor(), new InMemoryKeyValueStorage());
    sessionDataStorage =
        new GenericKeyValueStorageFacade<>(
            key -> key.getBytes(StandardCharsets.UTF_8),
            new BlocksHeadersConvertor(new MainnetBlockHeaderFunctions()),
            new InMemoryKeyValueStorage());

    Block genesisBlock = blockDataGenerator.genesisBlock();
    remoteBlockchain = createInMemoryBlockchain(genesisBlock);
    localBlockchain = createInMemoryBlockchain(genesisBlock);

    for (int i = 1; i <= REMOTE_HEIGHT; i++) {
      final BlockDataGenerator.BlockOptions options =
          new BlockDataGenerator.BlockOptions()
              .setBlockNumber(i)
              .setParentHash(remoteBlockchain.getBlockHashByNumber(i - 1).orElseThrow());
      final Block block = blockDataGenerator.block(options);
      final List<TransactionReceipt> receipts = blockDataGenerator.receipts(block);

      remoteBlockchain.appendBlock(block, receipts);
      if (i <= LOCAL_HEIGHT) {
        localBlockchain.appendBlock(block, receipts);
      }
    }

    when(context.getProtocolContext().getBlockchain()).thenReturn(localBlockchain);
    when(context.getProtocolSchedule()).thenReturn(protocolSchedule);
    when(context.getBatchSize()).thenReturn(2);
    when(context.getSynchronizerConfiguration()).thenReturn(syncConfig);
    EthProtocolManager ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setSynchronizerConfiguration(syncConfig)
            .setPeerTaskExecutor(peerTaskExecutor)
            .build();

    peer = EthProtocolManagerTestUtil.createPeer(ethProtocolManager);
    EthContext ethContext = ethProtocolManager.ethContext();
    when(context.getEthContext()).thenReturn(ethContext);

    when(context
            .getBlockValidatorForBlock(any())
            .validateAndProcessBlock(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              final Object[] arguments = invocation.getArguments();
              Block block = (Block) arguments[1];
              return new BlockProcessingResult(
                  Optional.of(
                      new BlockProcessingOutputs(
                          ForestReferenceTestWorldState.create(Collections.emptyMap()),
                          blockDataGenerator.receipts(block))));
            });

    when(peerTaskExecutor.execute(any(GetBodiesFromPeerTask.class)))
        .thenAnswer(
            (invocationOnMock) -> {
              GetBodiesFromPeerTask task =
                  invocationOnMock.getArgument(0, GetBodiesFromPeerTask.class);
              List<Block> blocks =
                  task.getBlockHeaders().stream()
                      .map(
                          (bh) ->
                              new Block(bh, remoteBlockchain.getBlockBody(bh.getBlockHash()).get()))
                      .collect(Collectors.toList());
              return new PeerTaskExecutorResult<List<Block>>(
                  Optional.of(blocks),
                  PeerTaskExecutorResponseCode.SUCCESS,
                  Optional.of(peer.getEthPeer()));
            });
  }

  @Test
  public void shouldExecuteForwardSyncWhenPossible() throws Exception {
    when(syncConfig.isPeerTaskSystemEnabled()).thenReturn(false);
    final BackwardChain backwardChain = createBackwardChain(LOCAL_HEIGHT, LOCAL_HEIGHT + 3);
    ForwardSyncStep step = new ForwardSyncStep(context, backwardChain);

    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(remoteBlockchain);

    final CompletableFuture<Void> completableFuture = step.executeAsync();

    peer.respondWhile(
        responder,
        () -> {
          try {
            Thread.sleep(5);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          return !completableFuture.isDone();
        });

    completableFuture.get();
  }

  @Test
  public void shouldExecuteForwardSyncWhenPossibleUsingPeerTaskSystem() throws Exception {
    when(syncConfig.isPeerTaskSystemEnabled()).thenReturn(true);
    final BackwardChain backwardChain = createBackwardChain(LOCAL_HEIGHT, LOCAL_HEIGHT + 3);
    ForwardSyncStep step = new ForwardSyncStep(context, backwardChain);

    final CompletableFuture<Void> completableFuture = step.executeAsync();

    completableFuture.get();
  }

  @Test
  public void shouldNotRequestWhenNull() {
    ForwardSyncStep phase = new ForwardSyncStep(context, null);
    final CompletableFuture<Void> completableFuture =
        phase.possibleRequestBodies(Collections.emptyList());
    assertThat(completableFuture.isDone()).isTrue();

    final CompletableFuture<Void> completableFuture1 =
        phase.possibleRequestBodies(Collections.emptyList());
    assertThat(completableFuture1.isDone()).isTrue();
  }

  @Test
  public void shouldFindBlockWhenRequested() throws Exception {
    ForwardSyncStep step =
        new ForwardSyncStep(context, createBackwardChain(LOCAL_HEIGHT + 1, LOCAL_HEIGHT + 3));

    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(remoteBlockchain);

    final CompletableFuture<List<Block>> future =
        step.requestBodies(List.of(getBlockByNumber(LOCAL_HEIGHT + 1).getHeader()));
    peer.respondWhile(responder, () -> !future.isDone());
    final List<Block> blocks = future.get();
    Assertions.assertThat(blocks)
        .hasSize(1)
        .containsExactlyInAnyOrder(getBlockByNumber(LOCAL_HEIGHT + 1));
  }

  @Test
  public void shouldFindBlockWhenRequestedUsingPeerTaskSystem() throws Exception {
    when(syncConfig.isPeerTaskSystemEnabled()).thenReturn(true);
    ForwardSyncStep step =
        new ForwardSyncStep(context, createBackwardChain(LOCAL_HEIGHT + 1, LOCAL_HEIGHT + 3));

    final CompletableFuture<List<Block>> future =
        step.requestBodies(List.of(getBlockByNumber(LOCAL_HEIGHT + 1).getHeader()));
    final List<Block> blocks = future.get();
    Assertions.assertThat(blocks)
        .hasSize(1)
        .containsExactlyInAnyOrder(getBlockByNumber(LOCAL_HEIGHT + 1));
  }

  private BackwardChain createBackwardChain(final int from, final int until) {
    BackwardChain chain = backwardChainFromBlock(until);
    for (int i = until; i > from; --i) {
      chain.prependAncestorsHeader(getBlockByNumber(i - 1).getHeader());
    }
    return chain;
  }

  @Nonnull
  private BackwardChain backwardChainFromBlock(final int number) {
    final BackwardChain backwardChain =
        new BackwardChain(headersStorage, blocksStorage, chainStorage, sessionDataStorage);
    backwardChain.appendTrustedBlock(remoteBlockchain.getBlockByNumber(number).orElseThrow());
    return backwardChain;
  }

  @Nonnull
  private Block getBlockByNumber(final int number) {
    return remoteBlockchain.getBlockByNumber(number).orElseThrow();
  }
}
