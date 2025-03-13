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

import org.idnecology.idn.nativelib.gnark.LibGnarkEIP2537;

import org.apache.tuweni.bytes.Bytes;

/** The BLS12MapFpToG1 precompiled contract. */
public class BLS12MapFpToG1PrecompiledContract extends AbstractBLS12PrecompiledContract {

  private static final int PARAMETER_LENGTH = 64;

  /** Instantiates a new BLS12MapFpToG1 precompiled contract. */
  public BLS12MapFpToG1PrecompiledContract() {
    super(
        "BLS12_MAP_FIELD_TO_CURVE",
        LibGnarkEIP2537.BLS12_MAP_FP_TO_G1_OPERATION_SHIM_VALUE,
        PARAMETER_LENGTH);
  }

  @Override
  public long gasRequirement(final Bytes input) {
    return 5_500;
  }
}
