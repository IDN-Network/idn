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
package org.idnecology.idn.consensus.qbft.protocol;

import org.idnecology.idn.consensus.qbft.core.messagedata.QbftV1;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.SubProtocol;

/** The Istanbul100 sub protocol. */
public class Istanbul100SubProtocol implements SubProtocol {

  /** The constant NAME. */
  public static String NAME = "istanbul";

  /** The constant ISTANBUL_100. */
  public static final Capability ISTANBUL_100 = Capability.create(NAME, 100);

  private static final Istanbul100SubProtocol INSTANCE = new Istanbul100SubProtocol();

  /** Default constructor */
  public Istanbul100SubProtocol() {}

  /**
   * Get istanbul100 sub protocol.
   *
   * @return the istanbul100 sub protocol
   */
  public static Istanbul100SubProtocol get() {
    return INSTANCE;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public int messageSpace(final int protocolVersion) {
    return QbftV1.MESSAGE_SPACE;
  }

  @Override
  public boolean isValidMessageCode(final int protocolVersion, final int code) {
    switch (code) {
      case QbftV1.PROPOSAL:
      case QbftV1.PREPARE:
      case QbftV1.COMMIT:
      case QbftV1.ROUND_CHANGE:
        return true;

      default:
        return false;
    }
  }

  @Override
  public String messageName(final int protocolVersion, final int code) {
    switch (code) {
      case QbftV1.PROPOSAL:
        return "Proposal";
      case QbftV1.PREPARE:
        return "Prepare";
      case QbftV1.COMMIT:
        return "Commit";
      case QbftV1.ROUND_CHANGE:
        return "RoundChange";
      default:
        return INVALID_MESSAGE_NAME;
    }
  }
}
