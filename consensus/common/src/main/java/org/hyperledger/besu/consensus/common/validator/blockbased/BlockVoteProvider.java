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
package org.idnecology.idn.consensus.common.validator.blockbased;

import org.idnecology.idn.consensus.common.validator.ValidatorVote;
import org.idnecology.idn.consensus.common.validator.VoteProvider;
import org.idnecology.idn.consensus.common.validator.VoteType;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Map;
import java.util.Optional;

/** The Block vote provider. */
public class BlockVoteProvider implements VoteProvider {

  private final VoteTallyCache voteTallyCache;
  private final VoteProposer voteProposer;

  /**
   * Instantiates a new Block vote provider.
   *
   * @param voteTallyCache the vote tally cache
   * @param voteProposer the vote proposer
   */
  public BlockVoteProvider(final VoteTallyCache voteTallyCache, final VoteProposer voteProposer) {
    this.voteTallyCache = voteTallyCache;
    this.voteProposer = voteProposer;
  }

  @Override
  public Optional<ValidatorVote> getVoteAfterBlock(
      final BlockHeader header, final Address localAddress) {
    final VoteTally voteTally = voteTallyCache.getVoteTallyAfterBlock(header);
    return voteProposer.getVote(localAddress, voteTally);
  }

  @Override
  public void authVote(final Address address) {
    voteProposer.auth(address);
  }

  @Override
  public void dropVote(final Address address) {
    voteProposer.drop(address);
  }

  @Override
  public void discardVote(final Address address) {
    voteProposer.discard(address);
  }

  @Override
  public Map<Address, VoteType> getProposals() {
    return voteProposer.getProposals();
  }
}
