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
package org.idnecology.idn.ethereum.p2p.rlpx.connections.netty;

import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

final class OutboundMessage {

  private final Capability capability;

  private final MessageData messageData;

  OutboundMessage(final Capability capability, final MessageData data) {
    this.capability = capability;
    this.messageData = data;
  }

  public MessageData getData() {
    return messageData;
  }

  public Capability getCapability() {
    return capability;
  }
}
