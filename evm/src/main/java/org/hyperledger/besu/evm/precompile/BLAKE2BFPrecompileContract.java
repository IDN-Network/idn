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

import static java.util.Arrays.copyOfRange;
import static org.idnecology.idn.crypto.Blake2bfMessageDigest.Blake2bfDigest.MESSAGE_LENGTH_BYTES;

import org.idnecology.idn.crypto.Hash;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import java.math.BigInteger;
import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The BLAKE2BF precompile contract. */
// https://github.com/keep-network/go-ethereum/pull/4
public class BLAKE2BFPrecompileContract extends AbstractPrecompiledContract {

  private static final Logger LOG = LoggerFactory.getLogger(BLAKE2BFPrecompileContract.class);

  /**
   * Instantiates a new BLAKE2BF precompile contract.
   *
   * @param gasCalculator the gas calculator
   */
  public BLAKE2BFPrecompileContract(final GasCalculator gasCalculator) {
    super("BLAKE2f", gasCalculator);
  }

  @Override
  public long gasRequirement(final Bytes input) {
    if (input.size() != MESSAGE_LENGTH_BYTES) {
      // Input is malformed, we can't read the number of rounds.
      // Precompile can't be executed, so we set its price to 0.
      return 0L;
    }
    if ((input.get(212) & 0xFE) != 0) {
      // Input is malformed, F value can be only 0 or 1
      return 0L;
    }

    final byte[] roundsBytes = copyOfRange(input.toArrayUnsafe(), 0, 4);
    final BigInteger rounds = new BigInteger(1, roundsBytes);
    return rounds.longValueExact();
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    if (input.size() != MESSAGE_LENGTH_BYTES) {
      LOG.trace(
          "Incorrect input length.  Expected {} and got {}", MESSAGE_LENGTH_BYTES, input.size());
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
    if ((input.get(212) & 0xFE) != 0) {
      LOG.trace("Incorrect finalization flag, expected 0 or 1 and got {}", input.get(212));
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
    return PrecompileContractResult.success(Hash.blake2bf(input));
  }
}
