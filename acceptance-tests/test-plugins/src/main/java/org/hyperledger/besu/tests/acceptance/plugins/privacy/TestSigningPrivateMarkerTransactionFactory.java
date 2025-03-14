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
package org.idnecology.idn.tests.acceptance.plugins.privacy;

import static org.idnecology.idn.datatypes.Address.extract;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SECPPrivateKey;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.plugin.data.PrivateTransaction;
import org.idnecology.idn.plugin.data.UnsignedPrivateMarkerTransaction;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSigningPrivateMarkerTransactionFactory implements PrivateMarkerTransactionFactory {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSigningPrivateMarkerTransactionFactory.class);

  KeyPair aliceFixedSigningKey;
  Address sender;

  public void setSigningKeyEnabled(final String privateMarkerTransactionSigningKey) {
    final SignatureAlgorithm algorithm = SignatureAlgorithmFactory.getInstance();
    final SECPPrivateKey privateKey =
        algorithm.createPrivateKey(Bytes32.fromHexString(privateMarkerTransactionSigningKey));

    aliceFixedSigningKey = algorithm.createKeyPair(privateKey);
    sender = extract(Hash.hash(aliceFixedSigningKey.getPublicKey().getEncodedBytes()));
  }

  @Override
  public Bytes create(
      final UnsignedPrivateMarkerTransaction unsignedPrivateMarkerTransaction,
      final PrivateTransaction privateTransaction,
      final String privacyUserId) {

    final Transaction transaction =
        Transaction.builder()
            .type(TransactionType.FRONTIER)
            .nonce(unsignedPrivateMarkerTransaction.getNonce())
            .gasPrice(
                unsignedPrivateMarkerTransaction.getGasPrice().map(Wei::fromQuantity).orElse(null))
            .gasLimit(unsignedPrivateMarkerTransaction.getGasLimit())
            .to(unsignedPrivateMarkerTransaction.getTo().orElseThrow())
            .value(Wei.fromQuantity(unsignedPrivateMarkerTransaction.getValue()))
            .payload(unsignedPrivateMarkerTransaction.getPayload())
            .signAndBuild(aliceFixedSigningKey);

    LOG.info("Signing PMT from {}", sender);

    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    transaction.writeTo(out);
    return out.encoded();
  }

  @Override
  public Address getSender(
      final PrivateTransaction privateTransaction, final String privacyUserId) {
    return sender;
  }
}
