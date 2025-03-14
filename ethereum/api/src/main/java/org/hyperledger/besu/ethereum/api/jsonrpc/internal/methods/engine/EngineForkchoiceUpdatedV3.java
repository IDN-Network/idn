/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine;

import static org.idnecology.idn.datatypes.HardforkId.MainnetHardforkId.CANCUN;

import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.EngineForkchoiceUpdatedParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.EnginePayloadAttributesParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;

import java.util.Optional;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineForkchoiceUpdatedV3 extends AbstractEngineForkchoiceUpdated {

  private static final Logger LOG = LoggerFactory.getLogger(EngineForkchoiceUpdatedV3.class);

  public EngineForkchoiceUpdatedV3(
      final Vertx vertx,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final MergeMiningCoordinator mergeCoordinator,
      final EngineCallListener engineCallListener) {
    super(vertx, protocolSchedule, protocolContext, mergeCoordinator, engineCallListener);
  }

  @Override
  public String getName() {
    return RpcMethod.ENGINE_FORKCHOICE_UPDATED_V3.getMethodName();
  }

  @Override
  protected ValidationResult<RpcErrorType> validateParameter(
      final EngineForkchoiceUpdatedParameter fcuParameter,
      final Optional<EnginePayloadAttributesParameter> maybePayloadAttributes) {
    if (fcuParameter.getHeadBlockHash() == null) {
      return ValidationResult.invalid(
          getInvalidPayloadAttributesError(), "Missing head block hash");
    } else if (fcuParameter.getSafeBlockHash() == null) {
      return ValidationResult.invalid(
          getInvalidPayloadAttributesError(), "Missing safe block hash");
    } else if (fcuParameter.getFinalizedBlockHash() == null) {
      return ValidationResult.invalid(
          getInvalidPayloadAttributesError(), "Missing finalized block hash");
    }
    if (maybePayloadAttributes.isPresent()) {
      if (maybePayloadAttributes.get().getParentBeaconBlockRoot() == null) {
        return ValidationResult.invalid(
            getInvalidPayloadAttributesError(), "Missing parent beacon block root hash");
      }
    }
    return ValidationResult.valid();
  }

  @Override
  protected ValidationResult<RpcErrorType> validateForkSupported(final long blockTimestamp) {
    return ForkSupportHelper.validateForkSupported(CANCUN, cancunMilestone, blockTimestamp);
  }

  @Override
  protected Optional<JsonRpcErrorResponse> isPayloadAttributesValid(
      final Object requestId, final EnginePayloadAttributesParameter payloadAttributes) {

    if (payloadAttributes.getParentBeaconBlockRoot() == null) {
      LOG.error(
          "Parent beacon block root hash not present in payload attributes after cancun hardfork");
      return Optional.of(new JsonRpcErrorResponse(requestId, getInvalidPayloadAttributesError()));
    }

    if (payloadAttributes.getTimestamp() == 0) {
      return Optional.of(new JsonRpcErrorResponse(requestId, getInvalidPayloadAttributesError()));
    }

    ValidationResult<RpcErrorType> forkValidationResult =
        validateForkSupported(payloadAttributes.getTimestamp());
    if (!forkValidationResult.isValid()) {
      return Optional.of(new JsonRpcErrorResponse(requestId, forkValidationResult));
    }

    return Optional.empty();
  }
}
