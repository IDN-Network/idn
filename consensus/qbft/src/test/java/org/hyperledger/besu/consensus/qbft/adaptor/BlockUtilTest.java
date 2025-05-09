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
package org.idnecology.idn.consensus.qbft.adaptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;

import org.junit.jupiter.api.Test;

class BlockUtilTest {

  @Test
  void canConvertQbftBlockToIdnBlock() {
    BlockHeader header = new BlockHeaderTestFixture().buildHeader();
    Block idnBlock = new Block(header, BlockBody.empty());
    QbftBlock qbftBlock = new QbftBlockAdaptor(idnBlock);

    assertThat(BlockUtil.toIdnBlock(qbftBlock)).isSameAs(idnBlock);
  }
}
