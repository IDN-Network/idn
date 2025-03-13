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
package org.idnecology.idn.consensus.common.bft.headervalidationrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.consensus.common.bft.BftContextBuilder.setupContextWithValidators;

import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Util;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

public class BftCoinbaseValidationRuleTest {

  public static BlockHeader createProposedBlockHeader(final NodeKey proposerNodeKey) {

    final BlockHeaderTestFixture builder = new BlockHeaderTestFixture();
    builder.number(1); // must NOT be block 0, as that should not contain seals at all
    builder.coinbase(Util.publicKeyToAddress(proposerNodeKey.getPublicKey()));
    return builder.buildHeader();
  }

  @Test
  public void proposerInValidatorListPassesValidation() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress =
        Address.extract(Hash.hash(proposerNodeKey.getPublicKey().getEncodedBytes()));

    final List<Address> validators = Lists.newArrayList(proposerAddress);

    final ProtocolContext context =
        new ProtocolContext(
            null, null, setupContextWithValidators(validators), new BadBlockManager());

    final BftCoinbaseValidationRule coinbaseValidationRule = new BftCoinbaseValidationRule();

    final BlockHeader header = createProposedBlockHeader(proposerNodeKey);

    assertThat(coinbaseValidationRule.validate(header, null, context)).isTrue();
  }

  @Test
  public void proposerNotInValidatorListFailsValidation() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();

    final NodeKey otherValidatorNodeKey = NodeKeyUtils.generate();
    final Address otherValidatorNodeAddress =
        Address.extract(Hash.hash(otherValidatorNodeKey.getPublicKey().getEncodedBytes()));

    final List<Address> validators = Lists.newArrayList(otherValidatorNodeAddress);

    final ProtocolContext context =
        new ProtocolContext(
            null, null, setupContextWithValidators(validators), new BadBlockManager());

    final BftCoinbaseValidationRule coinbaseValidationRule = new BftCoinbaseValidationRule();

    final BlockHeader header = createProposedBlockHeader(proposerNodeKey);

    assertThat(coinbaseValidationRule.validate(header, null, context)).isFalse();
  }
}
