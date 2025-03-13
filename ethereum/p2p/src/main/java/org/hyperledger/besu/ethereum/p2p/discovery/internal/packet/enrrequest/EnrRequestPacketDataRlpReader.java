/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.ethereum.p2p.discovery.internal.packet.enrrequest;

import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.PacketDataDeserializer;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EnrRequestPacketDataRlpReader implements PacketDataDeserializer<EnrRequestPacketData> {
  private final EnrRequestPacketDataFactory enrRequestPacketDataFactory;

  public @Inject EnrRequestPacketDataRlpReader(
      final EnrRequestPacketDataFactory enrRequestPacketDataFactory) {
    this.enrRequestPacketDataFactory = enrRequestPacketDataFactory;
  }

  @Override
  public EnrRequestPacketData readFrom(final RLPInput in) {
    in.enterList();
    final long expiration = in.readLongScalar();
    in.leaveListLenient();
    return enrRequestPacketDataFactory.create(expiration);
  }
}
