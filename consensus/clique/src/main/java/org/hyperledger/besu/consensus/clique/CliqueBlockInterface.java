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
package org.idnecology.idn.consensus.clique;

import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.validator.ValidatorVote;
import org.idnecology.idn.consensus.common.validator.VoteType;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;

import java.util.Collection;
import java.util.Optional;

import com.google.common.collect.ImmutableBiMap;
import org.apache.tuweni.bytes.Bytes;

/** The Clique block interface. */
public class CliqueBlockInterface implements BlockInterface {

  /** The constant NO_VOTE_SUBJECT. */
  public static final Address NO_VOTE_SUBJECT = Address.wrap(Bytes.wrap(new byte[Address.SIZE]));

  /** The constant ADD_NONCE. */
  public static final long ADD_NONCE = 0xFFFFFFFFFFFFFFFFL;

  /** The constant DROP_NONCE. */
  public static final long DROP_NONCE = 0x0L;

  private static final ImmutableBiMap<VoteType, Long> voteToValue =
      ImmutableBiMap.of(
          VoteType.ADD, ADD_NONCE,
          VoteType.DROP, DROP_NONCE);

  /** Default constructor. */
  public CliqueBlockInterface() {}

  @Override
  public Address getProposerOfBlock(final BlockHeader header) {
    return CliqueHelpers.getProposerOfBlock(header);
  }

  @Override
  public Address getProposerOfBlock(final org.idnecology.idn.plugin.data.BlockHeader header) {
    return getProposerOfBlock(
        BlockHeader.convertPluginBlockHeader(header, new CliqueBlockHeaderFunctions()));
  }

  @Override
  public Optional<ValidatorVote> extractVoteFromHeader(final BlockHeader header) {
    final Address candidate = header.getCoinbase();
    if (!candidate.equals(NO_VOTE_SUBJECT)) {
      final Address proposer = getProposerOfBlock(header);
      final VoteType votePolarity = voteToValue.inverse().get(header.getNonce());
      final Address recipient = header.getCoinbase();

      return Optional.of(new ValidatorVote(votePolarity, proposer, recipient));
    }
    return Optional.empty();
  }

  /**
   * Create header builder with vote headers.
   *
   * @param builder the builder
   * @param vote the vote
   * @return the block header builder
   */
  public static BlockHeaderBuilder createHeaderBuilderWithVoteHeaders(
      final BlockHeaderBuilder builder, final Optional<ValidatorVote> vote) {
    final BlockHeaderBuilder voteHeaderBuilder = BlockHeaderBuilder.fromBuilder(builder);
    if (vote.isPresent()) {
      final ValidatorVote voteToCast = vote.get();
      voteHeaderBuilder.nonce(voteToValue.get(voteToCast.getVotePolarity()));
      voteHeaderBuilder.coinbase(voteToCast.getRecipient());
    } else {
      voteHeaderBuilder.nonce(voteToValue.get(VoteType.DROP));
      voteHeaderBuilder.coinbase(NO_VOTE_SUBJECT);
    }
    return voteHeaderBuilder;
  }

  @Override
  public Collection<Address> validatorsInBlock(final BlockHeader header) {
    return CliqueExtraData.decode(header).getValidators();
  }

  /**
   * Is valid vote value.
   *
   * @param value the value
   * @return the boolean
   */
  public static boolean isValidVoteValue(final long value) {
    return voteToValue.values().contains(value);
  }
}
