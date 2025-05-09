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
package org.idnecology.idn.evm.operation;

import static org.idnecology.idn.evm.operation.PushOperation.PUSH_BASE;

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes;

/** The Push0 operation. */
public class Push0Operation extends AbstractFixedCostOperation {

  /** The Push0 operation success result. */
  static final OperationResult push0Success = new OperationResult(2, null);

  /**
   * Instantiates a new Push 0 operation.
   *
   * @param gasCalculator the gas calculator
   */
  public Push0Operation(final GasCalculator gasCalculator) {
    super(PUSH_BASE, "PUSH0", 0, 1, gasCalculator, gasCalculator.getBaseTierGasCost());
  }

  @Override
  public OperationResult executeFixedCostOperation(final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs push0 operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    frame.pushStackItem(Bytes.EMPTY);
    return push0Success;
  }
}
