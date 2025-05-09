/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.consensus.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.merge.blockcreation.PayloadIdentifier;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockWithReceipts;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostMergeContextTest {

  @Mock private SyncState mockSyncState;

  private PostMergeContext postMergeContext;

  private MergeStateChangeCollector mergeStateChangeCollector;

  @BeforeEach
  public void setUp() {
    mergeStateChangeCollector = new MergeStateChangeCollector();
    postMergeContext = new PostMergeContext();
    postMergeContext.observeNewIsPostMergeState(mergeStateChangeCollector);
    postMergeContext.setSyncState(mockSyncState);
    postMergeContext.setTerminalTotalDifficulty(Difficulty.of(10L));
  }

  @Test
  public void switchFromPoWToPoSStopSyncAndCallsSubscribers() {
    when(mockSyncState.hasReachedTerminalDifficulty()).thenReturn(Optional.of(Boolean.TRUE));

    postMergeContext.setIsPostMerge(Difficulty.of(10L));

    verify(mockSyncState).setReachedTerminalDifficulty(true);
    assertThat(postMergeContext.isPostMerge()).isTrue();
    assertThat(postMergeContext.isSyncing()).isFalse();
    assertThat(mergeStateChangeCollector.stateChanges).containsExactly(true);
  }

  @Test
  public void setPrePoSStateNotStopSync() {
    when(mockSyncState.hasReachedTerminalDifficulty()).thenReturn(Optional.of(Boolean.FALSE));

    postMergeContext.setIsPostMerge(Difficulty.of(9L));

    verify(mockSyncState, never()).setReachedTerminalDifficulty(false);
    assertThat(postMergeContext.isPostMerge()).isFalse();
    assertThat(postMergeContext.isSyncing()).isTrue();
    assertThat(mergeStateChangeCollector.stateChanges).containsExactly(false);
  }

  @Test
  public void setPostPoWDifficultyAfterFinalizedBlockDoesNothing() {
    // first simulate a switch to PoS, and reset mocks and collectors
    postMergeContext.setIsPostMerge(Difficulty.of(11L));
    reset(mockSyncState);
    mergeStateChangeCollector.reset();

    // then perform the actual test
    BlockHeader mockFinalizedHeader = mock(BlockHeader.class);
    postMergeContext.setFinalized(mockFinalizedHeader);

    postMergeContext.setIsPostMerge(Difficulty.of(19L));

    verifyNoInteractions(mockSyncState);
    assertThat(postMergeContext.isPostMerge()).isTrue();
    assertThat(mergeStateChangeCollector.stateChanges).isEmpty();
  }

  @Test
  public void candidateHeadIsValidBeforeAnyFinalizedBlock() {
    assertThat(postMergeContext.getFinalized()).isEmpty();

    BlockHeader mockHeader = mock(BlockHeader.class);
    assertThat(postMergeContext.validateCandidateHead(mockHeader)).isTrue();
  }

  @Test
  public void candidateHeadIsValidIfAfterFinalizedBlock() {
    BlockHeader mockFinalizedHeader = mock(BlockHeader.class);
    when(mockFinalizedHeader.getNumber()).thenReturn(2L);
    postMergeContext.setFinalized(mockFinalizedHeader);

    BlockHeader mockHeader = mock(BlockHeader.class);
    when(mockHeader.getNumber()).thenReturn(3L);
    assertThat(postMergeContext.validateCandidateHead(mockHeader)).isTrue();
  }

  @Test
  public void candidateHeadIsInvalidIfBeforeFinalizedBlock() {
    BlockHeader mockFinalizedHeader = mock(BlockHeader.class);
    when(mockFinalizedHeader.getNumber()).thenReturn(3L);
    postMergeContext.setFinalized(mockFinalizedHeader);

    BlockHeader mockHeader = mock(BlockHeader.class);
    when(mockHeader.getNumber()).thenReturn(2L);
    assertThat(postMergeContext.validateCandidateHead(mockHeader)).isFalse();
  }

  @Test
  public void putAndRetrieveFirstPayload() {
    BlockWithReceipts mockBlockWithReceipts = createBlockWithReceipts(1, 21000, 1);

    PayloadIdentifier firstPayloadId = new PayloadIdentifier(1L);
    final var payloadWrapper = createPayloadWrapper(firstPayloadId, mockBlockWithReceipts, Wei.ONE);
    postMergeContext.putPayloadById(payloadWrapper);

    assertThat(postMergeContext.retrievePayloadById(firstPayloadId))
        .map(PayloadWrapper::blockWithReceipts)
        .contains(mockBlockWithReceipts);
  }

  @Test
  public void puttingTwoBlocksWithTheSamePayloadIdWeRetrieveTheBest() {
    BlockWithReceipts zeroTxBlockWithReceipts = createBlockWithReceipts(1, 0, 0);
    BlockWithReceipts betterBlockWithReceipts = createBlockWithReceipts(2, 11, 1);

    PayloadIdentifier payloadId = new PayloadIdentifier(1L);
    final var zeroTxPayloadWrapper =
        createPayloadWrapper(payloadId, zeroTxBlockWithReceipts, Wei.ZERO);
    final var betterPayloadWrapper =
        createPayloadWrapper(payloadId, betterBlockWithReceipts, Wei.ONE);
    postMergeContext.putPayloadById(zeroTxPayloadWrapper);
    postMergeContext.putPayloadById(betterPayloadWrapper);

    assertThat(postMergeContext.retrievePayloadById(payloadId))
        .map(PayloadWrapper::blockWithReceipts)
        .contains(betterBlockWithReceipts);
  }

  @Test
  public void puttingABlockWithTheSamePayloadIdSmallerThanAnExistingOneWeRetrieveTheBest() {
    BlockWithReceipts zeroTxBlockWithReceipts = createBlockWithReceipts(1, 0, 0);
    BlockWithReceipts betterBlockWithReceipts = createBlockWithReceipts(2, 11, 1);
    BlockWithReceipts smallBlockWithReceipts = createBlockWithReceipts(3, 5, 1);

    PayloadIdentifier payloadId = new PayloadIdentifier(1L);
    final var zeroTxPayloadWrapper =
        createPayloadWrapper(payloadId, zeroTxBlockWithReceipts, Wei.ZERO);
    final var betterPayloadWrapper =
        createPayloadWrapper(payloadId, betterBlockWithReceipts, Wei.of(2));
    final var smallPayloadWrapper =
        createPayloadWrapper(payloadId, smallBlockWithReceipts, Wei.ONE);
    postMergeContext.putPayloadById(zeroTxPayloadWrapper);
    postMergeContext.putPayloadById(betterPayloadWrapper);
    postMergeContext.putPayloadById(smallPayloadWrapper);

    assertThat(postMergeContext.retrievePayloadById(payloadId))
        .map(PayloadWrapper::blockWithReceipts)
        .contains(betterBlockWithReceipts);
  }

  @Test
  public void tryingToRetrieveANotYetPutPayloadIdReturnsEmpty() {
    PayloadIdentifier payloadId = new PayloadIdentifier(1L);

    assertThat(postMergeContext.retrievePayloadById(payloadId)).isEmpty();
  }

  @Test
  public void tryingToRetrieveABlockPutButEvictedReturnsEmpty() {
    PayloadIdentifier evictedPayloadId = new PayloadIdentifier(0L);

    assertThat(postMergeContext.retrievePayloadById(evictedPayloadId)).isEmpty();
  }

  @Test
  public void syncStateNullShouldNotThrowWhenIsSyncingIsCalled() {
    // simulate a possible syncState null when we still have got a syncState set yet.
    postMergeContext.setSyncState(null);
    assertThat(postMergeContext.isSyncing()).isTrue();

    // after setting a syncState things should progress as expected.
    postMergeContext.setSyncState(mockSyncState);

    // Assuming we're not in sync
    when(mockSyncState.isInSync()).thenReturn(Boolean.FALSE);

    when(mockSyncState.hasReachedTerminalDifficulty()).thenReturn(Optional.empty());
    assertThat(postMergeContext.isSyncing()).isTrue();

    when(mockSyncState.hasReachedTerminalDifficulty()).thenReturn(Optional.of(Boolean.FALSE));
    assertThat(postMergeContext.isSyncing()).isTrue();

    when(mockSyncState.hasReachedTerminalDifficulty()).thenReturn(Optional.of(Boolean.TRUE));
    assertThat(postMergeContext.isSyncing()).isFalse();

    // if we're in sync reached ttd does not matter anymore
    when(mockSyncState.isInSync()).thenReturn(Boolean.TRUE);
    assertThat(postMergeContext.isSyncing()).isFalse();
  }

  private PayloadWrapper createPayloadWrapper(
      final PayloadIdentifier firstPayloadId,
      final BlockWithReceipts mockBlockWithReceipts,
      final Wei blockValue) {
    final var payloadWrapper = mock(PayloadWrapper.class);
    when(payloadWrapper.payloadIdentifier()).thenReturn(firstPayloadId);
    when(payloadWrapper.blockWithReceipts()).thenReturn(mockBlockWithReceipts);
    when(payloadWrapper.blockValue()).thenReturn(blockValue);
    return payloadWrapper;
  }

  private static BlockWithReceipts createBlockWithReceipts(
      final int number, final long gasUsed, final int txCount) {
    Block mockBlock = mock(Block.class, RETURNS_DEEP_STUBS);
    // using lenient here, since some code is only executed when debug log is enabled
    lenient()
        .when(mockBlock.toLogString())
        .thenReturn(number + " (" + Hash.wrap(Bytes32.random()) + ")");
    lenient().when(mockBlock.getHeader().getGasUsed()).thenReturn(gasUsed);
    lenient().when(mockBlock.getBody().getTransactions().size()).thenReturn(txCount);
    BlockWithReceipts mockBlockWithReceipts = mock(BlockWithReceipts.class);
    lenient().when(mockBlockWithReceipts.getBlock()).thenReturn(mockBlock);
    return mockBlockWithReceipts;
  }

  private static class MergeStateChangeCollector implements MergeStateHandler {
    final List<Boolean> stateChanges = new ArrayList<>();

    @Override
    public void mergeStateChanged(
        final boolean isPoS,
        final Optional<Boolean> oldState,
        final Optional<Difficulty> difficultyStoppedAt) {
      stateChanges.add(isPoS);
    }

    public void reset() {
      stateChanges.clear();
    }
  }
}
