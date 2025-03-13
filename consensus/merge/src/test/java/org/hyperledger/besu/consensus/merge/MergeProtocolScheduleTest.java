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
package org.idnecology.idn.consensus.merge;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.evm.operation.InvalidOperation;
import org.idnecology.idn.evm.operation.PrevRanDaoOperation;
import org.idnecology.idn.evm.operation.Push0Operation;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class MergeProtocolScheduleTest {

  @Test
  public void protocolSpecsAreCreatedAtBlockDefinedInJson() {
    final String jsonInput =
        "{\"config\": "
            + "{\"chainId\": 1,\n"
            + "\"homesteadBlock\": 1,\n"
            + "\"LondonBlock\": 1559}"
            + "}";

    final GenesisConfigOptions config = GenesisConfig.fromConfig(jsonInput).getConfigOptions();
    final ProtocolSchedule protocolSchedule =
        MergeProtocolSchedule.create(
            config,
            false,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final ProtocolSpec homesteadSpec = protocolSchedule.getByBlockHeader(blockHeader(1));
    final ProtocolSpec londonSpec = protocolSchedule.getByBlockHeader(blockHeader(1559));

    assertThat(homesteadSpec).isNotEqualTo(londonSpec);
    assertThat(homesteadSpec.getFeeMarket().implementsBaseFee()).isFalse();
    assertThat(londonSpec.getFeeMarket().implementsBaseFee()).isTrue();
  }

  @Test
  public void mergeSpecificModificationsAreUnappliedForShanghai() {

    final GenesisConfigOptions config = GenesisConfig.mainnet().getConfigOptions();
    final ProtocolSchedule protocolSchedule =
        MergeProtocolSchedule.create(
            config,
            false,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final long lastParisBlockNumber = 17034869L;
    final ProtocolSpec parisSpec =
        protocolSchedule.getByBlockHeader(blockHeader(lastParisBlockNumber));
    final ProtocolSpec shanghaiSpec =
        protocolSchedule.getByBlockHeader(
            new BlockHeaderTestFixture().timestamp(1681338455).buildHeader());

    assertThat(parisSpec.getName()).isEqualTo("Paris");
    assertThat(shanghaiSpec.getName()).isEqualTo("Shanghai");

    // ensure PUSH0 is enabled in Shanghai
    final int PUSH0 = 0x5f;
    assertThat(parisSpec.getEvm().getOperationsUnsafe()[PUSH0])
        .isInstanceOf(InvalidOperation.class);
    assertThat(shanghaiSpec.getEvm().getOperationsUnsafe()[PUSH0])
        .isInstanceOf(Push0Operation.class);

    assertProofOfStakeConfigIsEnabled(parisSpec);
    assertProofOfStakeConfigIsEnabled(shanghaiSpec);
  }

  @Test
  public void mergeSpecificModificationsAreUnappliedForCancun_whenShanghaiNotConfigured() {

    final String jsonInput =
        "{\"config\": "
            + "{\"chainId\": 1,\n"
            + "\"parisBlock\": 0,\n"
            + "\"cancunTime\": 1000}"
            + "}";

    final GenesisConfigOptions config = GenesisConfig.fromConfig(jsonInput).getConfigOptions();
    final ProtocolSchedule protocolSchedule =
        MergeProtocolSchedule.create(
            config,
            false,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final ProtocolSpec parisSpec =
        protocolSchedule.getByBlockHeader(
            new BlockHeaderTestFixture().number(9).timestamp(999).buildHeader());
    final ProtocolSpec cancunSpec =
        protocolSchedule.getByBlockHeader(
            new BlockHeaderTestFixture().number(10).timestamp(1000).buildHeader());

    assertThat(parisSpec.getName()).isEqualTo("Paris");
    assertThat(cancunSpec.getName()).isEqualTo("Cancun");

    // ensure PUSH0 is enabled in Cancun (i.e. it has picked up the Shanghai change rather than been
    // reverted to Paris)
    final int PUSH0 = 0x5f;
    assertThat(parisSpec.getEvm().getOperationsUnsafe()[PUSH0])
        .isInstanceOf(InvalidOperation.class);
    assertThat(cancunSpec.getEvm().getOperationsUnsafe()[PUSH0]).isInstanceOf(Push0Operation.class);

    assertProofOfStakeConfigIsEnabled(parisSpec);
    assertProofOfStakeConfigIsEnabled(cancunSpec);
  }

  @Test
  public void mergeSpecificModificationsAreUnappliedForAllMainnetForksAfterParis() {
    final GenesisConfigOptions config = GenesisConfig.mainnet().getConfigOptions();
    final ProtocolSchedule protocolSchedule =
        MergeProtocolSchedule.create(
            config,
            false,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final long lastParisBlockNumber = 17034869L;
    final ProtocolSpec parisSpec =
        protocolSchedule.getByBlockHeader(blockHeader(lastParisBlockNumber));
    assertThat(parisSpec.getName()).isEqualTo("Paris");

    for (long forkTimestamp : config.getForkBlockTimestamps()) {
      final ProtocolSpec postParisSpec =
          protocolSchedule.getByBlockHeader(
              new BlockHeaderTestFixture().timestamp(forkTimestamp).buildHeader());

      assertThat(postParisSpec.getName()).isNotEqualTo("Paris");
      // ensure PUSH0 is enabled from Shanghai onwards
      final int PUSH0 = 0x5f;
      assertThat(parisSpec.getEvm().getOperationsUnsafe()[PUSH0])
          .isInstanceOf(InvalidOperation.class);
      assertThat(postParisSpec.getEvm().getOperationsUnsafe()[PUSH0])
          .isInstanceOf(Push0Operation.class);

      assertProofOfStakeConfigIsEnabled(parisSpec);
      assertProofOfStakeConfigIsEnabled(postParisSpec);
    }
  }

  @Test
  public void parametersAlignWithMainnetWithAdjustments() {
    final ProtocolSpec london =
        MergeProtocolSchedule.create(
                GenesisConfig.DEFAULT.getConfigOptions(),
                false,
                MiningConfiguration.MINING_DISABLED,
                new BadBlockManager(),
                false,
                new NoOpMetricsSystem())
            .getByBlockHeader(blockHeader(0));

    assertThat(london.getName()).isEqualTo("Paris");
    assertProofOfStakeConfigIsEnabled(london);
  }

  private static void assertProofOfStakeConfigIsEnabled(final ProtocolSpec spec) {
    assertThat(spec.isPoS()).isTrue();
    assertThat(spec.getEvm().getOperationsUnsafe()[0x44]).isInstanceOf(PrevRanDaoOperation.class);
    assertThat(spec.getDifficultyCalculator().nextDifficulty(-1, null)).isEqualTo(BigInteger.ZERO);
    assertThat(spec.getBlockReward()).isEqualTo(Wei.ZERO);
    assertThat(spec.isSkipZeroBlockRewards()).isTrue();
    assertThat(spec.getBlockProcessor()).isInstanceOf(MainnetBlockProcessor.class);
  }

  private BlockHeader blockHeader(final long number) {
    return new BlockHeaderTestFixture().number(number).buildHeader();
  }
}
