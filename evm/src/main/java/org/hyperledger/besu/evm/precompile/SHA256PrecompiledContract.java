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
package org.idnecology.idn.evm.precompile;

import org.idnecology.idn.crypto.Hash;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;

/** The Sha256 precompiled contract. */
public class SHA256PrecompiledContract extends AbstractPrecompiledContract {

  /**
   * Instantiates a new Sha256 precompiled contract.
   *
   * @param gasCalculator the gas calculator
   */
  public SHA256PrecompiledContract(final GasCalculator gasCalculator) {
    super("SHA256", gasCalculator);
  }

  @Override
  public long gasRequirement(final Bytes input) {
    return gasCalculator().sha256PrecompiledContractGasCost(input);
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    return PrecompileContractResult.success(Hash.sha256(input));
  }
}
