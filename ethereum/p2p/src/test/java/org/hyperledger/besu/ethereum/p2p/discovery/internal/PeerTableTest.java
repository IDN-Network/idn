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
package org.idnecology.idn.ethereum.p2p.discovery.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.ethereum.p2p.discovery.DiscoveryPeer;
import org.idnecology.idn.ethereum.p2p.discovery.Endpoint;
import org.idnecology.idn.ethereum.p2p.discovery.PeerDiscoveryTestHelper;
import org.idnecology.idn.ethereum.p2p.discovery.internal.PeerTable.AddResult.AddOutcome;
import org.idnecology.idn.ethereum.p2p.discovery.internal.PeerTable.EvictResult;
import org.idnecology.idn.ethereum.p2p.discovery.internal.PeerTable.EvictResult.EvictOutcome;
import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;
import org.idnecology.idn.ethereum.p2p.peers.Peer;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class PeerTableTest {

  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);
  private final PeerDiscoveryTestHelper helper = new PeerDiscoveryTestHelper();

  @Test
  public void addPeer() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final List<DiscoveryPeer> peers = helper.createDiscoveryPeers(5);

    for (final DiscoveryPeer peer : peers) {
      final PeerTable.AddResult result = table.tryAdd(peer);
      assertThat(result.getOutcome()).isEqualTo(AddOutcome.ADDED);
    }

    assertThat(table.streamAllPeers()).hasSize(5);
  }

  @Test
  public void addSelf() {
    final DiscoveryPeer localPeer =
        DiscoveryPeer.fromEnode(
            EnodeURLImpl.builder()
                .nodeId(Peer.randomId())
                .ipAddress("127.0.0.1")
                .discoveryAndListeningPorts(12345)
                .build());
    final PeerTable table = new PeerTable(localPeer.getId());
    final PeerTable.AddResult result = table.tryAdd(localPeer);

    assertThat(result.getOutcome()).isEqualTo(AddOutcome.SELF);
    assertThat(table.streamAllPeers()).hasSize(0);
  }

  @Test
  public void peerExists() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final DiscoveryPeer peer = helper.createDiscoveryPeer();

    assertThat(table.tryAdd(peer).getOutcome()).isEqualTo(AddOutcome.ADDED);

    assertThat(table.tryAdd(peer))
        .satisfies(
            result -> {
              assertThat(result.getOutcome()).isEqualTo(AddOutcome.ALREADY_EXISTED);
              assertThat(result.getEvictionCandidate()).isNull();
            });
  }

  @Test
  public void peerExists_withDifferentIp() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final Bytes peerId =
        SIGNATURE_ALGORITHM.get().generateKeyPair().getPublicKey().getEncodedBytes();
    final DiscoveryPeer peer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.1", 30303, Optional.empty()));

    assertThat(table.tryAdd(peer).getOutcome()).isEqualTo(AddOutcome.ADDED);

    final DiscoveryPeer duplicatePeer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.2", 30303, Optional.empty()));
    assertThat(table.tryAdd(duplicatePeer))
        .satisfies(
            result -> {
              assertThat(result.getOutcome()).isEqualTo(AddOutcome.ALREADY_EXISTED);
              assertThat(result.getEvictionCandidate()).isNull();
            });
  }

  @Test
  public void peerExists_withDifferentUdpPort() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final Bytes peerId =
        SIGNATURE_ALGORITHM.get().generateKeyPair().getPublicKey().getEncodedBytes();
    final DiscoveryPeer peer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.1", 30303, Optional.empty()));

    assertThat(table.tryAdd(peer).getOutcome()).isEqualTo(AddOutcome.ADDED);

    final DiscoveryPeer duplicatePeer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.1", 30301, Optional.empty()));
    assertThat(table.tryAdd(duplicatePeer))
        .satisfies(
            result -> {
              assertThat(result.getOutcome()).isEqualTo(AddOutcome.ALREADY_EXISTED);
              assertThat(result.getEvictionCandidate()).isNull();
            });
  }

  @Test
  public void peerExists_withDifferentIdAndUdpPort() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final Bytes peerId =
        SIGNATURE_ALGORITHM.get().generateKeyPair().getPublicKey().getEncodedBytes();
    final DiscoveryPeer peer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.1", 30303, Optional.empty()));

    assertThat(table.tryAdd(peer).getOutcome()).isEqualTo(AddOutcome.ADDED);

    final DiscoveryPeer duplicatePeer =
        DiscoveryPeer.fromIdAndEndpoint(peerId, new Endpoint("1.1.1.2", 30301, Optional.empty()));
    assertThat(table.tryAdd(duplicatePeer))
        .satisfies(
            result -> {
              assertThat(result.getOutcome()).isEqualTo(AddOutcome.ALREADY_EXISTED);
              assertThat(result.getEvictionCandidate()).isNull();
            });
  }

  @Test
  public void evictExistingPeerShouldEvict() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final DiscoveryPeer peer = helper.createDiscoveryPeer();

    table.tryAdd(peer);

    final EvictResult evictResult = table.tryEvict(peer);
    assertThat(evictResult.getOutcome()).isEqualTo(EvictOutcome.EVICTED);
  }

  @Test
  public void evictPeerFromEmptyTableShouldNotEvict() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final DiscoveryPeer peer = helper.createDiscoveryPeer();

    final EvictResult evictResult = table.tryEvict(peer);
    assertThat(evictResult.getOutcome()).isEqualTo(EvictOutcome.ABSENT);
  }

  @Test
  public void evictAbsentPeerShouldNotEvict() {
    final PeerTable table = new PeerTable(Peer.randomId());
    final DiscoveryPeer peer = helper.createDiscoveryPeer();
    final List<DiscoveryPeer> otherPeers = helper.createDiscoveryPeers(5);
    otherPeers.forEach(table::tryAdd);

    final EvictResult evictResult = table.tryEvict(peer);
    assertThat(evictResult.getOutcome()).isEqualTo(EvictOutcome.ABSENT);
  }

  @Test
  public void evictSelfPeerShouldReturnSelfOutcome() {
    final DiscoveryPeer peer = helper.createDiscoveryPeer();
    final PeerTable table = new PeerTable(peer.getId());

    final EvictResult evictResult = table.tryEvict(peer);
    assertThat(evictResult.getOutcome()).isEqualTo(EvictOutcome.SELF);
  }

  @Test
  public void ipAddressIsInvalidReturnsTrue() {
    final Endpoint endpoint1 = new Endpoint("1.1.1.1", 2, Optional.of(Integer.valueOf(1)));
    final DiscoveryPeer peer1 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint1);
    final PeerTable table = new PeerTable(Bytes.random(64));

    table.invalidateIP(endpoint1);

    assertThat(table.isIpAddressInvalid(peer1.getEndpoint())).isEqualTo(true);
  }

  @Test
  public void ipAddressIsInvalidReturnsFalse() {
    final Endpoint endpoint1 = new Endpoint("1.1.1.1", 2, Optional.of(Integer.valueOf(1)));
    final Endpoint endpoint2 = new Endpoint("1.1.1.1", 3, Optional.of(Integer.valueOf(2)));
    final DiscoveryPeer peer1 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint1);
    final DiscoveryPeer peer2 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint2);
    final PeerTable table = new PeerTable(Bytes.random(64));

    final PeerTable.AddResult addResult1 = table.tryAdd(peer1);
    assertThat(addResult1.getOutcome()).isEqualTo(PeerTable.AddResult.added().getOutcome());

    assertThat(table.isIpAddressInvalid(peer2.getEndpoint())).isEqualTo(false);
  }

  @Test
  public void invalidIPAddressNotAdded() {
    final Endpoint endpoint1 = new Endpoint("1.1.1.1", 2, Optional.of(Integer.valueOf(1)));
    final DiscoveryPeer peer1 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint1);
    final PeerTable table = new PeerTable(Bytes.random(64));

    table.invalidateIP(endpoint1);
    final PeerTable.AddResult addResult1 = table.tryAdd(peer1);
    assertThat(addResult1.getOutcome()).isEqualTo(PeerTable.AddResult.invalid().getOutcome());
  }

  @Test
  public void validIPAddressAdded() {
    final Endpoint endpoint1 = new Endpoint("1.1.1.1", 2, Optional.of(Integer.valueOf(1)));
    final Endpoint endpoint2 = new Endpoint("1.1.1.1", 3, Optional.of(Integer.valueOf(2)));
    final DiscoveryPeer peer1 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint1);
    final DiscoveryPeer peer2 = DiscoveryPeer.fromIdAndEndpoint(Peer.randomId(), endpoint2);
    final PeerTable table = new PeerTable(Bytes.random(64));

    final PeerTable.AddResult addResult1 = table.tryAdd(peer1);
    assertThat(addResult1.getOutcome()).isEqualTo(PeerTable.AddResult.added().getOutcome());

    final PeerTable.AddResult addResult2 = table.tryAdd(peer2);
    assertThat(addResult2.getOutcome()).isEqualTo(PeerTable.AddResult.added().getOutcome());
  }
}
