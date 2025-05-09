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

import static org.idnecology.idn.evm.internal.Words.clampedToLong;

import org.idnecology.idn.evm.Code;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/** The Return operation. */
public class ReturnContractOperation extends AbstractOperation {

  /** Opcode of RETURNCONTRACT operation */
  public static final int OPCODE = 0xEE;

  /**
   * Instantiates a new Return operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ReturnContractOperation(final GasCalculator gasCalculator) {
    super(OPCODE, "RETURNCONTRACT", 2, 0, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    Code code = frame.getCode();
    if (code.getEofVersion() == 0) {
      return InvalidOperation.INVALID_RESULT;
    }

    int pc = frame.getPC();
    int index = code.readU8(pc + 1);

    final long from = clampedToLong(frame.popStackItem());
    final long length = clampedToLong(frame.popStackItem());

    final long cost = gasCalculator().memoryExpansionGasCost(frame, from, length);
    if (frame.getRemainingGas() < cost) {
      return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
    }

    if (index >= code.getSubcontainerCount()) {
      return new OperationResult(cost, ExceptionalHaltReason.NONEXISTENT_CONTAINER);
    }

    Bytes auxData = frame.readMemory(from, length);
    if (code.getDataSize() + auxData.size() > evm.getMaxCodeSize()) {
      return new OperationResult(cost, ExceptionalHaltReason.CODE_TOO_LARGE);
    }
    if (code.getDataSize() + auxData.size() < code.getDeclaredDataSize()) {
      return new OperationResult(cost, ExceptionalHaltReason.DATA_TOO_SMALL);
    }
    Optional<Code> newCode = code.getSubContainer(index, auxData, evm);
    if (newCode.isEmpty()) {
      return new OperationResult(cost, ExceptionalHaltReason.INVALID_CONTAINER);
    }

    frame.setCreatedCode(newCode.get());
    frame.setState(MessageFrame.State.CODE_SUCCESS);
    return new OperationResult(cost, null);
  }
}
