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

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.TrieGenerator;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.AccountRangeDataRequest;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.BytecodeRequest;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.SnapDataRequest;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.StorageRangeDataRequest;
import org.idnecology.idn.ethereum.proof.WorldStateProofProvider;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.MerkleTrie;
import org.idnecology.idn.ethereum.trie.RangeManager;
import org.idnecology.idn.ethereum.trie.RangeStorageEntriesCollector;
import org.idnecology.idn.ethereum.trie.TrieIterator;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.patricia.StoredMerklePatriciaTrie;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.services.tasks.Task;

import java.util.List;
import java.util.TreeMap;

import kotlin.collections.ArrayDeque;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class TaskGenerator {

  public static List<Task<SnapDataRequest>> createAccountRequest(
      final boolean withData, final boolean withNullTaskElement) {

    final BonsaiWorldStateKeyValueStorage worldStateKeyValueStorage =
        new BonsaiWorldStateKeyValueStorage(
            new InMemoryKeyValueStorageProvider(),
            new NoOpMetricsSystem(),
            DataStorageConfiguration.DEFAULT_BONSAI_CONFIG);

    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    final WorldStateProofProvider worldStateProofProvider =
        new WorldStateProofProvider(worldStateStorageCoordinator);

    final MerkleTrie<Bytes, Bytes> trie =
        TrieGenerator.generateTrie(worldStateStorageCoordinator, 1);
    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(
            Bytes32.ZERO, RangeManager.MAX_RANGE, 1, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> accounts =
        (TreeMap<Bytes32, Bytes>)
            trie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Bytes32.ZERO));

    final Hash rootHash = Hash.wrap(trie.getRootHash());

    final AccountRangeDataRequest accountRangeDataRequest =
        SnapDataRequest.createAccountRangeDataRequest(
            rootHash, RangeManager.MIN_RANGE, RangeManager.MAX_RANGE);
    if (withData) {
      accountRangeDataRequest.addResponse(worldStateProofProvider, accounts, new ArrayDeque<>());
    }

    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(RLP.input(accounts.firstEntry().getValue()));
    final Hash accountHash = Hash.wrap(accounts.firstKey());

    final StorageRangeDataRequest storageRangeDataRequest =
        createStorageRangeDataRequest(
            worldStateProofProvider,
            worldStateStorageCoordinator,
            rootHash,
            accountHash,
            stateTrieAccountValue.getStorageRoot(),
            withData,
            withNullTaskElement);
    final BytecodeRequest bytecodeRequest =
        createBytecodeDataRequest(
            worldStateKeyValueStorage,
            rootHash,
            accountHash,
            stateTrieAccountValue.getCodeHash(),
            withData);

    return List.of(
        new StubTask(accountRangeDataRequest),
        new StubTask(storageRangeDataRequest),
        new StubTask(bytecodeRequest));
  }

  private static StorageRangeDataRequest createStorageRangeDataRequest(
      final WorldStateProofProvider worldStateProofProvider,
      final WorldStateStorageCoordinator worldStateKeyValueStorage,
      final Hash rootHash,
      final Hash accountHash,
      final Bytes32 storageRoot,
      final boolean withData,
      final boolean withNullTaskElement) {

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(
            Bytes32.ZERO, RangeManager.MAX_RANGE, 100, Integer.MAX_VALUE);
    final StoredMerklePatriciaTrie<Bytes, Bytes> storageTrie =
        new StoredMerklePatriciaTrie<>(
            (location, hash) ->
                worldStateKeyValueStorage.getAccountStorageTrieNode(accountHash, location, hash),
            storageRoot,
            b -> b,
            b -> b);

    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> slots =
        (TreeMap<Bytes32, Bytes>)
            storageTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Bytes32.ZERO));

    final StorageRangeDataRequest request =
        SnapDataRequest.createStorageRangeDataRequest(
            rootHash, accountHash, storageRoot, RangeManager.MIN_RANGE, RangeManager.MAX_RANGE);
    if (withData) {
      request.setProofValid(true);
      request.addResponse(null, worldStateProofProvider, slots, new ArrayDeque<>());
    }

    if (withNullTaskElement) {
      // setting isValidProof to true to simulate a null task element.
      request.setProofValid(true);
    }
    return request;
  }

  private static BytecodeRequest createBytecodeDataRequest(
      final BonsaiWorldStateKeyValueStorage worldStateKeyValueStorage,
      final Hash rootHash,
      final Hash accountHash,
      final Hash codeHash,
      final boolean withData) {
    final BytecodeRequest request =
        SnapDataRequest.createBytecodeRequest(accountHash, rootHash, codeHash);
    if (withData) {
      request.setCode(worldStateKeyValueStorage.getCode(codeHash, accountHash).get());
    }
    return request;
  }
}
