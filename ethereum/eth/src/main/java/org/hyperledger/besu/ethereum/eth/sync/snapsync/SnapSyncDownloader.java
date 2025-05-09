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

import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncActions;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncDownloader;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncState;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncStateStorage;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.SnapDataRequest;
import org.idnecology.idn.ethereum.eth.sync.worldstate.WorldStateDownloader;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.SyncDurationMetrics;
import org.idnecology.idn.services.tasks.TaskCollection;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class SnapSyncDownloader extends FastSyncDownloader<SnapDataRequest> {

  public SnapSyncDownloader(
      final FastSyncActions fastSyncActions,
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final WorldStateDownloader worldStateDownloader,
      final FastSyncStateStorage fastSyncStateStorage,
      final TaskCollection<SnapDataRequest> taskCollection,
      final Path fastSyncDataDirectory,
      final FastSyncState initialFastSyncState,
      final SyncDurationMetrics syncDurationMetrics) {
    super(
        fastSyncActions,
        worldStateStorageCoordinator,
        worldStateDownloader,
        fastSyncStateStorage,
        taskCollection,
        fastSyncDataDirectory,
        initialFastSyncState,
        syncDurationMetrics);
  }

  @Override
  protected CompletableFuture<FastSyncState> start(final FastSyncState fastSyncState) {
    LOG.debug("Start snap sync with initial sync state {}", fastSyncState);
    return findPivotBlock(fastSyncState, fss -> downloadChainAndWorldState(fastSyncActions, fss));
  }

  @Override
  protected FastSyncState storeState(final FastSyncState fastSyncState) {
    initialFastSyncState = fastSyncState;
    fastSyncStateStorage.storeState(fastSyncState);
    return new SnapSyncProcessState(fastSyncState);
  }
}
