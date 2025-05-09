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
package org.idnecology.idn.consensus.qbft.validator;

import org.idnecology.idn.config.QbftConfigOptions;
import org.idnecology.idn.consensus.common.ForkSpec;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.VoteProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Collection;
import java.util.Optional;

/** The Forking validator provider. */
public class ForkingValidatorProvider implements ValidatorProvider {

  private final Blockchain blockchain;
  private final ForksSchedule<QbftConfigOptions> forksSchedule;
  private final BlockValidatorProvider blockValidatorProvider;
  private final TransactionValidatorProvider transactionValidatorProvider;

  /**
   * Instantiates a new Forking validator provider.
   *
   * @param blockchain the blockchain
   * @param forksSchedule the forks schedule
   * @param blockValidatorProvider the block validator provider
   * @param transactionValidatorProvider the transaction validator provider
   */
  public ForkingValidatorProvider(
      final Blockchain blockchain,
      final ForksSchedule<QbftConfigOptions> forksSchedule,
      final BlockValidatorProvider blockValidatorProvider,
      final TransactionValidatorProvider transactionValidatorProvider) {
    this.blockchain = blockchain;
    this.forksSchedule = forksSchedule;
    this.blockValidatorProvider = blockValidatorProvider;
    this.transactionValidatorProvider = transactionValidatorProvider;
  }

  @Override
  public Collection<Address> getValidatorsAtHead() {
    final BlockHeader chainHead = blockchain.getChainHeadHeader();
    return getValidatorsAfterBlock(chainHead);
  }

  @Override
  public Collection<Address> getValidatorsAfterBlock(final BlockHeader parentHeader) {
    final long nextBlock = parentHeader.getNumber() + 1;
    return resolveValidatorProvider(nextBlock).getValidatorsAfterBlock(parentHeader);
  }

  @Override
  public Collection<Address> getValidatorsForBlock(final BlockHeader header) {
    return resolveValidatorProvider(header.getNumber()).getValidatorsForBlock(header);
  }

  @Override
  public Optional<VoteProvider> getVoteProviderAtHead() {
    return resolveValidatorProvider(blockchain.getChainHeadHeader().getNumber())
        .getVoteProviderAtHead();
  }

  @Override
  public Optional<VoteProvider> getVoteProviderAfterBlock(final BlockHeader header) {
    return resolveValidatorProvider(header.getNumber() + 1).getVoteProviderAtHead();
  }

  private ValidatorProvider resolveValidatorProvider(final long block) {
    final ForkSpec<QbftConfigOptions> fork = forksSchedule.getFork(block);
    return fork.getValue().isValidatorContractMode()
        ? transactionValidatorProvider
        : blockValidatorProvider;
  }
}
