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

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.evm.frame.ExceptionalHaltReason.INSUFFICIENT_GAS;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.evm.frame.BlockValues;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.ConstantinopleGasCalculator;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.operation.Operation.OperationResult;
import org.idnecology.idn.evm.testutils.FakeBlockValues;
import org.idnecology.idn.evm.testutils.TestMessageFrameBuilder;
import org.idnecology.idn.evm.toy.ToyWorld;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.List;

import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SStoreOperationTest {

  private static final GasCalculator gasCalculator = new ConstantinopleGasCalculator();

  static Iterable<Arguments> data() {
    return List.of(
        Arguments.of(SStoreOperation.FRONTIER_MINIMUM, 200L, 200L, null),
        Arguments.of(SStoreOperation.EIP_1706_MINIMUM, 200L, 200L, INSUFFICIENT_GAS),
        Arguments.of(SStoreOperation.FRONTIER_MINIMUM, 10_000L, 10_000L, null),
        Arguments.of(SStoreOperation.EIP_1706_MINIMUM, 10_000L, 10_000L, null),
        Arguments.of(SStoreOperation.FRONTIER_MINIMUM, 10_000L, 200L, null),
        Arguments.of(SStoreOperation.EIP_1706_MINIMUM, 10_000L, 200L, INSUFFICIENT_GAS));
  }

  private MessageFrame createMessageFrame(
      final Address address, final long initialGas, final long remainingGas) {
    final ToyWorld toyWorld = new ToyWorld();
    final WorldUpdater worldStateUpdater = toyWorld.updater();
    final BlockValues blockHeader = new FakeBlockValues(1337);
    final MessageFrame frame =
        new TestMessageFrameBuilder()
            .address(address)
            .worldUpdater(worldStateUpdater)
            .blockValues(blockHeader)
            .initialGas(initialGas)
            .build();
    worldStateUpdater.getOrCreate(address).setBalance(Wei.of(1));
    worldStateUpdater.commit();
    frame.setGasRemaining(remainingGas);

    return frame;
  }

  @ParameterizedTest(
      name = "{index}: minimum gas {0}, initial gas {1}, remaining gas {2}, expected halt {3}")
  @MethodSource("data")
  void storeOperation(
      final long minimumGasAvailable,
      final long initialGas,
      final long remainingGas,
      final ExceptionalHaltReason expectedHalt) {
    final SStoreOperation operation = new SStoreOperation(gasCalculator, minimumGasAvailable);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(UInt256.ZERO);
    frame.pushStackItem(UInt256.fromHexString("0x01"));

    final OperationResult result = operation.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(expectedHalt);
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
