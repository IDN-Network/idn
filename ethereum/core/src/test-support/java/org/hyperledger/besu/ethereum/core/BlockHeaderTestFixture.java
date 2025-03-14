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
package org.idnecology.idn.ethereum.core;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.BlobGas;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.evm.log.LogsBloomFilter;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class BlockHeaderTestFixture {

  private Hash parentHash = Hash.EMPTY;
  private Hash ommersHash = Hash.EMPTY_LIST_HASH;
  private Address coinbase = Address.ECREC;

  private Hash stateRoot = Hash.EMPTY_TRIE_HASH;
  private Hash transactionsRoot = Hash.EMPTY_TRIE_HASH;
  private Hash receiptsRoot = Hash.EMPTY_TRIE_HASH;

  private LogsBloomFilter logsBloom = LogsBloomFilter.empty();
  private Difficulty difficulty = Difficulty.ZERO;
  private long number = 0;

  private long gasLimit = 0;
  private Optional<Wei> baseFee = Optional.empty();
  private Optional<Bytes32> prevRandao = Optional.empty();
  private long gasUsed = 0;
  private long timestamp = 0;
  private Bytes extraData = Bytes.EMPTY;

  private Hash mixHash = Hash.EMPTY;
  private long nonce = 0;
  private Optional<Hash> withdrawalsRoot = Optional.empty();
  private Optional<Hash> requestsHash = Optional.empty();
  private BlockHeaderFunctions blockHeaderFunctions = new MainnetBlockHeaderFunctions();
  private Optional<BlobGas> excessBlobGas = Optional.empty();
  private Optional<Long> blobGasUsed = Optional.empty();
  private Optional<Bytes32> parentBeaconBlockRoot = Optional.empty();

  public BlockHeader buildHeader() {
    final BlockHeaderBuilder builder = BlockHeaderBuilder.create();
    builder.parentHash(parentHash);
    builder.ommersHash(ommersHash);
    builder.coinbase(coinbase);
    builder.stateRoot(stateRoot);
    builder.transactionsRoot(transactionsRoot);
    builder.receiptsRoot(receiptsRoot);
    builder.logsBloom(logsBloom);
    builder.difficulty(difficulty);
    builder.number(number);
    builder.gasLimit(gasLimit);
    builder.gasUsed(gasUsed);
    baseFee.ifPresent(builder::baseFee);
    prevRandao.ifPresent((builder::prevRandao));
    builder.timestamp(timestamp);
    builder.extraData(extraData);
    builder.mixHash(mixHash);
    builder.nonce(nonce);
    withdrawalsRoot.ifPresent(builder::withdrawalsRoot);
    excessBlobGas.ifPresent(builder::excessBlobGas);
    blobGasUsed.ifPresent(builder::blobGasUsed);
    requestsHash.ifPresent(builder::requestsHash);
    parentBeaconBlockRoot.ifPresent(builder::parentBeaconBlockRoot);
    builder.blockHeaderFunctions(blockHeaderFunctions);

    return builder.buildBlockHeader();
  }

  public BlockHeaderTestFixture parentHash(final Hash parentHash) {
    this.parentHash = parentHash;
    return this;
  }

  public BlockHeaderTestFixture ommersHash(final Hash ommersHash) {
    this.ommersHash = ommersHash;
    return this;
  }

  public BlockHeaderTestFixture coinbase(final Address coinbase) {
    this.coinbase = coinbase;
    return this;
  }

  public BlockHeaderTestFixture stateRoot(final Hash stateRoot) {
    this.stateRoot = stateRoot;
    return this;
  }

  public BlockHeaderTestFixture transactionsRoot(final Hash transactionsRoot) {
    this.transactionsRoot = transactionsRoot;
    return this;
  }

  public BlockHeaderTestFixture receiptsRoot(final Hash receiptsRoot) {
    this.receiptsRoot = receiptsRoot;
    return this;
  }

  public BlockHeaderTestFixture logsBloom(final LogsBloomFilter logsBloom) {
    this.logsBloom = logsBloom;
    return this;
  }

  public BlockHeaderTestFixture difficulty(final Difficulty difficulty) {
    this.difficulty = difficulty;
    return this;
  }

  public BlockHeaderTestFixture number(final long number) {
    this.number = number;
    return this;
  }

  public BlockHeaderTestFixture gasLimit(final long gasLimit) {
    this.gasLimit = gasLimit;
    return this;
  }

  public BlockHeaderTestFixture gasUsed(final long gasUsed) {
    this.gasUsed = gasUsed;
    return this;
  }

  public BlockHeaderTestFixture baseFeePerGas(final Wei baseFee) {
    this.baseFee = Optional.of(baseFee);
    return this;
  }

  public BlockHeaderTestFixture prevRandao(final Bytes32 prevRandao) {
    this.prevRandao = Optional.ofNullable(prevRandao);
    return this;
  }

  public BlockHeaderTestFixture timestamp(final long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public BlockHeaderTestFixture extraData(final Bytes extraData) {
    this.extraData = extraData;
    return this;
  }

  public BlockHeaderTestFixture mixHash(final Hash mixHash) {
    this.mixHash = mixHash;
    return this;
  }

  public BlockHeaderTestFixture nonce(final long nonce) {
    this.nonce = nonce;
    return this;
  }

  public BlockHeaderTestFixture withdrawalsRoot(final Hash withdrawalsRoot) {
    this.withdrawalsRoot = Optional.ofNullable(withdrawalsRoot);
    return this;
  }

  public BlockHeaderTestFixture requestsHash(final Hash requestsHash) {
    this.requestsHash = Optional.ofNullable(requestsHash);
    return this;
  }

  public BlockHeaderTestFixture excessBlobGas(final BlobGas excessBlobGas) {
    this.excessBlobGas = Optional.ofNullable(excessBlobGas);
    return this;
  }

  public BlockHeaderTestFixture blobGasUsed(final Long blobGasUsed) {
    this.blobGasUsed = Optional.ofNullable(blobGasUsed);
    return this;
  }

  public BlockHeaderTestFixture blockHeaderFunctions(
      final BlockHeaderFunctions blockHeaderFunctions) {
    this.blockHeaderFunctions = blockHeaderFunctions;
    return this;
  }

  public BlockHeaderTestFixture parentBeaconBlockRoot(
      final Optional<Bytes32> parentBeaconBlockRoot) {
    this.parentBeaconBlockRoot = parentBeaconBlockRoot;
    return this;
  }
}
