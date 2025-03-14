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
package org.idnecology.idn.consensus.qbft.core.messagedata;

import org.idnecology.idn.consensus.common.bft.messagedata.AbstractBftMessageData;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Commit;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;

/** The Commit message data. */
public class CommitMessageData extends AbstractBftMessageData {

  private static final int MESSAGE_CODE = QbftV1.COMMIT;

  private CommitMessageData(final Bytes data) {
    super(data);
  }

  /**
   * Create commit message data from message data.
   *
   * @param messageData the message data
   * @return the commit message data
   */
  public static CommitMessageData fromMessageData(final MessageData messageData) {
    return fromMessageData(
        messageData, MESSAGE_CODE, CommitMessageData.class, CommitMessageData::new);
  }

  /**
   * Decode.
   *
   * @return the commit
   */
  public Commit decode() {
    return Commit.decode(data);
  }

  /**
   * Create commit message data.
   *
   * @param commit the commit
   * @return the commit message data
   */
  public static CommitMessageData create(final Commit commit) {
    return new CommitMessageData(commit.encode());
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }
}
