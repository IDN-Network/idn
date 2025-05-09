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
package org.idnecology.idn.consensus.ibft.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.VoteProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IbftProposeValidatorVoteTest {
  private final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
  private final VoteProvider voteProvider = mock(VoteProvider.class);
  private final String IBFT_METHOD = "ibft_proposeValidatorVote";
  private final String JSON_RPC_VERSION = "2.0";
  private IbftProposeValidatorVote method;

  @BeforeEach
  public void setup() {
    method = new IbftProposeValidatorVote(validatorProvider);
    when(validatorProvider.getVoteProviderAtHead()).thenReturn(Optional.of(voteProvider));
  }

  @Test
  public void returnsCorrectMethodName() {
    assertThat(method.getName()).isEqualTo(IBFT_METHOD);
  }

  @Test
  public void exceptionWhenNoParamsSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams()))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid address parameter (index 0)");
  }

  @Test
  public void exceptionWhenNoAuthSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams(Address.fromHexString("1"))))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid vote type parameter (index 1)");
  }

  @Test
  public void exceptionWhenNoAddressSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams("true")))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Invalid address parameter (index 0)");
  }

  @Test
  public void exceptionWhenInvalidBoolParameterSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams(Address.fromHexString("1"), "c")))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Invalid vote type parameter (index 1)");
  }

  @Test
  public void addValidator() {
    final Address parameterAddress = Address.fromHexString("1");
    final JsonRpcRequestContext request = requestWithParams(parameterAddress, "true");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(request.getRequest().getId(), true);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(voteProvider).authVote(parameterAddress);
  }

  @Test
  public void removeValidator() {
    final Address parameterAddress = Address.fromHexString("1");
    final JsonRpcRequestContext request = requestWithParams(parameterAddress, "false");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(request.getRequest().getId(), true);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);

    verify(voteProvider).dropVote(parameterAddress);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, IBFT_METHOD, params));
  }
}
