/*
 * Copyright contributors to Hyperledger Idn.
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
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.internal.Words;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The KZGPointEval precompile contract. */
public class KZGPointEvalPrecompiledContract implements PrecompiledContract {
  private static final AtomicBoolean loaded = new AtomicBoolean(false);

  private static final Logger LOG = LoggerFactory.getLogger(KZGPointEvalPrecompiledContract.class);

  private static Bytes successResult;

  private static void loadLib() {
    CKZG4844JNI.loadNativeLibrary();
    Bytes fieldElementsPerBlob =
        Bytes32.wrap(Words.intBytes(CKZG4844JNI.FIELD_ELEMENTS_PER_BLOB).xor(Bytes32.ZERO));
    Bytes blsModulus =
        Bytes32.wrap(Bytes.of(CKZG4844JNI.BLS_MODULUS.toByteArray()).xor(Bytes32.ZERO));

    successResult = Bytes.concatenate(fieldElementsPerBlob, blsModulus);
  }

  /**
   * Init the C-KZG native lib using a file as trusted setup
   *
   * @param trustedSetupFile the file with the trusted setup
   * @throws IllegalStateException is the trusted setup was already loaded
   */
  public static void init(final Path trustedSetupFile) {
    if (loaded.compareAndSet(false, true)) {
      loadLib();
      final String trustedSetupResourceName = trustedSetupFile.toAbsolutePath().toString();
      LOG.info("Loading trusted setup from user-specified resource {}", trustedSetupResourceName);
      CKZG4844JNI.loadTrustedSetup(trustedSetupResourceName, 0);
    } else {
      throw new IllegalStateException("KZG trusted setup was already loaded");
    }
  }

  /**
   * Init the C-KZG native lib using mainnet trusted setup
   *
   * @throws IllegalStateException is the trusted setup was already loaded
   */
  public static void init() {
    if (loaded.compareAndSet(false, true)) {
      loadLib();
      final String trustedSetupResourceName = "/kzg-trusted-setups/mainnet.txt";
      LOG.info(
          "Loading network trusted setup from classpath resource {}", trustedSetupResourceName);
      CKZG4844JNI.loadTrustedSetupFromResource(
          trustedSetupResourceName, KZGPointEvalPrecompiledContract.class, 0);
    }
  }

  /** free up resources. */
  @VisibleForTesting
  public static void tearDown() {
    CKZG4844JNI.freeTrustedSetup();
    loaded.set(false);
  }

  /** Default constructor. */
  public KZGPointEvalPrecompiledContract() {}

  @Override
  public String getName() {
    return "KZGPointEval";
  }

  @Override
  public long gasRequirement(final Bytes input) {
    // As defined in EIP-4844
    return 50000;
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {

    if (input.size() != 192) {
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
    Bytes32 versionedHash = Bytes32.wrap(input.slice(0, 32));
    Bytes z = input.slice(32, 32);
    Bytes y = input.slice(64, 32);
    Bytes commitment = input.slice(96, 48);
    Bytes proof = input.slice(144, 48);
    if (versionedHash.get(0) != 0x01) { // unsupported hash version
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    } else {
      byte[] hash = Hash.sha256(commitment).toArrayUnsafe();
      hash[0] = 0x01;
      if (!versionedHash.equals(Bytes32.wrap(hash))) {
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
    try {
      boolean proved =
          CKZG4844JNI.verifyKzgProof(
              commitment.toArray(), z.toArray(), y.toArray(), proof.toArray());

      if (proved) {
        return PrecompileContractResult.success(successResult);
      } else {
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    } catch (RuntimeException kzgFailed) {
      LOG.debug("Native KZG failed", kzgFailed);

      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
  }
}
