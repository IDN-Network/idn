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
package org.idnecology.idn.ethereum.eth.messages;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.RawMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public final class GetReceiptsMessageTest {

  @Test
  public void roundTripTest() {
    // Generate some hashes
    final BlockDataGenerator gen = new BlockDataGenerator(1);
    final List<Hash> hashes = new ArrayList<>();
    final int hashCount = 20;
    for (int i = 0; i < hashCount; ++i) {
      hashes.add(gen.hash());
    }

    // Perform round-trip transformation
    // Create GetReceipts message, copy it to a generic message, then read back into a GetReceipts
    // message
    final MessageData initialMessage = GetReceiptsMessage.create(hashes);
    final MessageData raw = new RawMessage(EthPV63.GET_RECEIPTS, initialMessage.getData());
    final GetReceiptsMessage message = GetReceiptsMessage.readFrom(raw);

    // Read hashes back out after round trip and check they match originals.
    final Iterator<Hash> readData = message.hashes().iterator();
    for (int i = 0; i < hashCount; ++i) {
      Assertions.assertThat(readData.next()).isEqualTo(hashes.get(i));
    }
    Assertions.assertThat(readData.hasNext()).isFalse();
  }
}
