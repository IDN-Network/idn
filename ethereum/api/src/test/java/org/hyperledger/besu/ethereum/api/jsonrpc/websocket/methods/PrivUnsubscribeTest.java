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
package org.idnecology.idn.ethereum.api.jsonrpc.websocket.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionNotFoundException;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.InvalidSubscriptionRequestException;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.PrivateUnsubscribeRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

import io.vertx.core.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrivUnsubscribeTest {

  private final String PRIVACY_GROUP_ID = "B1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private final String CONNECTION_ID = "test-connection-id";

  @Mock private SubscriptionManager subscriptionManagerMock;
  @Mock private SubscriptionRequestMapper mapperMock;
  @Mock private PrivacyController privacyController;
  @Mock private PrivacyIdProvider privacyIdProvider;

  private PrivUnsubscribe privUnsubscribe;

  @BeforeEach
  public void before() {
    privUnsubscribe =
        new PrivUnsubscribe(
            subscriptionManagerMock, mapperMock, privacyController, privacyIdProvider);
  }

  @Test
  public void expectedMethodName() {
    assertThat(privUnsubscribe.getName()).isEqualTo("priv_unsubscribe");
  }

  @Test
  public void responseContainsUnsubscribeStatus() {
    final JsonRpcRequestContext request = createPrivUnsubscribeRequest();
    final PrivateUnsubscribeRequest unsubscribeRequest =
        new PrivateUnsubscribeRequest(1L, CONNECTION_ID, PRIVACY_GROUP_ID);
    when(mapperMock.mapPrivateUnsubscribeRequest(eq(request))).thenReturn(unsubscribeRequest);
    when(subscriptionManagerMock.unsubscribe(eq(unsubscribeRequest))).thenReturn(true);

    final JsonRpcSuccessResponse expectedResponse =
        new JsonRpcSuccessResponse(request.getRequest().getId(), true);

    assertThat(privUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  @Test
  public void invalidUnsubscribeRequestReturnsInvalidRequestResponse() {
    final JsonRpcRequestContext request = createPrivUnsubscribeRequest();
    when(mapperMock.mapPrivateUnsubscribeRequest(any()))
        .thenThrow(new InvalidSubscriptionRequestException());

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(request.getRequest().getId(), RpcErrorType.INVALID_REQUEST);

    assertThat(privUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  @Test
  public void whenSubscriptionNotFoundReturnError() {
    final JsonRpcRequestContext request = createPrivUnsubscribeRequest();
    when(mapperMock.mapPrivateUnsubscribeRequest(any()))
        .thenReturn(mock(PrivateUnsubscribeRequest.class));
    when(subscriptionManagerMock.unsubscribe(any()))
        .thenThrow(new SubscriptionNotFoundException(1L));

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(request.getRequest().getId(), RpcErrorType.SUBSCRIPTION_NOT_FOUND);

    assertThat(privUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  private JsonRpcRequestContext createPrivUnsubscribeRequest() {
    return new JsonRpcRequestContext(
        Json.decodeValue(
            "{\"id\": 1, \"method\": \"priv_unsubscribe\", \"params\": [\""
                + PRIVACY_GROUP_ID
                + "\", \"0x0\"]}",
            JsonRpcRequest.class));
  }
}
