/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.consensus.qbft.jsonrpc.methods;

import org.idnecology.idn.config.BftConfigOptions;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;

/**
 * Implements the qbft_getRequestTimeoutSeconds RPC method to retrieve the QBFT request timeout in
 * seconds.
 */
public class QbftGetRequestTimeoutSeconds implements JsonRpcMethod {

  private final BftConfigOptions bftConfig;

  /**
   * Constructs a new QbftGetRequestTimeoutSeconds instance.
   *
   * @param bftConfig The BFT configuration options
   */
  public QbftGetRequestTimeoutSeconds(final BftConfigOptions bftConfig) {
    this.bftConfig = bftConfig;
  }

  @Override
  public String getName() {
    return RpcMethod.QBFT_GET_REQUEST_TIMEOUT_SECONDS.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), bftConfig.getRequestTimeoutSeconds());
  }
}
