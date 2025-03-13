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
package org.idnecology.idn.consensus.ibftlegacy;

import static org.idnecology.idn.consensus.ibftlegacy.IbftBlockHeaderValidationRulesetFactory.ibftBlockHeaderValidatorBuilder;

import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.IbftLegacyConfigOptions;
import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockBodyValidator;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockImporter;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSpecs;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecBuilder;
import org.idnecology.idn.evm.internal.EvmConfiguration;

import java.math.BigInteger;
import java.util.Optional;

/** Defines the protocol behaviours for a blockchain using IBFT. */
public class IbftProtocolSchedule {

  // Default constructor
  /** Default constructor */
  public IbftProtocolSchedule() {}

  private static final BigInteger DEFAULT_CHAIN_ID = BigInteger.ONE;
  private static final IbftExtraDataCodec ibftExtraDataCodec = new IbftExtraDataCodec();

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param privacyParameters the privacy parameters
   * @param isRevertReasonEnabled the is revert reason enabled
   * @param evmConfiguration the evm configuration
   * @return the protocol schedule
   */
  public static ProtocolSchedule create(
      final GenesisConfigOptions config,
      final PrivacyParameters privacyParameters,
      final boolean isRevertReasonEnabled,
      final EvmConfiguration evmConfiguration) {
    final IbftLegacyConfigOptions ibftConfig = config.getIbftLegacyConfigOptions();
    final long blockPeriod = ibftConfig.getBlockPeriodSeconds();

    return new ProtocolScheduleBuilder(
            config,
            Optional.of(DEFAULT_CHAIN_ID),
            ProtocolSpecAdapters.create(0, builder -> applyIbftChanges(blockPeriod, builder)),
            privacyParameters,
            isRevertReasonEnabled,
            evmConfiguration,
            null,
            new BadBlockManager(),
            false,
            null)
        .createProtocolSchedule();
  }

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param isRevertReasonEnabled the is revert reason enabled
   * @param evmConfiguration the evm configuration
   * @return the protocol schedule
   */
  public static ProtocolSchedule create(
      final GenesisConfigOptions config,
      final boolean isRevertReasonEnabled,
      final EvmConfiguration evmConfiguration) {
    return create(config, PrivacyParameters.DEFAULT, isRevertReasonEnabled, evmConfiguration);
  }

  private static ProtocolSpecBuilder applyIbftChanges(
      final long secondsBetweenBlocks, final ProtocolSpecBuilder builder) {
    return builder
        .blockHeaderValidatorBuilder(
            feeMarket -> ibftBlockHeaderValidatorBuilder(secondsBetweenBlocks))
        .ommerHeaderValidatorBuilder(
            feeMarket -> ibftBlockHeaderValidatorBuilder(secondsBetweenBlocks))
        .blockBodyValidatorBuilder(MainnetBlockBodyValidator::new)
        .blockValidatorBuilder(MainnetProtocolSpecs.blockValidatorBuilder())
        .blockImporterBuilder(MainnetBlockImporter::new)
        .difficultyCalculator((time, parent) -> BigInteger.ONE)
        .blockReward(Wei.ZERO)
        .skipZeroBlockRewards(true)
        .blockHeaderFunctions(
            new BftBlockHeaderFunctions(
                IbftBlockHashing::calculateHashOfIbftBlockOnchain, ibftExtraDataCodec));
  }
}
