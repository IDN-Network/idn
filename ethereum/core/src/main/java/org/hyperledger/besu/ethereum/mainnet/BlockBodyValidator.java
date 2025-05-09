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
package org.idnecology.idn.ethereum.mainnet;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.TransactionReceipt;

import java.util.List;

/** Validates block bodies. */
public interface BlockBodyValidator {

  /**
   * Validates that the block body is valid.
   *
   * @param context The context to validate against
   * @param block The block to validate
   * @param receipts The receipts that correspond to the blocks transactions
   * @param worldStateRootHash The rootHash defining the world state after processing this block and
   *     all of its transactions.
   * @param ommerValidationMode The validation mode to use for ommer headers
   * @param bodyValidationMode The validation mode to use for the body
   * @return {@code true} if valid; otherwise {@code false}
   */
  boolean validateBody(
      final ProtocolContext context,
      final Block block,
      final List<TransactionReceipt> receipts,
      final Hash worldStateRootHash,
      final HeaderValidationMode ommerValidationMode,
      final BodyValidationMode bodyValidationMode);

  /**
   * Validates that the block body is valid, but skips state root validation.
   *
   * @param context The context to validate against
   * @param block The block to validate
   * @param receipts The receipts that correspond to the blocks transactions
   * @param ommerValidationMode The validation mode to use for ommer headers
   * @return {@code true} if valid; otherwise {@code false}
   */
  boolean validateBodyLight(
      ProtocolContext context,
      Block block,
      List<TransactionReceipt> receipts,
      final HeaderValidationMode ommerValidationMode);
}
