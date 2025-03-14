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
package org.idnecology.idn.ethereum.api.query;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;

import java.util.Optional;

public class TransactionReceiptWithMetadata {
  private final TransactionReceipt receipt;
  private final Hash transactionHash;
  private final int transactionIndex;
  private final long gasUsed;
  private final Optional<Wei> baseFee;
  private final long blockNumber;
  private final Hash blockHash;
  private final Transaction transaction;
  private final Optional<Long> blobGasUsed;
  private final Optional<Wei> blobGasPrice;
  private final int logIndexOffset;

  private TransactionReceiptWithMetadata(
      final TransactionReceipt receipt,
      final Transaction transaction,
      final Hash transactionHash,
      final int transactionIndex,
      final long gasUsed,
      final Optional<Wei> baseFee,
      final Hash blockHash,
      final long blockNumber,
      final Optional<Long> blobGasUsed,
      final Optional<Wei> blobGasPrice,
      final int logIndexOffset) {
    this.receipt = receipt;
    this.transactionHash = transactionHash;
    this.transactionIndex = transactionIndex;
    this.gasUsed = gasUsed;
    this.baseFee = baseFee;
    this.blockHash = blockHash;
    this.blockNumber = blockNumber;
    this.transaction = transaction;
    this.blobGasUsed = blobGasUsed;
    this.blobGasPrice = blobGasPrice;
    this.logIndexOffset = logIndexOffset;
  }

  public static TransactionReceiptWithMetadata create(
      final TransactionReceipt receipt,
      final Transaction transaction,
      final Hash transactionHash,
      final int transactionIndex,
      final long gasUsed,
      final Optional<Wei> baseFee,
      final Hash blockHash,
      final long blockNumber,
      final Optional<Long> blobGasUsed,
      final Optional<Wei> blobGasPrice,
      final int logIndexOffset) {
    return new TransactionReceiptWithMetadata(
        receipt,
        transaction,
        transactionHash,
        transactionIndex,
        gasUsed,
        baseFee,
        blockHash,
        blockNumber,
        blobGasUsed,
        blobGasPrice,
        logIndexOffset);
  }

  public TransactionReceipt getReceipt() {
    return receipt;
  }

  public Hash getTransactionHash() {
    return transactionHash;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public int getTransactionIndex() {
    return transactionIndex;
  }

  public Hash getBlockHash() {
    return blockHash;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  // The gas used for this particular transaction (as opposed to cumulativeGas which is included in
  // the receipt itself)
  public long getGasUsed() {
    return gasUsed;
  }

  public Optional<Wei> getBaseFee() {
    return baseFee;
  }

  public Optional<Long> getBlobGasUsed() {
    return blobGasUsed;
  }

  public Optional<Wei> getBlobGasPrice() {
    return blobGasPrice;
  }

  public int getLogIndexOffset() {
    return logIndexOffset;
  }
}
