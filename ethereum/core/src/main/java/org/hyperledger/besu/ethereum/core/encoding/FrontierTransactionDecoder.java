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
package org.idnecology.idn.ethereum.core.encoding;

import static org.idnecology.idn.ethereum.core.Transaction.REPLAY_PROTECTED_V_BASE;
import static org.idnecology.idn.ethereum.core.Transaction.REPLAY_PROTECTED_V_MIN;
import static org.idnecology.idn.ethereum.core.Transaction.REPLAY_UNPROTECTED_V_BASE;
import static org.idnecology.idn.ethereum.core.Transaction.REPLAY_UNPROTECTED_V_BASE_PLUS_1;
import static org.idnecology.idn.ethereum.core.Transaction.TWO;

import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public class FrontierTransactionDecoder {
  // Supplier for the signature algorithm
  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);

  public static Transaction decode(final RLPInput input) {
    RLPInput transactionRlp = input.readAsRlp();
    transactionRlp.enterList();
    final Transaction.Builder builder =
        Transaction.builder()
            .type(TransactionType.FRONTIER)
            .nonce(transactionRlp.readLongScalar())
            .gasPrice(Wei.of(transactionRlp.readUInt256Scalar()))
            .gasLimit(transactionRlp.readLongScalar())
            .to(transactionRlp.readBytes(v -> v.size() == 0 ? null : Address.wrap(v)))
            .value(Wei.of(transactionRlp.readUInt256Scalar()))
            .payload(transactionRlp.readBytes())
            .rawRlp(transactionRlp.raw());

    final BigInteger v = transactionRlp.readBigIntegerScalar();
    final byte recId;
    Optional<BigInteger> chainId = Optional.empty();
    if (v.equals(REPLAY_UNPROTECTED_V_BASE) || v.equals(REPLAY_UNPROTECTED_V_BASE_PLUS_1)) {
      recId = v.subtract(REPLAY_UNPROTECTED_V_BASE).byteValueExact();
    } else if (v.compareTo(REPLAY_PROTECTED_V_MIN) > 0) {
      chainId = Optional.of(v.subtract(REPLAY_PROTECTED_V_BASE).divide(TWO));
      recId = v.subtract(TWO.multiply(chainId.get()).add(REPLAY_PROTECTED_V_BASE)).byteValueExact();
    } else {
      throw new RuntimeException(
          String.format("An unsupported encoded `v` value of %s was found", v));
    }
    final BigInteger r = transactionRlp.readUInt256Scalar().toUnsignedBigInteger();
    final BigInteger s = transactionRlp.readUInt256Scalar().toUnsignedBigInteger();
    final SECPSignature signature = SIGNATURE_ALGORITHM.get().createSignature(r, s, recId);

    transactionRlp.leaveList();

    chainId.ifPresent(builder::chainId);
    return builder.signature(signature).build();
  }
}
