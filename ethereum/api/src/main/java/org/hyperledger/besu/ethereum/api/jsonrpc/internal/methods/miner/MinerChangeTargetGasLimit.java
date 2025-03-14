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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;

public class MinerChangeTargetGasLimit implements JsonRpcMethod {

  private final MiningCoordinator miningCoordinator;

  public MinerChangeTargetGasLimit(final MiningCoordinator miningCoordinator) {
    this.miningCoordinator = miningCoordinator;
  }

  @Override
  public String getName() {
    return RpcMethod.MINER_CHANGE_TARGET_GAS_LIMIT.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    try {
      miningCoordinator.changeTargetGasLimit(requestContext.getRequiredParameter(0, Long.class));
      return new JsonRpcSuccessResponse(requestContext.getRequest().getId());
    } catch (final IllegalArgumentException invalidJsonRpcParameters) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_TARGET_GAS_LIMIT_PARAMS);
    } catch (final UnsupportedOperationException unsupportedOperationException) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(),
          RpcErrorType.TARGET_GAS_LIMIT_MODIFICATION_UNSUPPORTED);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid target gas limit parameter (index 0)",
          RpcErrorType.INVALID_TARGET_GAS_LIMIT_PARAMS,
          e);
    }
  }
}
