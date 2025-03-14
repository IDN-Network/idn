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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.BlockValues;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.LondonGasCalculator;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;

class PrevRanDaoOperationTest {

  @Test
  void pushesPrevRandaoWhenDifficultyZero() {
    PrevRanDaoOperation op = new PrevRanDaoOperation(new LondonGasCalculator());
    MessageFrame messageFrame = mock(MessageFrame.class);
    BlockValues blockHeader = mock(BlockValues.class);
    Bytes32 prevRandao = Bytes32.fromHexString("0xb0b0face");
    when(blockHeader.getDifficultyBytes()).thenReturn(UInt256.ZERO);
    when(blockHeader.getMixHashOrPrevRandao()).thenReturn(prevRandao);
    when(messageFrame.getBlockValues()).thenReturn(blockHeader);
    EVM evm = mock(EVM.class);
    Operation.OperationResult r = op.executeFixedCostOperation(messageFrame, evm);
    assertThat(r.getHaltReason()).isNull();
    verify(messageFrame).pushStackItem(prevRandao);
  }

  @Test
  void pushesPrevRandDaoWhenDifficultyPresent() {
    PrevRanDaoOperation op = new PrevRanDaoOperation(new LondonGasCalculator());
    MessageFrame messageFrame = mock(MessageFrame.class);
    BlockValues blockHeader = mock(BlockValues.class);
    Bytes32 prevRandao = Bytes32.fromHexString("0xb0b0face");
    Bytes difficulty = Bytes.random(32);
    when(blockHeader.getDifficultyBytes()).thenReturn(difficulty);
    when(blockHeader.getMixHashOrPrevRandao()).thenReturn(prevRandao);
    when(messageFrame.getBlockValues()).thenReturn(blockHeader);
    EVM evm = mock(EVM.class);
    Operation.OperationResult r = op.executeFixedCostOperation(messageFrame, evm);
    assertThat(r.getHaltReason()).isNull();
    verify(messageFrame).pushStackItem(prevRandao);
  }
}
