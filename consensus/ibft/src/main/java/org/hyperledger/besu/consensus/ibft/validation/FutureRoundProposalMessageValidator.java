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
package org.idnecology.idn.consensus.ibft.validation;

import org.idnecology.idn.consensus.ibft.messagewrappers.Proposal;
import org.idnecology.idn.ethereum.core.BlockHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Future round proposal message validator. */
/* One of these will be created by the IbftBlockHeightManager and will exist for the life of the
chainheight, and used to ensure supplied Proposals are suitable for starting a new round.
 */
public class FutureRoundProposalMessageValidator {

  private static final Logger LOG =
      LoggerFactory.getLogger(FutureRoundProposalMessageValidator.class);

  private final MessageValidatorFactory messageValidatorFactory;
  private final long chainHeight;
  private final BlockHeader parentHeader;

  /**
   * Instantiates a new Future round proposal message validator.
   *
   * @param messageValidatorFactory the message validator factory
   * @param chainHeight the chain height
   * @param parentHeader the parent header
   */
  public FutureRoundProposalMessageValidator(
      final MessageValidatorFactory messageValidatorFactory,
      final long chainHeight,
      final BlockHeader parentHeader) {
    this.messageValidatorFactory = messageValidatorFactory;
    this.chainHeight = chainHeight;
    this.parentHeader = parentHeader;
  }

  /**
   * Validate proposal message.
   *
   * @param msg the msg
   * @return the boolean
   */
  public boolean validateProposalMessage(final Proposal msg) {

    if (msg.getRoundIdentifier().getSequenceNumber() != chainHeight) {
      LOG.info("Illegal Proposal message, does not target the correct round height.");
      return false;
    }

    final MessageValidator messageValidator =
        messageValidatorFactory.createMessageValidator(msg.getRoundIdentifier(), parentHeader);

    return messageValidator.validateProposal(msg);
  }
}
