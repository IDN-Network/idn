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

import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPOutput;

import org.apache.tuweni.bytes.Bytes;

public class WithdrawalEncoder {

  public static void encode(final Withdrawal withdrawal, final RLPOutput rlpOutput) {
    rlpOutput.startList();
    rlpOutput.writeBigIntegerScalar(withdrawal.getIndex().toBigInteger());
    rlpOutput.writeBigIntegerScalar(withdrawal.getValidatorIndex().toBigInteger());
    rlpOutput.writeBytes(withdrawal.getAddress());
    rlpOutput.writeUInt64Scalar(withdrawal.getAmount());
    rlpOutput.endList();
  }

  public static Bytes encodeOpaqueBytes(final Withdrawal withdrawal) {
    return RLP.encode(rlpOutput -> encode(withdrawal, rlpOutput));
  }
}
