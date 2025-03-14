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
package org.idnecology.idn.consensus.common.bft.messagedata;

import org.idnecology.idn.ethereum.p2p.rlpx.wire.AbstractMessageData;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;

/** The Abstract bft message data. */
public abstract class AbstractBftMessageData extends AbstractMessageData {
  /**
   * Instantiates a new Abstract bft message data.
   *
   * @param data the data
   */
  protected AbstractBftMessageData(final Bytes data) {
    super(data);
  }

  /**
   * From message data to type of AbstractBftMessageData.
   *
   * @param <T> the type parameter AbstractBftMessageData
   * @param messageData the message data
   * @param messageCode the message code
   * @param clazz the clazz
   * @param constructor the constructor
   * @return the type of AbstractBftMessageData
   */
  protected static <T extends AbstractBftMessageData> T fromMessageData(
      final MessageData messageData,
      final int messageCode,
      final Class<T> clazz,
      final Function<Bytes, T> constructor) {
    if (clazz.isInstance(messageData)) {
      @SuppressWarnings("unchecked")
      T castMessage = (T) messageData;
      return castMessage;
    }
    final int code = messageData.getCode();
    if (code != messageCode) {
      throw new IllegalArgumentException(
          String.format(
              "MessageData has code %d and thus is not a %s", code, clazz.getSimpleName()));
    }

    return constructor.apply(messageData.getData());
  }
}
