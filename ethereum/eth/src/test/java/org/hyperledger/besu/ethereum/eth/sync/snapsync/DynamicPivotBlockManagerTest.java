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
package org.idnecology.idn.ethereum.eth.sync.snapsync;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncActions;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncState;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DynamicPivotBlockManagerTest {

  private final SnapSyncProcessState snapSyncState = mock(SnapSyncProcessState.class);
  private final FastSyncActions fastSyncActions = mock(FastSyncActions.class);
  private final SyncState syncState = mock(SyncState.class);
  private final EthContext ethContext = mock(EthContext.class);
  private DynamicPivotBlockSelector dynamicPivotBlockManager;

  @BeforeEach
  public void setup() {
    when(fastSyncActions.getSyncState()).thenReturn(syncState);
    when(ethContext.getScheduler()).thenReturn(new DeterministicEthScheduler());
    dynamicPivotBlockManager =
        new DynamicPivotBlockSelector(
            ethContext,
            fastSyncActions,
            snapSyncState,
            SnapSyncConfiguration.DEFAULT_PIVOT_BLOCK_WINDOW_VALIDITY,
            SnapSyncConfiguration.DEFAULT_PIVOT_BLOCK_DISTANCE_BEFORE_CACHING);
  }

  @Test
  public void shouldNotSearchNewPivotBlockWhenCloseToTheHead() {

    when(syncState.bestChainHeight()).thenReturn(1000L);

    when(snapSyncState.getPivotBlockNumber()).thenReturn(OptionalLong.of(999));
    dynamicPivotBlockManager.check(
        (blockHeader, newBlockFound) -> assertThat(newBlockFound).isFalse());
  }

  @Test
  public void shouldSearchNewPivotBlockWhenNotCloseToTheHead() {
    final FastSyncState selectPivotBlockState = new FastSyncState(1090);
    final BlockHeader pivotBlockHeader = new BlockHeaderTestFixture().number(1090).buildHeader();
    final FastSyncState downloadPivotBlockHeaderState = new FastSyncState(pivotBlockHeader);
    when(fastSyncActions.selectPivotBlock(FastSyncState.EMPTY_SYNC_STATE))
        .thenReturn(completedFuture(selectPivotBlockState));
    when(fastSyncActions.downloadPivotBlockHeader(selectPivotBlockState))
        .thenReturn(completedFuture(downloadPivotBlockHeaderState));

    when(syncState.bestChainHeight()).thenReturn(1000L);

    when(snapSyncState.getPivotBlockNumber()).thenReturn(OptionalLong.of(939));
    dynamicPivotBlockManager.check(
        (blockHeader, newBlockFound) -> assertThat(newBlockFound).isFalse());
  }

  @Test
  public void shouldSwitchToNewPivotBlockWhenNeeded() {
    final FastSyncState selectPivotBlockState = new FastSyncState(1060);
    final BlockHeader pivotBlockHeader = new BlockHeaderTestFixture().number(1060).buildHeader();
    final FastSyncState downloadPivotBlockHeaderState = new FastSyncState(pivotBlockHeader);
    when(fastSyncActions.selectPivotBlock(FastSyncState.EMPTY_SYNC_STATE))
        .thenReturn(completedFuture(selectPivotBlockState));
    when(fastSyncActions.downloadPivotBlockHeader(selectPivotBlockState))
        .thenReturn(completedFuture(downloadPivotBlockHeaderState));
    when(fastSyncActions.getBestChainHeight()).thenReturn(1000L);

    when(snapSyncState.getPivotBlockNumber()).thenReturn(OptionalLong.of(939));
    dynamicPivotBlockManager.check(
        (blockHeader, newBlockFound) -> {
          assertThat(blockHeader.getNumber()).isEqualTo(939);
          assertThat(newBlockFound).isFalse();
        });

    when(fastSyncActions.getBestChainHeight()).thenReturn(1066L);

    dynamicPivotBlockManager.check(
        (blockHeader, newBlockFound) -> {
          assertThat(blockHeader.getNumber()).isEqualTo(pivotBlockHeader.getNumber());
          assertThat(newBlockFound).isTrue();
        });

    verify(snapSyncState).setCurrentHeader(pivotBlockHeader);
  }

  @Test
  public void shouldSwitchToNewPivotOnlyOnce() {
    final FastSyncState selectPivotBlockState = new FastSyncState(1060);
    final BlockHeader pivotBlockHeader = new BlockHeaderTestFixture().number(1060).buildHeader();
    final FastSyncState downloadPivotBlockHeaderState = new FastSyncState(pivotBlockHeader);
    when(fastSyncActions.selectPivotBlock(FastSyncState.EMPTY_SYNC_STATE))
        .thenReturn(completedFuture(selectPivotBlockState));
    when(fastSyncActions.downloadPivotBlockHeader(selectPivotBlockState))
        .thenReturn(completedFuture(downloadPivotBlockHeaderState));

    when(syncState.bestChainHeight()).thenReturn(1066L);

    dynamicPivotBlockManager.check(
        (blockHeader, newBlockFound) -> {
          assertThat(blockHeader.getNumber()).isEqualTo(pivotBlockHeader.getNumber());
          assertThat(newBlockFound).isTrue();
          dynamicPivotBlockManager.check(
              (blockHeader1, aBoolean) -> {
                assertThat(blockHeader1.getNumber()).isEqualTo(939);
                assertThat(aBoolean).isFalse();
              });
        });
  }
}
