/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionResult;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPException;
import org.idnecology.idn.metrics.ObservableMetricsSystem;

import java.util.Collection;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugTraceBlock extends AbstractDebugTraceBlock {

  private static final Logger LOG = LoggerFactory.getLogger(DebugTraceBlock.class);
  private final BlockHeaderFunctions blockHeaderFunctions;

  public DebugTraceBlock(
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries,
      final ObservableMetricsSystem metricsSystem,
      final EthScheduler ethScheduler) {
    super(protocolSchedule, blockchainQueries, metricsSystem, ethScheduler);
    this.blockHeaderFunctions = ScheduleBasedBlockHeaderFunctions.create(protocolSchedule);
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_TRACE_BLOCK.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Block block;
    try {
      final String input = requestContext.getRequiredParameter(0, String.class);
      block = Block.readFrom(RLP.input(Bytes.fromHexString(input)), this.blockHeaderFunctions);
    } catch (final RLPException | IllegalArgumentException e) {
      LOG.debug("Failed to parse block RLP (index 0)", e);
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_BLOCK_PARAMS);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block params (index 0)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }
    final TraceOptions traceOptions = getTraceOptions(requestContext);

    if (getBlockchainQueries()
        .getBlockchain()
        .getBlockByHash(block.getHeader().getParentHash())
        .isPresent()) {
      final Collection<DebugTraceTransactionResult> results =
          getTraces(requestContext, traceOptions, Optional.ofNullable(block));
      return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), results);
    } else {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.PARENT_BLOCK_NOT_FOUND);
    }
  }
}
