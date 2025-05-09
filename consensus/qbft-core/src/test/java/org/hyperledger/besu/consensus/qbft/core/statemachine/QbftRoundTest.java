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
package org.idnecology.idn.consensus.qbft.core.statemachine;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.consensus.qbft.core.QbftContextBuilder.setupContextWithBftBlockInterface;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.core.QbftBlockTestFixture;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.RoundChange;
import org.idnecology.idn.consensus.qbft.core.network.QbftMessageTransmitter;
import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.payload.PreparePayload;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCreator;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockImporter;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockInterface;
import org.idnecology.idn.consensus.qbft.core.types.QbftContext;
import org.idnecology.idn.consensus.qbft.core.types.QbftMinedBlockObserver;
import org.idnecology.idn.consensus.qbft.core.types.QbftProtocolSchedule;
import org.idnecology.idn.consensus.qbft.core.validation.MessageValidator;
import org.idnecology.idn.consensus.qbft.core.validation.QbftBlockHeaderTestFixture;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.services.securitymodule.SecurityModuleException;
import org.idnecology.idn.util.Subscribers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QbftRoundTest {

  private final NodeKey nodeKey = NodeKeyUtils.generate();
  private final NodeKey nodeKey2 = NodeKeyUtils.generate();
  private final ConsensusRoundIdentifier roundIdentifier = new ConsensusRoundIdentifier(1, 0);
  private final Subscribers<QbftMinedBlockObserver> subscribers = Subscribers.create();
  private ProtocolContext protocolContext;
  private MessageFactory messageFactory;
  private MessageFactory messageFactory2;

  @Mock private QbftProtocolSchedule protocolSchedule;
  @Mock private MutableBlockchain blockChain;
  @Mock private WorldStateArchive worldStateArchive;
  @Mock private QbftMessageTransmitter transmitter;
  @Mock private QbftMinedBlockObserver minedBlockObserver;
  @Mock private QbftBlockCreator blockCreator;
  @Mock private MessageValidator messageValidator;
  @Mock private RoundTimer roundTimer;
  @Mock private QbftBlockImporter blockImporter;
  @Mock private QbftBlockHeader parentHeader;
  @Mock private BftExtraDataCodec bftExtraDataCodec;
  @Mock private QbftBlockInterface blockInteface;
  @Mock private QbftBlockCodec blockEncoder;

  @Captor private ArgumentCaptor<QbftBlock> blockCaptor;

  private QbftBlock proposedBlock;

  private final SECPSignature remoteCommitSeal =
      SignatureAlgorithmFactory.getInstance()
          .createSignature(BigInteger.ONE, BigInteger.ONE, (byte) 1);

  @BeforeEach
  public void setup() {
    messageFactory = new MessageFactory(nodeKey, blockEncoder);
    messageFactory2 = new MessageFactory(nodeKey2, blockEncoder);

    protocolContext =
        new ProtocolContext(
            blockChain,
            worldStateArchive,
            setupContextWithBftBlockInterface(QbftContext.class, emptyList(), blockInteface),
            new BadBlockManager());

    when(messageValidator.validateProposal(any())).thenReturn(true);
    when(messageValidator.validatePrepare(any())).thenReturn(true);
    when(messageValidator.validateCommit(any())).thenReturn(true);

    final QbftBlockHeader header = new QbftBlockHeaderTestFixture().number(1).buildHeader();

    proposedBlock = new QbftBlockTestFixture().blockHeader(header).build();

    when(blockCreator.createBlock(anyLong(), any())).thenReturn(proposedBlock);

    when(protocolSchedule.getBlockImporter(any())).thenReturn(blockImporter);

    when(blockImporter.importBlock(any())).thenReturn(true);

    BftExtraData bftExtraData =
        new BftExtraData(Bytes.wrap(new byte[32]), emptyList(), empty(), 0, emptyList());
    when(bftExtraDataCodec.decode(any())).thenReturn(bftExtraData);
    when(bftExtraDataCodec.encode(any())).thenReturn(Bytes.EMPTY);
    when(bftExtraDataCodec.encodeWithoutCommitSeals(any())).thenReturn(Bytes.EMPTY);
    when(bftExtraDataCodec.encodeWithoutCommitSealsAndRoundNumber(any())).thenReturn(Bytes.EMPTY);

    subscribers.subscribe(minedBlockObserver);
  }

  @Test
  public void onConstructionRoundTimerIsStarted() {
    final RoundState roundState = new RoundState(roundIdentifier, 3, messageValidator);
    new QbftRound(
        roundState,
        blockCreator,
        protocolContext,
        protocolSchedule,
        subscribers,
        nodeKey,
        messageFactory,
        transmitter,
        roundTimer,
        bftExtraDataCodec,
        parentHeader);
    verify(roundTimer, times(1)).startTimer(roundIdentifier);
  }

  @Test
  public void onReceptionOfValidProposalSendsAPrepareToNetworkPeers() {
    final RoundState roundState = new RoundState(roundIdentifier, 3, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    when(blockInteface.replaceRoundInBlock(eq(proposedBlock), eq(0))).thenReturn(proposedBlock);

    round.handleProposalMessage(
        messageFactory.createProposal(
            roundIdentifier, proposedBlock, Collections.emptyList(), Collections.emptyList()));
    verify(transmitter, times(1)).multicastPrepare(roundIdentifier, proposedBlock.getHash());
    verify(transmitter, never()).multicastCommit(any(), any(), any());
  }

  @Test
  public void aProposalWithAnewBlockIsSentUponReceptionOfARoundChangeWithNoCertificate() {
    final QbftBlockHeader header = new QbftBlockHeaderTestFixture().number(0).buildHeader();

    final QbftBlock commitBlock = new QbftBlockTestFixture().blockHeader(header).build();
    when(blockInteface.replaceRoundInBlock(proposedBlock, 0)).thenReturn(commitBlock);

    final RoundState roundState = new RoundState(roundIdentifier, 2, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    round.startRoundWith(new RoundChangeArtifacts(emptyList(), Optional.empty()), 15);
    verify(transmitter, times(1))
        .multicastProposal(eq(roundIdentifier), any(), eq(emptyList()), eq(emptyList()));
    verify(transmitter, times(1)).multicastPrepare(eq(roundIdentifier), any());
  }

  @Test
  public void aProposalMessageWithTheSameBlockIsSentUponReceptionOfARoundChangeWithCertificate() {
    final QbftBlock publishBlock =
        new QbftBlockTestFixture()
            .blockHeader(new QbftBlockHeaderTestFixture().number(0).buildHeader())
            .build();
    final QbftBlock commitBlock =
        new QbftBlockTestFixture()
            .blockHeader(new QbftBlockHeaderTestFixture().number(0).buildHeader())
            .build();
    when(blockInteface.replaceRoundInBlock(proposedBlock, 0)).thenReturn(publishBlock);
    when(blockInteface.replaceRoundInBlock(publishBlock, 0)).thenReturn(commitBlock);

    final ConsensusRoundIdentifier priorRoundChange = new ConsensusRoundIdentifier(1, 0);
    final RoundState roundState = new RoundState(roundIdentifier, 2, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    final SignedData<PreparePayload> preparedPayload =
        messageFactory.createPrepare(priorRoundChange, proposedBlock.getHash()).getSignedPayload();

    final RoundChange roundChange =
        messageFactory.createRoundChange(
            roundIdentifier,
            Optional.of(new PreparedCertificate(proposedBlock, singletonList(preparedPayload), 2)));

    final RoundChangeArtifacts roundChangeArtifacts =
        RoundChangeArtifacts.create(singletonList(roundChange));

    round.startRoundWith(roundChangeArtifacts, 15);
    verify(transmitter, times(1))
        .multicastProposal(
            eq(roundIdentifier),
            blockCaptor.capture(),
            eq(singletonList(roundChange.getSignedPayload())),
            eq(singletonList(preparedPayload)));
    verify(transmitter, times(1))
        .multicastPrepare(eq(roundIdentifier), eq(blockCaptor.getValue().getHash()));

    // Inject a single Prepare message, and confirm the roundState has gone to Prepared (which
    // indicates the block has entered the roundState (note: all msgs are deemed valid due to mocks)
    round.handlePrepareMessage(
        messageFactory2.createPrepare(roundIdentifier, proposedBlock.getHash()));
    assertThat(roundState.isPrepared()).isTrue();
  }

  @Test
  public void creatingNewBlockFromEmptyPreparedCertificateUpdatesInternalState() {
    final QbftBlock commitBlock =
        new QbftBlockTestFixture()
            .blockHeader(new QbftBlockHeaderTestFixture().number(0).buildHeader())
            .build();
    when(blockInteface.replaceRoundInBlock(proposedBlock, 0)).thenReturn(commitBlock);

    final RoundState roundState = new RoundState(roundIdentifier, 2, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    final RoundChange roundChange =
        messageFactory.createRoundChange(roundIdentifier, Optional.empty());

    final RoundChangeArtifacts roundChangeArtifacts =
        RoundChangeArtifacts.create(List.of(roundChange));

    round.startRoundWith(roundChangeArtifacts, 15);
    verify(transmitter, times(1))
        .multicastProposal(
            eq(roundIdentifier),
            blockCaptor.capture(),
            eq(List.of(roundChange.getSignedPayload())),
            eq(Collections.emptyList()));
    verify(transmitter, times(1))
        .multicastPrepare(eq(roundIdentifier), eq(blockCaptor.getValue().getHash()));

    // Inject a single Prepare message, and confirm the roundState has gone to Prepared (which
    // indicates the block has entered the roundState (note: all msgs are deemed valid due to mocks)
    round.handlePrepareMessage(
        messageFactory2.createPrepare(roundIdentifier, proposedBlock.getHash()));
    assertThat(roundState.isPrepared()).isTrue();
  }

  @Test
  public void blockIsOnlyImportedOnceWhenCommitsAreReceivedBeforeProposal() {
    final ConsensusRoundIdentifier roundIdentifier = new ConsensusRoundIdentifier(1, 0);
    final int QUORUM_SIZE = 2;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    when(blockInteface.replaceRoundInBlock(proposedBlock, 0)).thenReturn(proposedBlock);
    when(blockCreator.createSealedBlock(eq(proposedBlock), eq(0), any())).thenReturn(proposedBlock);

    round.handleCommitMessage(
        messageFactory.createCommit(roundIdentifier, proposedBlock.getHash(), remoteCommitSeal));
    verify(blockImporter, never()).importBlock(any());

    round.handleProposalMessage(
        messageFactory.createProposal(
            roundIdentifier, proposedBlock, Collections.emptyList(), Collections.emptyList()));
    verify(blockImporter).importBlock(proposedBlock);
  }

  @Test
  public void blockIsImportedOnlyOnceIfQuorumCommitsAreReceivedPriorToProposal() {
    final int QUORUM_SIZE = 1;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            messageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    when(blockInteface.replaceRoundInBlock(eq(proposedBlock), eq(0))).thenReturn(proposedBlock);
    when(blockCreator.createSealedBlock(eq(proposedBlock), eq(0), any())).thenReturn(proposedBlock);

    round.handleCommitMessage(
        messageFactory.createCommit(roundIdentifier, proposedBlock.getHash(), remoteCommitSeal));
    verify(blockImporter, never()).importBlock(any());

    round.handleProposalMessage(
        messageFactory.createProposal(
            roundIdentifier, proposedBlock, Collections.emptyList(), Collections.emptyList()));
    verify(blockImporter).importBlock(proposedBlock);
  }

  @Test
  public void exceptionDuringNodeKeySigningDoesNotEscape() {
    final int QUORUM_SIZE = 1;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final NodeKey throwingNodeKey = mock(NodeKey.class);
    final MessageFactory throwingMessageFactory = new MessageFactory(throwingNodeKey, blockEncoder);
    when(throwingNodeKey.sign(any())).thenThrow(new SecurityModuleException("Hsm is Offline"));

    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            throwingNodeKey,
            throwingMessageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    when(blockInteface.replaceRoundInBlock(eq(proposedBlock), eq(0))).thenReturn(proposedBlock);

    round.handleProposalMessage(
        messageFactory.createProposal(
            roundIdentifier, proposedBlock, Collections.emptyList(), Collections.emptyList()));

    // Verify that no prepare message was constructed by the QbftRound
    assertThat(roundState.constructPreparedCertificate()).isEmpty();

    verifyNoInteractions(transmitter);
  }
}
