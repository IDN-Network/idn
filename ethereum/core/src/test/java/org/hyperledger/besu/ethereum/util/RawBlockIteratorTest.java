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
package org.idnecology.idn.ethereum.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RawBlockIteratorTest {

  @TempDir private Path tmp;
  private BlockDataGenerator gen;

  @BeforeEach
  public void setup() {
    gen = new BlockDataGenerator(1);
  }

  @Test
  public void readsBlockAtBoundaryOfInitialCapacity() throws IOException {
    readsBlocksWithInitialCapacity(Function.identity());
  }

  @Test
  public void readsBlockThatExtendsPastInitialCapacity() throws IOException {
    readsBlocksWithInitialCapacity((size) -> size / 2);
  }

  @Test
  public void readsBlockWithinInitialCapacity() throws IOException {
    readsBlocksWithInitialCapacity((size) -> size * 2);
  }

  public void readsBlocksWithInitialCapacity(
      final Function<Integer, Integer> initialCapacityFromBlockSize) throws IOException {
    final int blockCount = 3;
    final List<Block> blocks = gen.blockSequence(blockCount);

    // Write a few blocks to a tmp file
    byte[] firstSerializedBlock = null;
    final File blocksFile = tmp.resolve("blocks").toFile();
    final DataOutputStream writer = new DataOutputStream(new FileOutputStream(blocksFile));
    for (Block block : blocks) {
      final byte[] serializedBlock = serializeBlock(block);
      writer.write(serializedBlock);
      if (firstSerializedBlock == null) {
        firstSerializedBlock = serializedBlock;
      }
    }
    writer.close();
    final BlockHeaderFunctions blockHeaderFunctions = new MainnetBlockHeaderFunctions();
    // Read blocks
    final int initialCapacity = initialCapacityFromBlockSize.apply(firstSerializedBlock.length);
    final RawBlockIterator iterator =
        new RawBlockIterator(blocksFile.toPath(), blockHeaderFunctions, initialCapacity);

    // Read blocks and check that they match
    for (int i = 0; i < blockCount; i++) {
      assertThat(iterator.hasNext()).isTrue();
      final Block readBlock = iterator.next();
      final Block expectedBlock = blocks.get(i);
      assertThat(readBlock).isEqualTo(expectedBlock);
    }

    assertThat(iterator.hasNext()).isFalse();
  }

  private byte[] serializeBlock(final Block block) {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    block.getHeader().writeTo(out);
    out.writeList(block.getBody().getTransactions(), Transaction::writeTo);
    out.writeList(block.getBody().getOmmers(), BlockHeader::writeTo);
    out.endList();
    return out.encoded().toArray();
  }
}
