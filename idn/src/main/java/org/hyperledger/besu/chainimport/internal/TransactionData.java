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
package org.idnecology.idn.chainimport.internal;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SECPPrivateKey;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/** The Transaction data. */
@JsonIgnoreProperties("comment")
public class TransactionData {

  private final long gasLimit;
  private final Wei gasPrice;
  private final Bytes data;
  private final Wei value;
  private final Optional<Address> to;
  private final SECPPrivateKey privateKey;

  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);

  /**
   * Instantiates a new Transaction data.
   *
   * @param gasLimit the gas limit
   * @param gasPrice the gas price
   * @param data the data
   * @param value the value
   * @param to the to
   * @param secretKey the secret key
   */
  @JsonCreator
  public TransactionData(
      @JsonProperty("gasLimit") final String gasLimit,
      @JsonProperty("gasPrice") final String gasPrice,
      @JsonProperty("data") final Optional<String> data,
      @JsonProperty("value") final Optional<String> value,
      @JsonProperty("to") final Optional<String> to,
      @JsonProperty("secretKey") final String secretKey) {
    this.gasLimit = UInt256.fromHexString(gasLimit).toLong();
    this.gasPrice = Wei.fromHexString(gasPrice);
    this.data = data.map(Bytes::fromHexString).orElse(Bytes.EMPTY);
    this.value = value.map(Wei::fromHexString).orElse(Wei.ZERO);
    this.to = to.map(Address::fromHexString);
    this.privateKey = SIGNATURE_ALGORITHM.get().createPrivateKey(Bytes32.fromHexString(secretKey));
  }

  /**
   * Gets signed transaction.
   *
   * @param nonceProvider the nonce provider
   * @return the signed transaction
   */
  public Transaction getSignedTransaction(final NonceProvider nonceProvider) {
    final KeyPair keyPair = SIGNATURE_ALGORITHM.get().createKeyPair(privateKey);

    final Address fromAddress = Address.extract(keyPair.getPublicKey());
    final long nonce = nonceProvider.get(fromAddress);
    return Transaction.builder()
        .gasLimit(gasLimit)
        .gasPrice(gasPrice)
        .nonce(nonce)
        .payload(data)
        .value(value)
        .to(to.orElse(null))
        .guessType()
        .signAndBuild(keyPair);
  }

  /** The interface Nonce provider. */
  @FunctionalInterface
  public interface NonceProvider {
    /**
     * Get Nonce.
     *
     * @param address the address
     * @return the Nonce
     */
    long get(final Address address);
  }
}
