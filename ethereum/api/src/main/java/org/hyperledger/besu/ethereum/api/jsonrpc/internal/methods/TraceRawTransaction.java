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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INTERNAL_ERROR;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.util.DomainObjectDecodeUtils;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.rlp.RLPException;
import org.idnecology.idn.ethereum.transaction.CallParameter;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.vm.DebugOperationTracer;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceRawTransaction extends AbstractTraceByBlock implements JsonRpcMethod {
  private static final Logger LOG = LoggerFactory.getLogger(TraceRawTransaction.class);

  public TraceRawTransaction(
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries,
      final TransactionSimulator transactionSimulator) {
    super(blockchainQueries, protocolSchedule, transactionSimulator);
  }

  @Override
  public String getName() {
    return transactionSimulator != null ? RpcMethod.TRACE_RAW_TRANSACTION.getMethodName() : null;
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext request, final long blockNumber) {
    // this method does not get called because response() does the work
    return null;
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    if (requestContext.getRequest().getParamLength() != 2) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_PARAM_COUNT);
    }

    final String rawTransaction;
    try {
      rawTransaction = requestContext.getRequiredParameter(0, String.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction parameters (index 0)", RpcErrorType.INVALID_TRANSACTION_PARAMS, e);
    }
    final TraceTypeParameter traceTypeParameter;
    try {
      traceTypeParameter = requestContext.getRequiredParameter(1, TraceTypeParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid trace type parameter (index 1)", RpcErrorType.INVALID_TRACE_TYPE_PARAMS, e);
    }
    LOG.trace(
        "Received RPC rpcName={} rawTx={} traceType={}",
        getName(),
        rawTransaction,
        traceTypeParameter);

    final Transaction transaction;
    try {
      transaction = DomainObjectDecodeUtils.decodeRawTransaction(rawTransaction);
      LOG.trace("rawTx decoded to transaction {}", transaction);
    } catch (final RLPException | IllegalArgumentException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_TRANSACTION_PARAMS);
    }

    final Set<TraceTypeParameter.TraceType> traceTypes = traceTypeParameter.getTraceTypes();
    final DebugOperationTracer tracer =
        new DebugOperationTracer(buildTraceOptions(traceTypes), false);
    final BlockHeader headBlock = blockchainQueriesSupplier.get().headBlockHeader();
    return transactionSimulator
        .process(
            CallParameter.fromTransaction(transaction),
            buildTransactionValidationParams(),
            tracer,
            (mutableWorldState, transactionSimulatorResult) ->
                transactionSimulatorResult.map(
                    result -> {
                      final TransactionTrace transactionTrace =
                          new TransactionTrace(
                              result.transaction(), result.result(), tracer.getTraceFrames());
                      final Optional<Block> maybeBlock =
                          blockchainQueriesSupplier
                              .get()
                              .getBlockchain()
                              .getBlockByNumber(headBlock.getNumber());

                      if (maybeBlock.isEmpty()) {
                        return new JsonRpcErrorResponse(
                            requestContext.getRequest().getId(), INTERNAL_ERROR);
                      }

                      final Block block = maybeBlock.get();
                      final Object response =
                          getTraceCallResult(
                              protocolSchedule, traceTypes, result, transactionTrace, block);

                      return new JsonRpcSuccessResponse(
                          requestContext.getRequest().getId(), response);
                    }),
            headBlock)
        .orElse(new JsonRpcErrorResponse(requestContext.getRequest().getId(), INTERNAL_ERROR));
  }
}
