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
package org.idnecology.idn.ethereum.eth.transactions.layered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.TransactionTestFixture.createSignedCodeDelegation;
import static org.idnecology.idn.ethereum.eth.transactions.PendingTransaction.MAX_SCORE;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Blob;
import org.idnecology.idn.datatypes.BlobsWithCommitments;
import org.idnecology.idn.datatypes.CodeDelegation;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.KZGCommitment;
import org.idnecology.idn.datatypes.KZGProof;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.VersionedHash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.metrics.StubMetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;

public class BaseTransactionPoolTest {

  protected static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);
  protected static final KeyPair KEYS1 = SIGNATURE_ALGORITHM.get().generateKeyPair();
  protected static final KeyPair KEYS2 = SIGNATURE_ALGORITHM.get().generateKeyPair();
  protected static final Address SENDER1 = Util.publicKeyToAddress(KEYS1.getPublicKey());
  protected static final Address SENDER2 = Util.publicKeyToAddress(KEYS2.getPublicKey());
  protected static final CodeDelegation CODE_DELEGATION_SENDER_1 =
      createSignedCodeDelegation(BigInteger.ONE, Address.ZERO, 0, KEYS1);
  protected static final Wei DEFAULT_MIN_GAS_PRICE = Wei.of(50);
  protected static final Wei DEFAULT_MIN_PRIORITY_FEE = Wei.ZERO;
  private static final Random randomizeTxType = new Random();

  protected final Transaction transaction0 = createTransaction(0);
  protected final Transaction transaction1 = createTransaction(1);
  protected final Transaction transaction2 = createTransaction(2);
  protected final Transaction blobTransaction0 = createEIP4844Transaction(0, KEYS1, 1, 1);

  protected final EthScheduler ethScheduler = new DeterministicEthScheduler();
  protected final StubMetricsSystem metricsSystem = new StubMetricsSystem();

  protected Transaction createTransaction(final long nonce) {
    return createTransaction(nonce, Wei.of(5000L), KEYS1);
  }

  protected Transaction createTransaction(final long nonce, final KeyPair keys) {
    return createTransaction(nonce, Wei.of(5000L), keys);
  }

  protected Transaction createTransaction(final long nonce, final Wei maxGasPrice) {
    return createTransaction(nonce, maxGasPrice, KEYS1);
  }

  protected Transaction createTransaction(final long nonce, final int payloadSize) {
    return createTransaction(nonce, Wei.of(5000L), payloadSize, KEYS1);
  }

  protected Transaction createTransaction(
      final long nonce, final Wei maxGasPrice, final KeyPair keys) {
    return createTransaction(nonce, maxGasPrice, 0, keys);
  }

  protected Transaction createEIP1559Transaction(
      final long nonce, final KeyPair keys, final int gasFeeMultiplier) {
    return createTransaction(
        TransactionType.EIP1559, nonce, Wei.of(5000L).multiply(gasFeeMultiplier), 0, null, keys);
  }

  protected Transaction createEIP4844Transaction(
      final long nonce, final KeyPair keys, final int gasFeeMultiplier, final int blobCount) {
    return createTransaction(
        TransactionType.BLOB,
        nonce,
        Wei.of(5000L).multiply(gasFeeMultiplier),
        Wei.of(5000L).multiply(gasFeeMultiplier).divide(10),
        0,
        blobCount,
        null,
        keys);
  }

  protected Transaction createEIP7702Transaction(
      final long nonce,
      final KeyPair keys,
      final int gasFeeMultiplier,
      final List<CodeDelegation> codeDelegations) {
    return createTransaction(
        TransactionType.DELEGATE_CODE,
        nonce,
        Wei.of(5000L).multiply(gasFeeMultiplier),
        0,
        codeDelegations,
        keys);
  }

  protected Transaction createTransactionOfSize(
      final long nonce, final Wei maxGasPrice, final int txSize, final KeyPair keys) {

    final TransactionType txType =
        TransactionType.values()[
            randomizeTxType.nextInt(txSize < blobTransaction0.getSize() ? 3 : 4)];

    final Transaction baseTx =
        createTransaction(txType, nonce, maxGasPrice, maxGasPrice.divide(10), 0, 1, null, keys);
    final int payloadSize = txSize - baseTx.getSize();

    return createTransaction(
        txType, nonce, maxGasPrice, maxGasPrice.divide(10), payloadSize, 1, null, keys);
  }

  protected Transaction createTransaction(
      final long nonce, final Wei maxGasPrice, final int payloadSize, final KeyPair keys) {

    final TransactionType txType = TransactionType.values()[randomizeTxType.nextInt(4)];

    return switch (txType) {
      case FRONTIER, ACCESS_LIST, EIP1559 ->
          createTransaction(txType, nonce, maxGasPrice, payloadSize, null, keys);
      case BLOB ->
          createTransaction(
              txType, nonce, maxGasPrice, maxGasPrice.divide(10), payloadSize, 1, null, keys);
      case DELEGATE_CODE ->
          createTransaction(
              txType, nonce, maxGasPrice, payloadSize, List.of(CODE_DELEGATION_SENDER_1), keys);
    };
  }

  protected Transaction createTransaction(
      final TransactionType type,
      final long nonce,
      final Wei maxGasPrice,
      final int payloadSize,
      final List<CodeDelegation> codeDelegations,
      final KeyPair keys) {
    return createTransaction(
        type, nonce, maxGasPrice, maxGasPrice.divide(10), payloadSize, 0, codeDelegations, keys);
  }

  protected Transaction createTransaction(
      final TransactionType type,
      final long nonce,
      final Wei maxGasPrice,
      final Wei maxPriorityFeePerGas,
      final int payloadSize,
      final int blobCount,
      final List<CodeDelegation> codeDelegations,
      final KeyPair keys) {
    return prepareTransaction(
            type, nonce, maxGasPrice, maxPriorityFeePerGas, payloadSize, blobCount, codeDelegations)
        .createTransaction(keys);
  }

  protected TransactionTestFixture prepareTransaction(
      final TransactionType type,
      final long nonce,
      final Wei maxGasPrice,
      final Wei maxPriorityFeePerGas,
      final int payloadSize,
      final int blobCount,
      final List<CodeDelegation> codeDelegations) {

    var tx =
        new TransactionTestFixture()
            .to(Optional.of(Address.fromHexString("0x634316eA0EE79c701c6F67C53A4C54cBAfd2316d")))
            .value(Wei.of(nonce))
            .nonce(nonce)
            .type(type);
    if (payloadSize > 0) {
      var payloadBytes = Bytes.fromHexString("01".repeat(payloadSize));
      tx.payload(payloadBytes);
    }
    if (type.supports1559FeeMarket()) {
      tx.maxFeePerGas(Optional.of(maxGasPrice))
          .maxPriorityFeePerGas(Optional.of(maxPriorityFeePerGas));
      if (type.supportsBlob() && blobCount > 0) {
        tx.maxFeePerBlobGas(Optional.of(maxGasPrice));
        final var versionHashes =
            IntStream.range(0, blobCount)
                .mapToObj(i -> new VersionedHash((byte) 1, Hash.ZERO))
                .toList();
        final var kgzCommitments =
            IntStream.range(0, blobCount)
                .mapToObj(i -> new KZGCommitment(Bytes48.random()))
                .toList();
        final var kzgProofs =
            IntStream.range(0, blobCount).mapToObj(i -> new KZGProof(Bytes48.random())).toList();
        final var blobs =
            IntStream.range(0, blobCount).mapToObj(i -> new Blob(Bytes.random(32 * 4096))).toList();
        tx.versionedHashes(Optional.of(versionHashes));
        final var blobsWithCommitments =
            new BlobsWithCommitments(kgzCommitments, blobs, kzgProofs, versionHashes);
        tx.blobsWithCommitments(Optional.of(blobsWithCommitments));
      } else if (type.supportsDelegateCode()) {
        tx.codeDelegations(codeDelegations);
      }
    } else {
      tx.gasPrice(maxGasPrice);
    }
    return tx;
  }

  protected Transaction createTransactionReplacement(
      final Transaction originalTransaction, final KeyPair keys) {
    return createTransaction(
        originalTransaction.getType(),
        originalTransaction.getNonce(),
        originalTransaction.getMaxGasPrice().multiply(2),
        originalTransaction.getMaxGasPrice().multiply(2).divide(10),
        0,
        1,
        originalTransaction.getCodeDelegationList().orElse(null),
        keys);
  }

  protected PendingTransaction createRemotePendingTransaction(final Transaction transaction) {
    return new PendingTransaction.Remote(transaction);
  }

  protected PendingTransaction createRemotePendingTransaction(
      final Transaction transaction, final boolean hasPriority) {
    return PendingTransaction.newPendingTransaction(transaction, false, hasPriority, MAX_SCORE);
  }

  protected PendingTransaction createLocalPendingTransaction(final Transaction transaction) {
    return new PendingTransaction.Local(transaction);
  }

  protected void assertTransactionPending(
      final PendingTransactions transactions, final Transaction t) {
    assertThat(transactions.getTransactionByHash(t.getHash())).contains(t);
  }

  protected void assertTransactionNotPending(
      final PendingTransactions transactions, final Transaction t) {
    assertThat(transactions.getTransactionByHash(t.getHash())).isEmpty();
  }

  protected void assertNoNextNonceForSender(
      final PendingTransactions pendingTransactions, final Address sender) {
    assertThat(pendingTransactions.getNextNonceForSender(sender)).isEmpty();
  }

  protected void assertNextNonceForSender(
      final PendingTransactions pendingTransactions, final Address sender1, final int i) {
    assertThat(pendingTransactions.getNextNonceForSender(sender1)).isPresent().hasValue(i);
  }

  protected void addLocalTransactions(
      final PendingTransactions sorter, final Account sender, final long... nonces) {
    for (final long nonce : nonces) {
      sorter.addTransaction(
          createLocalPendingTransaction(createTransaction(nonce)), Optional.of(sender));
    }
  }

  protected long getAddedCount(
      final String source, final String priority, final AddReason addReason, final String layer) {
    return metricsSystem.getCounterValue(
        TransactionPoolMetrics.ADDED_COUNTER_NAME, source, priority, addReason.label(), layer);
  }

  protected long getRemovedCount(
      final String source, final String priority, final String operation, final String layer) {
    return metricsSystem.getCounterValue(
        TransactionPoolMetrics.REMOVED_COUNTER_NAME, source, priority, operation, layer);
  }
}
