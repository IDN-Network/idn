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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.enclave.EnclaveClientException;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcEnclaveErrorConverter;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionGroupResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionLegacyResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionResult;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivateTransaction;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(since = "24.12.0")
public class PrivGetPrivateTransaction implements JsonRpcMethod {

  private static final Logger LOG = LoggerFactory.getLogger(PrivGetPrivateTransaction.class);

  private final PrivacyController privacyController;
  private final PrivacyIdProvider privacyIdProvider;

  public PrivGetPrivateTransaction(
      final PrivacyController privacyController, final PrivacyIdProvider privacyIdProvider) {
    this.privacyController = privacyController;
    this.privacyIdProvider = privacyIdProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_GET_PRIVATE_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    LOG.trace("Executing {}", RpcMethod.PRIV_GET_PRIVATE_TRANSACTION.getMethodName());

    final Hash hash;
    try {
      hash = requestContext.getRequiredParameter(0, Hash.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction hash parameter (index 0)",
          RpcErrorType.INVALID_TRANSACTION_HASH_PARAMS,
          e);
    }
    final String enclaveKey = privacyIdProvider.getPrivacyUserId(requestContext.getUser());

    final Optional<PrivateTransaction> maybePrivateTx;
    try {
      maybePrivateTx =
          privacyController
              .findPrivateTransactionByPmtHash(hash, enclaveKey)
              .map(PrivateTransaction.class::cast);
    } catch (EnclaveClientException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(),
          JsonRpcEnclaveErrorConverter.convertEnclaveInvalidReason(e.getMessage()));
    }

    return maybePrivateTx
        .map(this::mapTransactionResult)
        .map(result -> new JsonRpcSuccessResponse(requestContext.getRequest().getId(), result))
        .orElse(new JsonRpcSuccessResponse(requestContext.getRequest().getId(), null));
  }

  private PrivateTransactionResult mapTransactionResult(
      final PrivateTransaction privateTransaction) {
    if (privateTransaction.getPrivacyGroupId().isPresent()) {
      return new PrivateTransactionGroupResult(privateTransaction);
    } else {
      return new PrivateTransactionLegacyResult(privateTransaction);
    }
  }
}
