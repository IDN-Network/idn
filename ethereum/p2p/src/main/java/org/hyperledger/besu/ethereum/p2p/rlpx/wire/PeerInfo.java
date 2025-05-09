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
package org.idnecology.idn.ethereum.p2p.rlpx.wire;

import static org.apache.tuweni.bytes.Bytes.wrap;

import org.idnecology.idn.crypto.SECPPublicKey;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.rlp.RLPInput;
import org.idnecology.idn.ethereum.rlp.RLPOutput;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.owasp.encoder.Encode;

/**
 * Encapsulates information about a peer, including their protocol version, client ID, capabilities
 * and other.
 *
 * <p>The peer info is shared between peers during the <code>HELLO</code> wire protocol handshake.
 */
public class PeerInfo implements Comparable<PeerInfo> {
  private final int version;
  private final String clientId;
  private final List<Capability> capabilities;
  private final int port;
  private final Bytes nodeId;
  private Address address = null;

  public PeerInfo(
      final int version,
      final String clientId,
      final List<Capability> capabilities,
      final int port,
      final Bytes nodeId) {
    this.version = version;
    this.clientId = clientId;
    this.capabilities = capabilities;
    this.port = port;
    this.nodeId = nodeId;
  }

  public static PeerInfo readFrom(final RLPInput in) {
    in.enterList();
    final int version = in.readUnsignedByte();
    final String clientId = new String(in.readBytes().toArrayUnsafe(), StandardCharsets.UTF_8);
    final List<Capability> caps =
        in.nextIsNull() ? Collections.emptyList() : in.readList(Capability::readFrom);
    final int port = in.readIntScalar();
    final Bytes nodeId = in.readBytes();
    in.leaveListLenient();
    return new PeerInfo(version, clientId, caps, port, nodeId);
  }

  public int getVersion() {
    return version;
  }

  public String getClientId() {
    return clientId;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  /**
   * This value is meant to represent the port at which a peer is listening for connections. A value
   * of zero means the peer is not listening for incoming connections.
   *
   * @return The tcp port on which the peer is listening for connections, 0 indicates the peer is
   *     not listening for connections.
   */
  public int getPort() {
    return port;
  }

  public Bytes getNodeId() {
    return nodeId;
  }

  public Address getAddress() {
    if (address == null) {
      final SECPPublicKey remotePublicKey =
          SignatureAlgorithmFactory.getInstance().createPublicKey(nodeId);
      address = Util.publicKeyToAddress(remotePublicKey);
    }
    return address;
  }

  public void writeTo(final RLPOutput out) {
    out.startList();
    out.writeUnsignedByte(getVersion());
    out.writeBytes(wrap(getClientId().getBytes(StandardCharsets.UTF_8)));
    out.writeList(getCapabilities(), Capability::writeTo);
    out.writeIntScalar(getPort());
    out.writeBytes(getNodeId());
    out.endList();
  }

  @Override
  // Returned string is sanitized since it contains user input
  public String toString() {
    final StringBuilder sb = new StringBuilder("PeerInfo{");
    sb.append("version=").append(version);
    sb.append(", clientId='").append(Encode.forJava(clientId)).append('\'');
    sb.append(", capabilities=").append(capabilities);
    sb.append(", port=").append(port);
    sb.append(", nodeId=").append(nodeId);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof PeerInfo)) {
      return false;
    }
    final PeerInfo peerInfo = (PeerInfo) o;
    return version == peerInfo.version
        && port == peerInfo.port
        && Objects.equals(clientId, peerInfo.clientId)
        && Objects.equals(capabilities, peerInfo.capabilities)
        && Objects.equals(nodeId, peerInfo.nodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, clientId, capabilities, port, nodeId);
  }

  @Override
  public int compareTo(final @Nonnull PeerInfo peerInfo) {
    return this.nodeId.compareTo(peerInfo.nodeId);
  }
}
