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
import org.idnecology.idn.evm.internal.Words;

import org.apache.tuweni.bytes.Bytes;

/** The Gas operation. */
public class GasOperation extends AbstractFixedCostOperation {

  /**
   * Instantiates a new Gas operation.
   *
   * @param gasCalculator the gas calculator
   */
  public GasOperation(final GasCalculator gasCalculator) {
    super(0x5A, "GAS", 0, 1, gasCalculator, gasCalculator.getBaseTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    final long gasRemaining = frame.getRemainingGas() - gasCost;
    final Bytes value = Words.longBytes(gasRemaining);
    frame.pushStackItem(value);

    return successResponse;
  }
}
