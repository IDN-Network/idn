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
package org.idnecology.idn.consensus.clique.headervalidationrules;

import org.idnecology.idn.consensus.clique.CliqueDifficultyCalculator;
import org.idnecology.idn.consensus.clique.CliqueHelpers;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.mainnet.AttachedBlockHeaderValidationRule;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Clique difficulty validation rule. */
public class CliqueDifficultyValidationRule implements AttachedBlockHeaderValidationRule {

  private static final Logger LOG = LoggerFactory.getLogger(CliqueDifficultyValidationRule.class);

  /** Default constructor. */
  public CliqueDifficultyValidationRule() {}

  @Override
  public boolean validate(
      final BlockHeader header, final BlockHeader parent, final ProtocolContext protocolContext) {
    final Address actualBlockCreator = CliqueHelpers.getProposerOfBlock(header);
    final CliqueDifficultyCalculator diffCalculator =
        new CliqueDifficultyCalculator(actualBlockCreator);
    final BigInteger expectedDifficulty = diffCalculator.nextDifficulty(0, parent);

    final BigInteger actualDifficulty = header.getDifficulty().toBigInteger();

    if (!expectedDifficulty.equals(actualDifficulty)) {
      LOG.info(
          "Invalid block header: difficulty {} does not equal expected difficulty {}",
          actualDifficulty,
          expectedDifficulty);
      return false;
    }

    return true;
  }

  @Override
  public boolean includeInLightValidation() {
    return false;
  }
}
