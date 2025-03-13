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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.Tracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionDetails;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DebugTraceBlockByHashTest {
  @Mock private ProtocolSchedule protocolSchedule;
  @Mock private BlockchainQueries blockchainQueries;
  @Mock private ObservableMetricsSystem metricsSystem;
  @Mock private Blockchain blockchain;
  private DebugTraceBlockByHash debugTraceBlockByHash;

  @BeforeEach
  public void setUp() {
    debugTraceBlockByHash =
        new DebugTraceBlockByHash(
            protocolSchedule, blockchainQueries, metricsSystem, new DeterministicEthScheduler());
  }

  @Test
  public void nameShouldBeDebugTraceBlockByHash() {
    assertThat(debugTraceBlockByHash.getName()).isEqualTo("debug_traceBlockByHash");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReturnCorrectResponse() {
    final Block block =
        new BlockDataGenerator()
            .block(
                BlockDataGenerator.BlockOptions.create()
                    .setBlockHeaderFunctions(new MainnetBlockHeaderFunctions()));

    final Object[] params = new Object[] {block.getHash()};
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", "debug_traceBlockByHash", params));

    when(blockchainQueries.getBlockchain()).thenReturn(blockchain);
    when(blockchain.getBlockByHash(block.getHash())).thenReturn(Optional.of(block));

    DebugTraceTransactionDetails result1 = mock(DebugTraceTransactionDetails.class);
    DebugTraceTransactionDetails result2 = mock(DebugTraceTransactionDetails.class);

    List<DebugTraceTransactionDetails> resultList = Arrays.asList(result1, result2);

    try (MockedStatic<Tracer> mockedTracer = mockStatic(Tracer.class)) {
      mockedTracer
          .when(
              () ->
                  Tracer.processTracing(
                      eq(blockchainQueries),
                      eq(Optional.of(block.getHeader())),
                      any(Function.class)))
          .thenReturn(Optional.of(resultList));

      final JsonRpcResponse jsonRpcResponse = debugTraceBlockByHash.response(request);
      assertThat(jsonRpcResponse).isInstanceOf(JsonRpcSuccessResponse.class);
      JsonRpcSuccessResponse response = (JsonRpcSuccessResponse) jsonRpcResponse;

      final Collection<DebugTraceTransactionDetails> traceResult = getResult(response);
      assertThat(traceResult).isNotEmpty();
      assertThat(traceResult).isInstanceOf(Collection.class).hasSize(2);
      assertThat(traceResult).containsExactly(result1, result2);
    }
  }

  @SuppressWarnings("unchecked")
  private Collection<DebugTraceTransactionDetails> getResult(
      final JsonRpcSuccessResponse response) {
    return (Collection<DebugTraceTransactionDetails>) response.getResult();
  }

  @Test
  public void shouldHandleInvalidParametersGracefully() {
    final Object[] invalidParams = new Object[] {"aaaa"};
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("2.0", "debug_traceBlockByHash", invalidParams));

    assertThatThrownBy(() -> debugTraceBlockByHash.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Invalid block hash parameter");
  }
}
