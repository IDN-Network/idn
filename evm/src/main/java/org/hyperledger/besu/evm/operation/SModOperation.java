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
package org.idnecology.idn.evm.operation;

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;

/** The SMod operation. */
public class SModOperation extends AbstractFixedCostOperation {

  private static final OperationResult smodSuccess = new OperationResult(5, null);

  /**
   * Instantiates a new SMod operation.
   *
   * @param gasCalculator the gas calculator
   */
  public SModOperation(final GasCalculator gasCalculator) {
    super(0x07, "SMOD", 2, 1, gasCalculator, gasCalculator.getLowTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs SMod operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    final Bytes value0 = frame.popStackItem();
    final Bytes value1 = frame.popStackItem();

    if (value1.isZero()) {
      frame.pushStackItem(Bytes.EMPTY);
    } else {
      final BigInteger b1 =
          value0.size() < 32
              ? new BigInteger(1, value0.toArrayUnsafe())
              : new BigInteger(value0.toArrayUnsafe());
      final BigInteger b2 =
          value1.size() < 32
              ? new BigInteger(1, value1.toArrayUnsafe())
              : new BigInteger(value1.toArrayUnsafe());
      BigInteger result = b1.abs().mod(b2.abs());
      if (b1.signum() < 0) {
        result = result.negate();
      }

      Bytes resultBytes = Bytes.wrap(result.toByteArray());
      if (resultBytes.size() > 32) {
        resultBytes = resultBytes.slice(resultBytes.size() - 32, 32);
      }

      final byte[] padding = new byte[32 - resultBytes.size()];
      Arrays.fill(padding, result.signum() < 0 ? (byte) 0xFF : 0x00);

      frame.pushStackItem(Bytes.concatenate(Bytes.wrap(padding), resultBytes));
    }

    return smodSuccess;
  }
}
