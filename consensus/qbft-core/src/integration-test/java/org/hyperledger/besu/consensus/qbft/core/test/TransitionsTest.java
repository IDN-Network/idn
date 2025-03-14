/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.consensus.qbft.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.config.BftFork;
import org.idnecology.idn.config.JsonUtil;
import org.idnecology.idn.config.QbftFork;
import org.idnecology.idn.consensus.common.bft.BftEventQueue;
import org.idnecology.idn.consensus.qbft.core.support.TestContext;
import org.idnecology.idn.consensus.qbft.core.support.TestContextBuilder;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftNewChainHead;
import org.idnecology.idn.testutil.TestClock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class TransitionsTest {

  @Test
  public void transitionsBlockPeriod() throws InterruptedException {
    final TestClock clock = new TestClock(Instant.EPOCH);

    final List<QbftFork> qbftForks =
        List.of(
            new QbftFork(
                JsonUtil.objectNodeFromMap(
                    Map.of(BftFork.FORK_BLOCK_KEY, 1, BftFork.BLOCK_PERIOD_SECONDS_KEY, 10))),
            new QbftFork(
                JsonUtil.objectNodeFromMap(
                    Map.of(BftFork.FORK_BLOCK_KEY, 2, BftFork.BLOCK_PERIOD_SECONDS_KEY, 20))));

    final BftEventQueue bftEventQueue = new BftEventQueue(TestContextBuilder.MESSAGE_QUEUE_LIMIT);
    final TestContext context =
        new TestContextBuilder()
            .indexOfFirstLocallyProposedBlock(0)
            .validatorCount(1)
            .clock(clock)
            .qbftForks(qbftForks)
            .eventQueue(bftEventQueue)
            .buildAndStart();

    clock.stepMillis(10_000);
    context.getEventMultiplexer().handleBftEvent(bftEventQueue.poll(1, TimeUnit.SECONDS));

    context
        .getController()
        .handleNewBlockEvent(new QbftNewChainHead(context.getBlockchain().getChainHeadHeader()));
    clock.stepMillis(20_000);
    context.getEventMultiplexer().handleBftEvent(bftEventQueue.poll(1, TimeUnit.SECONDS));

    final QbftBlockHeader genesisBlock = context.getBlockHeader(0);
    final QbftBlockHeader blockHeader1 = context.getBlockHeader(1);
    final QbftBlockHeader blockHeader2 = context.getBlockHeader(2);

    assertThat(blockHeader1.getTimestamp()).isEqualTo(genesisBlock.getTimestamp() + 10);
    assertThat(blockHeader2.getTimestamp()).isEqualTo(blockHeader1.getTimestamp() + 20);
  }
}
