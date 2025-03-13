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
package org.idnecology.idn.ethereum.mainnet.blockhash;

import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.mainnet.ParentBeaconBlockRootHelper;
import org.idnecology.idn.ethereum.mainnet.systemcall.BlockProcessingContext;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

/** Processes the beacon block storage if it is present in the block header. */
public class CancunBlockHashProcessor extends FrontierBlockHashProcessor {

  @Override
  public Void process(final BlockProcessingContext context) {
    ProcessableBlockHeader currentBlockHeader = context.getBlockHeader();
    currentBlockHeader
        .getParentBeaconBlockRoot()
        .ifPresent(
            beaconBlockRoot -> {
              if (!beaconBlockRoot.isEmpty()) {
                WorldUpdater worldUpdater = context.getWorldState().updater();
                ParentBeaconBlockRootHelper.storeParentBeaconBlockRoot(
                    worldUpdater, currentBlockHeader.getTimestamp(), beaconBlockRoot);
                worldUpdater.commit();
              }
            });
    return null;
  }
}
