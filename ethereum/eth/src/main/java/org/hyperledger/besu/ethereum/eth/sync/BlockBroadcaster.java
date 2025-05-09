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
package org.idnecology.idn.ethereum.eth.sync;

import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.messages.NewBlockMessage;
import org.idnecology.idn.ethereum.p2p.rlpx.connections.PeerConnection;
import org.idnecology.idn.util.Subscribers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockBroadcaster {
  private static final Logger LOG = LoggerFactory.getLogger(BlockBroadcaster.class);

  private final EthContext ethContext;
  private final int maxMessageSize;
  private final Subscribers<BlockPropagatedSubscriber> blockPropagatedSubscribers =
      Subscribers.create();

  public BlockBroadcaster(final EthContext ethContext, final int maxMessageSize) {
    this.ethContext = ethContext;
    this.maxMessageSize = maxMessageSize;
  }

  public long subscribePropagateNewBlocks(final BlockPropagatedSubscriber callback) {
    return blockPropagatedSubscribers.subscribe(callback);
  }

  public void unsubscribePropagateNewBlocks(final long id) {
    blockPropagatedSubscribers.unsubscribe(id);
  }

  public void propagate(final Block block, final Difficulty totalDifficulty) {
    blockPropagatedSubscribers.forEach(listener -> listener.accept(block, totalDifficulty));
    final NewBlockMessage newBlockMessage;
    try {
      newBlockMessage = NewBlockMessage.create(block, totalDifficulty, this.maxMessageSize);
    } catch (final IllegalArgumentException e) {
      LOG.error("Failed to create block", e);
      return;
    }
    ethContext
        .getEthPeers()
        .streamAvailablePeers()
        .filter(ethPeer -> !ethPeer.hasSeenBlock(block.getHash()))
        .forEach(
            ethPeer -> {
              ethPeer.registerKnownBlock(block.getHash());
              try {
                ethPeer.send(newBlockMessage);
              } catch (final PeerConnection.PeerNotConnected e) {
                LOG.trace("Failed to broadcast new block to peer", e);
              }
            });
  }

  @FunctionalInterface
  public interface BlockPropagatedSubscriber {
    void accept(Block block, Difficulty totalDifficulty);
  }
}
