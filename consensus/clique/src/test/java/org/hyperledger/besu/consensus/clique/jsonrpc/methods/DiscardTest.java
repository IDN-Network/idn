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
package org.idnecology.idn.consensus.clique.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.VoteType;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.plugin.services.rpc.RpcResponseType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DiscardTest {
  private final String JSON_RPC_VERSION = "2.0";
  private final String METHOD = "clique_discard";

  private ValidatorProvider validatorProvider;

  @BeforeEach
  public void setup() {
    final Blockchain blockchain = mock(Blockchain.class);
    final EpochManager epochManager = mock(EpochManager.class);
    final BlockInterface blockInterface = mock(BlockInterface.class);
    validatorProvider =
        BlockValidatorProvider.nonForkingValidatorProvider(
            blockchain, epochManager, blockInterface);
  }

  @Test
  public void discardEmpty() {
    final Discard discard = new Discard(validatorProvider);
    final Address a0 = Address.fromHexString("0");

    final JsonRpcResponse response = discard.response(requestWithParams(a0));

    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
  }

  @Test
  public void discardAuth() {
    final Discard discard = new Discard(validatorProvider);
    final Address a0 = Address.fromHexString("0");

    validatorProvider.getVoteProviderAtHead().get().authVote(a0);

    final JsonRpcResponse response = discard.response(requestWithParams(a0));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void discardDrop() {
    final Discard discard = new Discard(validatorProvider);
    final Address a0 = Address.fromHexString("0");

    validatorProvider.getVoteProviderAtHead().get().dropVote(a0);

    final JsonRpcResponse response = discard.response(requestWithParams(a0));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void discardIsolation() {
    final Discard discard = new Discard(validatorProvider);
    final Address a0 = Address.fromHexString("0");
    final Address a1 = Address.fromHexString("1");

    validatorProvider.getVoteProviderAtHead().get().authVote(a0);
    validatorProvider.getVoteProviderAtHead().get().authVote(a1);

    final JsonRpcResponse response = discard.response(requestWithParams(a0));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.ADD);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void discardWithoutAddress() {
    final Discard discard = new Discard(validatorProvider);

    assertThatThrownBy(() -> discard.response(requestWithParams()))
        .hasMessage("Invalid address parameter (index 0)")
        .isInstanceOf(InvalidJsonRpcParameters.class);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, METHOD, params));
  }
}
