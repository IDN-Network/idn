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
package org.idnecology.idn.consensus.clique.blockcreation;

import org.idnecology.idn.config.CliqueConfigOptions;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.blockcreation.DefaultBlockScheduler;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.time.Clock;
import java.util.Collection;
import java.util.Random;

import com.google.common.annotations.VisibleForTesting;

/** The Clique block scheduler. */
public class CliqueBlockScheduler extends DefaultBlockScheduler {

  private final int OUT_OF_TURN_DELAY_MULTIPLIER_MILLIS = 500;

  private final ValidatorProvider validatorProvider;
  private final Address localNodeAddress;
  private final Random r = new Random();

  /**
   * Instantiates a new Clique block scheduler.
   *
   * @param clock the clock
   * @param validatorProvider the validator provider
   * @param localNodeAddress the local node address
   * @param forksSchedule the transitions
   */
  public CliqueBlockScheduler(
      final Clock clock,
      final ValidatorProvider validatorProvider,
      final Address localNodeAddress,
      final ForksSchedule<CliqueConfigOptions> forksSchedule) {
    super(
        parentHeader ->
            (long)
                forksSchedule
                    .getFork(parentHeader.getNumber() + 1)
                    .getValue()
                    .getBlockPeriodSeconds(),
        0L,
        clock);
    this.validatorProvider = validatorProvider;
    this.localNodeAddress = localNodeAddress;
  }

  @Override
  @VisibleForTesting
  public BlockCreationTimeResult getNextTimestamp(final BlockHeader parentHeader) {
    final BlockCreationTimeResult result = super.getNextTimestamp(parentHeader);

    final long milliSecondsUntilNextBlock =
        result.millisecondsUntilValid() + calculateTurnBasedDelay(parentHeader);

    return new BlockCreationTimeResult(
        result.timestampForHeader(), Math.max(0, milliSecondsUntilNextBlock));
  }

  private int calculateTurnBasedDelay(final BlockHeader parentHeader) {
    final CliqueProposerSelector proposerSelector = new CliqueProposerSelector(validatorProvider);
    final Address nextProposer = proposerSelector.selectProposerForNextBlock(parentHeader);

    if (nextProposer.equals(localNodeAddress)) {
      return 0;
    }
    return calculatorOutOfTurnDelay(validatorProvider.getValidatorsAfterBlock(parentHeader));
  }

  private int calculatorOutOfTurnDelay(final Collection<Address> validators) {
    final int countSigners = validators.size();
    final double multiplier = (countSigners / 2d) + 1;
    final int maxDelay = (int) (multiplier * OUT_OF_TURN_DELAY_MULTIPLIER_MILLIS);
    return r.nextInt(maxDelay) + 1;
  }
}
