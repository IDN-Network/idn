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
package org.idnecology.idn.ethereum.trie.diffbased.common.trielog;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.DiffBasedWorldState;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.accumulator.DiffBasedWorldStateUpdateAccumulator;
import org.idnecology.idn.plugin.services.trielogs.TrieLog;

import java.util.Optional;

public class NoOpTrieLogManager extends TrieLogManager {

  public NoOpTrieLogManager() {
    super(null, null, 0, null);
  }

  @Override
  public synchronized void saveTrieLog(
      final DiffBasedWorldStateUpdateAccumulator<?> localUpdater,
      final Hash forWorldStateRootHash,
      final BlockHeader forBlockHeader,
      final DiffBasedWorldState forWorldState) {
    // notify trie log added observers, synchronously
    TrieLog trieLog = trieLogFactory.create(localUpdater, forBlockHeader);
    trieLogObservers.forEach(o -> o.onTrieLogAdded(new TrieLogAddedEvent(trieLog)));
  }

  @Override
  public long getMaxLayersToLoad() {
    return 0;
  }

  @Override
  public Optional<TrieLog> getTrieLogLayer(final Hash blockHash) {
    return Optional.empty();
  }
}
