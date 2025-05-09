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
package org.idnecology.idn.consensus.common.bft;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.bft.network.ValidatorMulticaster;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.RawMessage;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UniqueMessageMulticasterTest {

  private final MessageTracker messageTracker = mock(MessageTracker.class);
  private final ValidatorMulticaster multicaster = mock(ValidatorMulticaster.class);
  private final UniqueMessageMulticaster uniqueMessageMulticaster =
      new UniqueMessageMulticaster(multicaster, messageTracker);
  private final RawMessage messageSent = new RawMessage(5, Bytes.wrap(new byte[5]));

  @Test
  public void previouslySentMessageIsNotSentAgain() {
    when(messageTracker.hasSeenMessage(messageSent)).thenReturn(false);
    uniqueMessageMulticaster.send(messageSent);
    verify(multicaster, times(1)).send(messageSent, emptyList());
    reset(multicaster);

    when(messageTracker.hasSeenMessage(messageSent)).thenReturn(true);
    uniqueMessageMulticaster.send(messageSent);
    uniqueMessageMulticaster.send(messageSent, emptyList());
    verifyNoInteractions(multicaster);
  }

  @Test
  public void messagesSentWithADenylistAreNotRetransmitted() {
    when(messageTracker.hasSeenMessage(messageSent)).thenReturn(false);
    uniqueMessageMulticaster.send(messageSent, emptyList());
    verify(multicaster, times(1)).send(messageSent, emptyList());
    reset(multicaster);

    when(messageTracker.hasSeenMessage(messageSent)).thenReturn(true);
    uniqueMessageMulticaster.send(messageSent, emptyList());
    uniqueMessageMulticaster.send(messageSent);
    verifyNoInteractions(multicaster);
  }

  @Test
  public void passedInDenylistIsPassedToUnderlyingValidator() {
    final List<Address> denylist =
        Lists.newArrayList(AddressHelpers.ofValue(0), AddressHelpers.ofValue(1));
    uniqueMessageMulticaster.send(messageSent, denylist);
    verify(multicaster, times(1)).send(messageSent, denylist);
  }
}
