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
package org.idnecology.idn.ethereum.privacy.storage.migration;

import static org.idnecology.idn.ethereum.privacy.storage.PrivateStateKeyValueStorage.SCHEMA_VERSION_1_4_0;
import static org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams.withBlockHeaderAndUpdateNodeHead;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.privacy.PrivateStateRootResolver;
import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyGroupHeadBlockMap;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivateStorageMigration {

  private static final Logger LOG = LoggerFactory.getLogger(PrivateStorageMigration.class);

  private final Blockchain blockchain;
  private final Address privacyPrecompileAddress;
  private final ProtocolSchedule protocolSchedule;
  private final WorldStateArchive publicWorldStateArchive;
  private final PrivateStateStorage privateStateStorage;
  private final PrivateStateRootResolver privateStateRootResolver;
  private final LegacyPrivateStateStorage legacyPrivateStateStorage;
  private final Function<ProtocolSpec, PrivateMigrationBlockProcessor>
      privateMigrationBlockProcessorBuilder;

  public PrivateStorageMigration(
      final Blockchain blockchain,
      final Address privacyPrecompileAddress,
      final ProtocolSchedule protocolSchedule,
      final WorldStateArchive publicWorldStateArchive,
      final PrivateStateStorage privateStateStorage,
      final PrivateStateRootResolver privateStateRootResolver,
      final LegacyPrivateStateStorage legacyPrivateStateStorage,
      final Function<ProtocolSpec, PrivateMigrationBlockProcessor>
          privateMigrationBlockProcessorBuilder) {
    this.privateStateStorage = privateStateStorage;
    this.blockchain = blockchain;
    this.privacyPrecompileAddress = privacyPrecompileAddress;
    this.protocolSchedule = protocolSchedule;
    this.publicWorldStateArchive = publicWorldStateArchive;
    this.privateStateRootResolver = privateStateRootResolver;
    this.legacyPrivateStateStorage = legacyPrivateStateStorage;
    this.privateMigrationBlockProcessorBuilder = privateMigrationBlockProcessorBuilder;
  }

  public void migratePrivateStorage() {
    final long migrationStartTimestamp = System.currentTimeMillis();
    final long chainHeadBlockNumber = blockchain.getChainHeadBlockNumber();

    LOG.info("Migrating private storage database...");

    for (long blockNumber = 0; blockNumber <= chainHeadBlockNumber; blockNumber++) {
      final Block block =
          blockchain
              .getBlockByNumber(blockNumber)
              .orElseThrow(PrivateStorageMigrationException::new);
      final Hash blockHash = block.getHash();
      final BlockHeader blockHeader = block.getHeader();
      LOG.info("Processing block {} ({}/{})", blockHash, blockNumber, chainHeadBlockNumber);

      createPrivacyGroupHeadBlockMap(blockHeader);

      final int lastPmtIndex = findLastPMTIndexInBlock(block);
      if (lastPmtIndex >= 0) {
        final ProtocolSpec protocolSpec = protocolSchedule.getByBlockHeader(blockHeader);
        final PrivateMigrationBlockProcessor privateMigrationBlockProcessor =
            privateMigrationBlockProcessorBuilder.apply(protocolSpec);

        final MutableWorldState publicWorldState =
            blockchain
                .getBlockHeader(blockHeader.getParentHash())
                .flatMap(
                    header ->
                        publicWorldStateArchive.getWorldState(
                            withBlockHeaderAndUpdateNodeHead(header)))
                .orElseThrow(PrivateStorageMigrationException::new);

        final List<Transaction> transactionsToProcess =
            block.getBody().getTransactions().subList(0, lastPmtIndex + 1);
        final List<BlockHeader> ommers = block.getBody().getOmmers();

        privateMigrationBlockProcessor.processBlock(
            blockchain,
            publicWorldState,
            protocolSpec.getBlockHashProcessor().createBlockHashLookup(blockchain, blockHeader),
            blockHeader,
            transactionsToProcess,
            ommers);
      }
    }

    if (isResultingPrivateStateRootAtHeadValid()) {
      privateStateStorage.updater().putDatabaseVersion(SCHEMA_VERSION_1_4_0).commit();
    } else {
      throw new PrivateStorageMigrationException("Inconsistent state root. Please re-sync.");
    }

    final long migrationDuration = System.currentTimeMillis() - migrationStartTimestamp;
    LOG.info("Migration took {} seconds", migrationDuration / 1000.0);
  }

  /*
   Returns the index of the last PMT in the block, or -1 if there are no PMTs in the block.
  */
  private int findLastPMTIndexInBlock(final Block block) {
    final List<Transaction> txs = block.getBody().getTransactions();
    int lastPmtIndex = -1;
    for (int i = 0; i < txs.size(); i++) {
      if (isPrivateMarkerTransaction(txs.get(i))) {
        lastPmtIndex = i;
      }
    }
    return lastPmtIndex;
  }

  private boolean isPrivateMarkerTransaction(final Transaction tx) {
    return tx.getTo().isPresent() && tx.getTo().get().equals(privacyPrecompileAddress);
  }

  private boolean isResultingPrivateStateRootAtHeadValid() {
    final Optional<PrivacyGroupHeadBlockMap> privacyGroupHeadBlockMap =
        privateStateStorage.getPrivacyGroupHeadBlockMap(blockchain.getChainHeadHash());
    final Set<Bytes32> privacyGroupIds =
        privacyGroupHeadBlockMap.orElseThrow(PrivateStorageMigrationException::new).keySet();

    privacyGroupIds.forEach(
        pgId -> {
          final Optional<Hash> legacyStateRoot = legacyPrivateStateStorage.getLatestStateRoot(pgId);
          final Hash newStateRoot =
              privateStateRootResolver.resolveLastStateRoot(pgId, blockchain.getChainHeadHash());
          if (!newStateRoot.equals(legacyStateRoot.orElse(Hash.EMPTY))) {
            throw new PrivateStorageMigrationException(
                "Inconsistent state root. Please delete your database and re-sync your node to avoid inconsistencies in your database.");
          }
        });

    return true;
  }

  private void createPrivacyGroupHeadBlockMap(final BlockHeader blockHeader) {
    final PrivacyGroupHeadBlockMap privacyGroupHeadBlockHash =
        new PrivacyGroupHeadBlockMap(
            privateStateStorage
                .getPrivacyGroupHeadBlockMap(blockHeader.getParentHash())
                .orElse(PrivacyGroupHeadBlockMap.empty()));

    privateStateStorage
        .updater()
        .putPrivacyGroupHeadBlockMap(blockHeader.getHash(), privacyGroupHeadBlockHash)
        .commit();
  }
}
