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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.ibft.messagewrappers.RoundChange;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoundChangeMessageTest {
  @Mock private RoundChange roundChangePayload;
  @Mock private Bytes messageBytes;
  @Mock private MessageData messageData;
  @Mock private RoundChangeMessageData roundChangeMessage;

  @Test
  public void createMessageFromRoundChangeMessageData() {
    when(roundChangePayload.encode()).thenReturn(messageBytes);
    RoundChangeMessageData roundChangeMessage = RoundChangeMessageData.create(roundChangePayload);

    assertThat(roundChangeMessage.getData()).isEqualTo(messageBytes);
    assertThat(roundChangeMessage.getCode()).isEqualTo(IbftV2.ROUND_CHANGE);
    verify(roundChangePayload).encode();
  }

  @Test
  public void createMessageFromRoundChangeMessage() {
    RoundChangeMessageData message = RoundChangeMessageData.fromMessageData(roundChangeMessage);
    assertThat(message).isSameAs(roundChangeMessage);
  }

  @Test
  public void createMessageFromGenericMessageData() {
    when(messageData.getData()).thenReturn(messageBytes);
    when(messageData.getCode()).thenReturn(IbftV2.ROUND_CHANGE);
    RoundChangeMessageData roundChangeMessage = RoundChangeMessageData.fromMessageData(messageData);

    assertThat(roundChangeMessage.getData()).isEqualTo(messageBytes);
    assertThat(roundChangeMessage.getCode()).isEqualTo(IbftV2.ROUND_CHANGE);
  }

  @Test
  public void createMessageFailsWhenIncorrectMessageCode() {
    when(messageData.getCode()).thenReturn(42);
    assertThatThrownBy(() -> RoundChangeMessageData.fromMessageData(messageData))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MessageData has code 42 and thus is not a RoundChangeMessageData");
  }
}
