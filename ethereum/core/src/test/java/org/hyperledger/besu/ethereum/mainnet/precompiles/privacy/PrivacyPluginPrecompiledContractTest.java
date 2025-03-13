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
package org.idnecology.idn.ethereum.mainnet.precompiles.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.PrivateTransactionDataFixture.privateTransactionIdn;
import static org.idnecology.idn.ethereum.mainnet.PrivateStateUtils.KEY_IS_PERSISTING_PRIVATE_STATE;
import static org.idnecology.idn.ethereum.mainnet.PrivateStateUtils.KEY_PRIVATE_METADATA_UPDATER;
import static org.idnecology.idn.ethereum.mainnet.PrivateStateUtils.KEY_TRANSACTION;
import static org.idnecology.idn.ethereum.privacy.PrivateTransaction.readFrom;
import static org.idnecology.idn.ethereum.privacy.PrivateTransaction.serialize;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.enclave.EnclaveFactory;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.InMemoryPrivacyStorageProvider;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.privacy.PrivateTransaction;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionProcessor;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyGroupHeadBlockMap;
import org.idnecology.idn.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPInput;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.SpuriousDragonGasCalculator;
import org.idnecology.idn.evm.log.Log;
import org.idnecology.idn.evm.precompile.PrecompiledContract;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.plugin.data.PrivacyGenesis;
import org.idnecology.idn.plugin.services.PrivacyPluginService;
import org.idnecology.idn.plugin.services.privacy.PrivacyGroupAuthProvider;
import org.idnecology.idn.plugin.services.privacy.PrivacyGroupGenesisProvider;
import org.idnecology.idn.plugin.services.privacy.PrivacyPluginPayloadProvider;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PrivacyPluginPrecompiledContractTest {
  private final String DEFAULT_OUTPUT = "0x01";

  MessageFrame messageFrame;

  PrivacyPluginPrecompiledContract contract;

  @BeforeEach
  public void setup() {
    messageFrame = mock(MessageFrame.class);

    final BlockDataGenerator blockGenerator = new BlockDataGenerator();
    final Block genesis = blockGenerator.genesisBlock();
    final Block block =
        blockGenerator.block(
            new BlockDataGenerator.BlockOptions().setParentHash(genesis.getHeader().getHash()));

    when(messageFrame.getContextVariable(KEY_IS_PERSISTING_PRIVATE_STATE, false)).thenReturn(false);
    when(messageFrame.hasContextVariable(KEY_PRIVATE_METADATA_UPDATER)).thenReturn(true);
    when(messageFrame.getContextVariable(KEY_PRIVATE_METADATA_UPDATER))
        .thenReturn(mock(PrivateMetadataUpdater.class));
    when(messageFrame.getBlockValues()).thenReturn(block.getHeader());

    final PrivateMetadataUpdater privateMetadataUpdater = mock(PrivateMetadataUpdater.class);
    when(messageFrame.hasContextVariable(KEY_PRIVATE_METADATA_UPDATER)).thenReturn(true);
    when(messageFrame.getContextVariable(KEY_PRIVATE_METADATA_UPDATER))
        .thenReturn(privateMetadataUpdater);
    when(privateMetadataUpdater.getPrivacyGroupHeadBlockMap())
        .thenReturn(PrivacyGroupHeadBlockMap.empty());

    contract =
        new PrivacyPluginPrecompiledContract(
            new SpuriousDragonGasCalculator(),
            new PrivacyParameters.Builder()
                .setEnabled(true)
                .setPrivacyPluginEnabled(true)
                .setStorageProvider(new InMemoryPrivacyStorageProvider())
                .setPrivacyService(
                    new PrivacyPluginService() {
                      @Override
                      public void setPayloadProvider(final PrivacyPluginPayloadProvider provider) {}

                      @Override
                      public PrivacyPluginPayloadProvider getPayloadProvider() {
                        return new PrivacyPluginPayloadProvider() {
                          @Override
                          public Bytes generateMarkerPayload(
                              final org.idnecology.idn.plugin.data.PrivateTransaction
                                  privateTransaction,
                              final String privacyUserId) {
                            return serialize(privateTransaction).encoded();
                          }

                          @Override
                          public Optional<org.idnecology.idn.plugin.data.PrivateTransaction>
                              getPrivateTransactionFromPayload(
                                  final org.idnecology.idn.datatypes.Transaction transaction) {
                            final BytesValueRLPInput bytesValueRLPInput =
                                new BytesValueRLPInput(transaction.getPayload(), false);
                            return Optional.of(readFrom(bytesValueRLPInput));
                          }
                        };
                      }

                      @Override
                      public void setPrivacyGroupAuthProvider(
                          final PrivacyGroupAuthProvider privacyGroupAuthProvider) {}

                      @Override
                      public PrivacyGroupAuthProvider getPrivacyGroupAuthProvider() {
                        return (privacyGroupId, privacyUserId, blockNumber) -> true;
                      }

                      @Override
                      public void setPrivacyGroupGenesisProvider(
                          final PrivacyGroupGenesisProvider privacyGroupAuthProvider) {}

                      @Override
                      public PrivacyGroupGenesisProvider getPrivacyGroupGenesisProvider() {
                        return (privacyGroupId, blockNumber) ->
                            (PrivacyGenesis) Collections::emptyList;
                      }

                      @Override
                      public PrivateMarkerTransactionFactory getPrivateMarkerTransactionFactory() {
                        return null;
                      }

                      @Override
                      public void setPrivateMarkerTransactionFactory(
                          final PrivateMarkerTransactionFactory privateMarkerTransactionFactory) {}
                    })
                .setEnclaveFactory(mock(EnclaveFactory.class))
                .build());
  }

  @Test
  public void testPayloadFoundInPayloadOfMarker() {
    final List<Log> logs = new ArrayList<>();
    contract.setPrivateTransactionProcessor(
        mockPrivateTxProcessor(
            TransactionProcessingResult.successful(
                logs, 0, 0, Bytes.fromHexString(DEFAULT_OUTPUT), null)));

    final PrivateTransaction privateTransaction = privateTransactionIdn();

    final Bytes payload = convertPrivateTransactionToBytes(privateTransaction);

    final Transaction transaction =
        Transaction.builder().payload(payload).gasPrice(Wei.ZERO).build();

    when(messageFrame.getContextVariable(KEY_TRANSACTION)).thenReturn(transaction);

    final PrecompiledContract.PrecompileContractResult result =
        contract.computePrecompile(payload, messageFrame);
    final Bytes actual = result.getOutput();

    assertThat(actual).isEqualTo(Bytes.fromHexString(DEFAULT_OUTPUT));
  }

  private Bytes convertPrivateTransactionToBytes(final PrivateTransaction privateTransaction) {
    final BytesValueRLPOutput bytesValueRLPOutput = new BytesValueRLPOutput();
    privateTransaction.writeTo(bytesValueRLPOutput);

    return bytesValueRLPOutput.encoded();
  }

  private PrivateTransactionProcessor mockPrivateTxProcessor(
      final TransactionProcessingResult result) {
    final PrivateTransactionProcessor mockPrivateTransactionProcessor =
        mock(PrivateTransactionProcessor.class);
    when(mockPrivateTransactionProcessor.processTransaction(
            nullable(WorldUpdater.class),
            nullable(WorldUpdater.class),
            nullable(ProcessableBlockHeader.class),
            nullable((Hash.class)),
            nullable(PrivateTransaction.class),
            nullable(Address.class),
            nullable(OperationTracer.class),
            nullable(BlockHashLookup.class),
            nullable(Bytes.class)))
        .thenReturn(result);

    return mockPrivateTransactionProcessor;
  }
}
