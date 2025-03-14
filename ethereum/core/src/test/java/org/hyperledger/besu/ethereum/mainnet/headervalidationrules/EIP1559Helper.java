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
package org.idnecology.idn.ethereum.mainnet.headervalidationrules;

import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Optional;

import org.mockito.Mockito;

public class EIP1559Helper {

  public static BlockHeader blockHeader(
      final long number, final long gasUsed, final Optional<Wei> baseFee) {
    return blockHeader(number, gasUsed, baseFee, 0);
  }

  public static BlockHeader blockHeader(
      final long number, final long gasUsed, final Optional<Wei> baseFee, final long gasLimit) {
    final BlockHeader mock = Mockito.mock(BlockHeader.class);
    when(mock.getNumber()).thenReturn(number);
    when(mock.getGasUsed()).thenReturn(gasUsed);
    when(mock.getBaseFee()).thenReturn(baseFee);
    when(mock.getGasLimit()).thenReturn(gasLimit);
    return mock;
  }
}
