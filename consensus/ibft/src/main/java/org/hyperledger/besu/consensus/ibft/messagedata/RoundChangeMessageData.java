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
package org.idnecology.idn.consensus.ibft.messagedata;

import org.idnecology.idn.consensus.common.bft.messagedata.AbstractBftMessageData;
import org.idnecology.idn.consensus.ibft.messagewrappers.RoundChange;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;

/** The Round change message data. */
public class RoundChangeMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = IbftV2.ROUND_CHANGE;

  private RoundChangeMessageData(final Bytes data) {
    super(data);
  }

  /**
   * From message data to round change message data.
   *
   * @param messageData the message data
   * @return the round change message data
   */
  public static RoundChangeMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, RoundChangeMessageData.class, RoundChangeMessageData::new);
  }

  /**
   * Decode to round change.
   *
   * @return the round change
   */
  public RoundChange decode() {
    return RoundChange.decode(data);
  }

  /**
   * Create round change message data.
   *
   * @param signedPayload the signed payload
   * @return the round change message data
   */
  public static RoundChangeMessageData create(final RoundChange signedPayload) {

    return new RoundChangeMessageData(signedPayload.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
