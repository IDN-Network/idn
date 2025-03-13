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

import static org.idnecology.idn.services.pipeline.PipelineBuilder.createPipelineFrom;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TransactionTraceParams;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.Tracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionResult;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.vm.DebugOperationTracer;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.metrics.LabelledMetric;
import org.idnecology.idn.services.pipeline.Pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public abstract class AbstractDebugTraceBlock implements JsonRpcMethod {

  private final ProtocolSchedule protocolSchedule;
  private final LabelledMetric<Counter> outputCounter;
  private final Supplier<BlockchainQueries> blockchainQueriesSupplier;
  private final EthScheduler ethScheduler;

  public AbstractDebugTraceBlock(
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries,
      final ObservableMetricsSystem metricsSystem,
      final EthScheduler ethScheduler) {
    this.blockchainQueriesSupplier = Suppliers.ofInstance(blockchainQueries);
    this.protocolSchedule = protocolSchedule;
    this.outputCounter =
        metricsSystem.createLabelledCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "transactions_debugTraceblock_pipeline_processed_total",
            "Number of transactions processed for each block",
            "step",
            "action");
    this.ethScheduler = ethScheduler;
  }

  protected BlockchainQueries getBlockchainQueries() {
    return blockchainQueriesSupplier.get();
  }

  protected TraceOptions getTraceOptions(final JsonRpcRequestContext requestContext) {
    final TraceOptions traceOptions;
    try {
      traceOptions =
          requestContext
              .getOptionalParameter(1, TransactionTraceParams.class)
              .map(TransactionTraceParams::traceOptions)
              .orElse(TraceOptions.DEFAULT);
    } catch (JsonRpcParameter.JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction trace parameter (index 1)",
          RpcErrorType.INVALID_TRANSACTION_TRACE_PARAMS,
          e);
    }
    return traceOptions;
  }

  protected Collection<DebugTraceTransactionResult> getTraces(
      final JsonRpcRequestContext requestContext,
      final TraceOptions traceOptions,
      final Optional<Block> maybeBlock) {
    return maybeBlock
        .flatMap(
            block ->
                Tracer.processTracing(
                    getBlockchainQueries(),
                    Optional.of(block.getHeader()),
                    traceableState -> {
                      List<DebugTraceTransactionResult> tracesList =
                          Collections.synchronizedList(new ArrayList<>());
                      final ProtocolSpec protocolSpec =
                          protocolSchedule.getByBlockHeader(block.getHeader());
                      final MainnetTransactionProcessor transactionProcessor =
                          protocolSpec.getTransactionProcessor();
                      final TraceBlock.ChainUpdater chainUpdater =
                          new TraceBlock.ChainUpdater(traceableState);

                      TransactionSource transactionSource = new TransactionSource(block);
                      DebugOperationTracer debugOperationTracer =
                          new DebugOperationTracer(traceOptions, true);
                      ExecuteTransactionStep executeTransactionStep =
                          new ExecuteTransactionStep(
                              chainUpdater,
                              transactionProcessor,
                              getBlockchainQueries().getBlockchain(),
                              debugOperationTracer,
                              protocolSpec,
                              block);
                      DebugTraceTransactionStep debugTraceTransactionStep =
                          new DebugTraceTransactionStep();
                      Pipeline<TransactionTrace> traceBlockPipeline =
                          createPipelineFrom(
                                  "getTransactions",
                                  transactionSource,
                                  4,
                                  outputCounter,
                                  false,
                                  "debug_trace_block")
                              .thenProcess("executeTransaction", executeTransactionStep)
                              .thenProcessAsyncOrdered(
                                  "debugTraceTransactionStep", debugTraceTransactionStep, 4)
                              .andFinishWith("collect_results", tracesList::add);

                      try {
                        ethScheduler.startPipeline(traceBlockPipeline).get();
                      } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                      }
                      return Optional.of(tracesList);
                    }))
        .orElse(null);
  }
}
