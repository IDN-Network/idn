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
package org.idnecology.idn.ethereum.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.idnecology.idn.ethereum.blockcreation.AbstractBlockTransactionSelectorTest.Sender.SENDER1;
import static org.idnecology.idn.ethereum.blockcreation.AbstractBlockTransactionSelectorTest.Sender.SENDER2;
import static org.idnecology.idn.ethereum.core.MiningConfiguration.DEFAULT_NON_POA_BLOCK_TXS_SELECTION_MAX_TIME;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.blockcreation.txselection.BlockTransactionSelector;
import org.idnecology.idn.ethereum.blockcreation.txselection.TransactionSelectionResults;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.BaseFeePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.data.TransactionSelectionResult;
import org.idnecology.idn.testutil.TestClock;
import org.idnecology.idn.util.number.Fraction;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class LondonFeeMarketBlockTransactionSelectorTest
    extends AbstractBlockTransactionSelectorTest {

  @Override
  protected GenesisConfig getGenesisConfig() {
    return GenesisConfig.fromResource("/block-transaction-selector/london-genesis.json");
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return new ProtocolScheduleBuilder(
            genesisConfig.getConfigOptions(),
            Optional.of(CHAIN_ID),
            ProtocolSpecAdapters.create(0, Function.identity()),
            new PrivacyParameters(),
            false,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem())
        .createProtocolSchedule();
  }

  @Override
  protected TransactionPool createTransactionPool() {
    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder()
            .txPoolMaxSize(5)
            .txPoolLimitByAccountPercentage(Fraction.fromFloat(1f))
            .minGasPrice(Wei.ONE)
            .build();
    final PendingTransactions pendingTransactions =
        new BaseFeePendingTransactionsSorter(
            poolConf,
            TestClock.system(ZoneId.systemDefault()),
            metricsSystem,
            blockchain::getChainHeadHeader);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> pendingTransactions,
            protocolSchedule,
            protocolContext,
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            poolConf,
            new BlobCache());
    transactionPool.setEnabled();
    return transactionPool;
  }

  @Test
  public void eip1559TransactionCurrentGasPriceLessThanMinimumIsSkippedAndKeptInThePool() {
    final ProcessableBlockHeader blockHeader = createBlock(301_000, Wei.ONE);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        createBlockSelectorAndSetupTxPool(
            createMiningParameters(
                transactionSelectionService,
                Wei.of(6),
                MIN_OCCUPANCY_80_PERCENT,
                DEFAULT_NON_POA_BLOCK_TXS_SELECTION_MAX_TIME),
            transactionProcessor,
            blockHeader,
            miningBeneficiary,
            Wei.ZERO,
            transactionSelectionService);

    // tx is willing to pay max 7 wei for gas, but current network condition (baseFee == 1)
    // result in it paying 2 wei, that is below the minimum accepted by the node, so it is skipped
    final Transaction tx = createEIP1559Transaction(1, Wei.of(7L), Wei.ONE, 100_000);
    final var addResults = transactionPool.addRemoteTransactions(List.of(tx));
    assertThat(addResults).extractingByKey(tx.getHash()).isEqualTo(ValidationResult.valid());

    final TransactionSelectionResults results = selector.buildTransactionListForBlock();

    assertThat(results.getSelectedTransactions()).isEmpty();
    assertThat(results.getNotSelectedTransactions())
        .containsOnly(entry(tx, TransactionSelectionResult.CURRENT_TX_PRICE_BELOW_MIN));
    assertThat(transactionPool.count()).isEqualTo(1);
  }

  @Test
  public void eip1559TransactionCurrentGasPriceGreaterThanMinimumIsSelected() {
    final ProcessableBlockHeader blockHeader = createBlock(301_000, Wei.of(5));

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        createBlockSelectorAndSetupTxPool(
            createMiningParameters(
                transactionSelectionService,
                Wei.of(6),
                MIN_OCCUPANCY_80_PERCENT,
                DEFAULT_NON_POA_BLOCK_TXS_SELECTION_MAX_TIME),
            transactionProcessor,
            blockHeader,
            miningBeneficiary,
            Wei.ZERO,
            transactionSelectionService);

    // tx is willing to pay max 7 wei for gas, and current network condition (baseFee == 5)
    // result in it paying the max, that is >= the minimum accepted by the node, so it is selected
    final Transaction tx = createEIP1559Transaction(1, Wei.of(7), Wei.ONE, 100_000);
    transactionPool.addRemoteTransactions(List.of(tx));

    ensureTransactionIsValid(tx);

    final TransactionSelectionResults results = selector.buildTransactionListForBlock();

    assertThat(results.getSelectedTransactions()).containsExactly(tx);
    assertThat(results.getNotSelectedTransactions()).isEmpty();
  }

  @Test
  public void eip1559PriorityTransactionCurrentGasPriceLessThanMinimumIsSelected() {
    final ProcessableBlockHeader blockHeader = createBlock(301_000, Wei.ONE);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        createBlockSelectorAndSetupTxPool(
            createMiningParameters(
                transactionSelectionService,
                Wei.of(6),
                MIN_OCCUPANCY_80_PERCENT,
                DEFAULT_NON_POA_BLOCK_TXS_SELECTION_MAX_TIME),
            transactionProcessor,
            blockHeader,
            miningBeneficiary,
            Wei.ZERO,
            transactionSelectionService);

    // tx is willing to pay max 7 wei for gas, but current network condition (baseFee == 1)
    // result in it paying 2 wei, that is below the minimum accepted by the node, but since it is
    // a local sender it is accepted anyway
    final Transaction tx = createEIP1559Transaction(1, Wei.of(7L), Wei.ONE, 100_000);
    final var addResult = transactionPool.addTransactionViaApi(tx);
    assertThat(addResult.isValid()).isTrue();

    ensureTransactionIsValid(tx);

    final TransactionSelectionResults results = selector.buildTransactionListForBlock();

    assertThat(results.getSelectedTransactions()).containsExactly(tx);
    assertThat(results.getNotSelectedTransactions()).isEmpty();
  }

  @Test
  public void transactionFromSameSenderWithMixedTypes() {
    final ProcessableBlockHeader blockHeader = createBlock(5_000_000);

    final Transaction txFrontier1 = createTransaction(0, Wei.of(7L), 100_000);
    final Transaction txLondon1 = createEIP1559Transaction(1, Wei.ONE, Wei.ONE, 100_000);
    final Transaction txFrontier2 = createTransaction(2, Wei.of(7L), 100_000);
    final Transaction txLondon2 = createEIP1559Transaction(3, Wei.ONE, Wei.ONE, 100_000);

    ensureTransactionIsValid(txFrontier1);
    ensureTransactionIsValid(txLondon1);
    ensureTransactionIsValid(txFrontier2);
    ensureTransactionIsValid(txLondon2);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        createBlockSelectorAndSetupTxPool(
            defaultTestMiningConfiguration,
            transactionProcessor,
            blockHeader,
            miningBeneficiary,
            Wei.ZERO,
            transactionSelectionService);

    transactionPool.addRemoteTransactions(List.of(txFrontier1, txLondon1, txFrontier2, txLondon2));

    final TransactionSelectionResults results = selector.buildTransactionListForBlock();

    assertThat(results.getSelectedTransactions())
        .containsExactly(txFrontier1, txLondon1, txFrontier2, txLondon2);
    assertThat(results.getNotSelectedTransactions()).isEmpty();
  }

  @Test
  @Override
  public void shouldNotSelectTransactionsWithPriorityFeeLessThanConfig() {
    ProcessableBlockHeader blockHeader = createBlock(5_000_000, Wei.ONE);
    final MiningConfiguration miningConfiguration =
        ImmutableMiningConfiguration.builder().from(defaultTestMiningConfiguration).build();
    miningConfiguration.setMinPriorityFeePerGas(Wei.of(7));

    final Transaction txSelected1 =
        createEIP1559Transaction(0, Wei.of(8), Wei.of(8), 100_000, SENDER1);
    ensureTransactionIsValid(txSelected1);

    // transaction txNotSelected1 should not be selected
    final Transaction txNotSelected1 =
        createEIP1559Transaction(1, Wei.of(7), Wei.of(7), 100_000, SENDER1);
    ensureTransactionIsValid(txNotSelected1);

    // transaction txSelected2 should be selected
    final Transaction txSelected2 =
        createEIP1559Transaction(0, Wei.of(8), Wei.of(8), 100_000, SENDER2);
    ensureTransactionIsValid(txSelected2);

    // transaction txNotSelected2 should not be selected
    final Transaction txNotSelected2 =
        createEIP1559Transaction(1, Wei.of(8), Wei.of(6), 100_000, SENDER2);
    ensureTransactionIsValid(txNotSelected2);

    final BlockTransactionSelector selector =
        createBlockSelectorAndSetupTxPool(
            miningConfiguration,
            transactionProcessor,
            blockHeader,
            AddressHelpers.ofValue(1),
            Wei.ZERO,
            transactionSelectionService);

    transactionPool.addRemoteTransactions(
        List.of(txSelected1, txNotSelected1, txSelected2, txNotSelected2));

    assertThat(transactionPool.getPendingTransactions().size()).isEqualTo(4);

    final TransactionSelectionResults results = selector.buildTransactionListForBlock();

    assertThat(results.getSelectedTransactions()).containsOnly(txSelected1, txSelected2);
    assertThat(results.getNotSelectedTransactions())
        .containsOnly(
            entry(
                txNotSelected1, TransactionSelectionResult.PRIORITY_FEE_PER_GAS_BELOW_CURRENT_MIN),
            entry(
                txNotSelected2, TransactionSelectionResult.PRIORITY_FEE_PER_GAS_BELOW_CURRENT_MIN));
  }
}
