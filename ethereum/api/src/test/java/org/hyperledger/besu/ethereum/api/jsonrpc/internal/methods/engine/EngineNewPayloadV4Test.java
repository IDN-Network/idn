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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.api.graphql.internal.response.GraphQLError.INVALID_PARAMS;
import static org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineTestSupport.fromErrorResp;
import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INVALID_EXECUTION_REQUESTS_PARAMS;
import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.UNSUPPORTED_FORK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.BlobGas;
import org.idnecology.idn.datatypes.RequestType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.BlockProcessingOutputs;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.EnginePayloadParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Request;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.mainnet.BodyValidation;
import org.idnecology.idn.ethereum.mainnet.ScheduledProtocolSpec;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.mainnet.requests.MainnetRequestsValidator;
import org.idnecology.idn.ethereum.mainnet.requests.ProhibitedRequestValidator;
import org.idnecology.idn.evm.gascalculator.PragueGasCalculator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EngineNewPayloadV4Test extends EngineNewPayloadV3Test {

  public EngineNewPayloadV4Test() {}

  @Override
  protected Set<ScheduledProtocolSpec.Hardfork> supportedHardforks() {
    return Set.of(pragueHardfork);
  }

  private static final List<Request> VALID_REQUESTS =
      List.of(
          new Request(RequestType.DEPOSIT, Bytes.of(1)),
          new Request(RequestType.WITHDRAWAL, Bytes.of(1)),
          new Request(RequestType.CONSOLIDATION, Bytes.of(1)));

  @BeforeEach
  @Override
  public void before() {
    super.before();
    maybeParentBeaconBlockRoot = Optional.of(Bytes32.ZERO);
    this.method =
        new EngineNewPayloadV4(
            vertx,
            protocolSchedule,
            protocolContext,
            mergeCoordinator,
            ethPeers,
            engineCallListener,
            new NoOpMetricsSystem());
    lenient().when(protocolSchedule.hardforkFor(any())).thenReturn(Optional.of(pragueHardfork));
    lenient().when(protocolSpec.getGasCalculator()).thenReturn(new PragueGasCalculator());
    mockAllowedRequestsValidator();
  }

  @Override
  public void shouldReturnExpectedMethodName() {
    assertThat(method.getName()).isEqualTo("engine_newPayloadV4");
  }

  @Override
  public void shouldReturnUnsupportedForkIfBlockTimestampIsAfterPragueMilestone() {
    // Only relevant for V3
  }

  @Test
  public void shouldReturnUnsupportedForkIfBlockTimestampIsBeforePragueMilestone() {
    final BlockHeader cancunHeader = createBlockHeaderFixtureForV3(Optional.empty()).buildHeader();

    var resp = resp(mockEnginePayload(cancunHeader, emptyList()));

    final JsonRpcError jsonRpcError = fromErrorResp(resp);
    assertThat(jsonRpcError.getCode()).isEqualTo(UNSUPPORTED_FORK.getCode());
    verify(engineCallListener, times(1)).executionEngineCalled();
  }

  @Test
  public void shouldReturnInvalidIfRequestsIsNull_WhenRequestsAllowed() {
    var resp =
        respWithInvalidRequests(
            mockEnginePayload(createValidBlockHeaderForV4(Optional.empty()), emptyList()));

    assertThat(fromErrorResp(resp).getCode()).isEqualTo(INVALID_PARAMS.getCode());
    assertThat(fromErrorResp(resp).getMessage())
        .isEqualTo(INVALID_EXECUTION_REQUESTS_PARAMS.getMessage());
    verify(engineCallListener, times(1)).executionEngineCalled();
  }

  @Test
  public void shouldReturnValidIfRequestsIsNotNull_WhenRequestsAllowed() {
    BlockHeader mockHeader =
        setupValidPayload(
            new BlockProcessingResult(
                Optional.of(
                    new BlockProcessingOutputs(null, List.of(), Optional.of(VALID_REQUESTS)))),
            Optional.empty());
    when(blockchain.getBlockHeader(mockHeader.getParentHash()))
        .thenReturn(Optional.of(mock(BlockHeader.class)));
    when(mergeCoordinator.getLatestValidAncestor(mockHeader))
        .thenReturn(Optional.of(mockHeader.getHash()));
    var resp = resp(mockEnginePayload(mockHeader, emptyList()));

    assertValidResponse(mockHeader, resp);
  }

  @Test
  public void shouldReturnInvalidIfRequestsIsNotNull_WhenRequestsProhibited() {
    mockProhibitedRequestsValidator();

    var resp = resp(mockEnginePayload(createValidBlockHeaderForV4(Optional.empty()), emptyList()));

    final JsonRpcError jsonRpcError = fromErrorResp(resp);
    assertThat(jsonRpcError.getCode()).isEqualTo(INVALID_PARAMS.getCode());
    verify(engineCallListener, times(1)).executionEngineCalled();
  }

  @Override
  @Test
  public void validateVersionedHash_whenListIsPresentAndEmpty() {
    final BlockHeader mockHeader =
        setupValidPayload(
            new BlockProcessingResult(Optional.of(new BlockProcessingOutputs(null, List.of()))),
            Optional.empty());
    final EnginePayloadParameter payload = mockEnginePayload(mockHeader, emptyList(), null);

    ValidationResult<RpcErrorType> res =
        method.validateParameters(
            payload,
            Optional.of(List.of()),
            Optional.of("0x0000000000000000000000000000000000000000000000000000000000000000"),
            Optional.of(List.of()));
    assertThat(res.isValid()).isTrue();
  }

  @Override
  @Test
  public void validateExecutionRequests_whenPresent() {
    final BlockHeader mockHeader =
        setupValidPayload(
            new BlockProcessingResult(Optional.of(new BlockProcessingOutputs(null, List.of()))),
            Optional.empty());
    final EnginePayloadParameter payload = mockEnginePayload(mockHeader, emptyList(), null);

    ValidationResult<RpcErrorType> res =
        method.validateParameters(
            payload,
            Optional.of(List.of()),
            Optional.of("0x0000000000000000000000000000000000000000000000000000000000000000"),
            Optional.of(emptyList()));
    assertThat(res.isValid()).isTrue();
  }

  private BlockHeader createValidBlockHeaderForV4(
      final Optional<List<Withdrawal>> maybeWithdrawals) {
    return createBlockHeaderFixtureForV3(maybeWithdrawals)
        .requestsHash(BodyValidation.requestsHash(VALID_REQUESTS))
        .timestamp(pragueHardfork.milestone())
        .buildHeader();
  }

  private BlockHeaderTestFixture createBlockHeaderFixtureForV3(
      final Optional<List<Withdrawal>> maybeWithdrawals) {
    BlockHeader parentBlockHeader =
        new BlockHeaderTestFixture()
            .baseFeePerGas(Wei.ONE)
            .timestamp(pragueHardfork.milestone() - 2) // cancun parent
            .excessBlobGas(BlobGas.ZERO)
            .blobGasUsed(0L)
            .buildHeader();

    return new BlockHeaderTestFixture()
        .baseFeePerGas(Wei.ONE)
        .parentHash(parentBlockHeader.getParentHash())
        .number(parentBlockHeader.getNumber() + 1)
        .timestamp(parentBlockHeader.getTimestamp() + 1)
        .withdrawalsRoot(maybeWithdrawals.map(BodyValidation::withdrawalsRoot).orElse(null))
        .excessBlobGas(BlobGas.ZERO)
        .blobGasUsed(0L)
        .parentBeaconBlockRoot(
            maybeParentBeaconBlockRoot.isPresent() ? maybeParentBeaconBlockRoot : null);
  }

  @Override
  protected BlockHeader createBlockHeader(final Optional<List<Withdrawal>> maybeWithdrawals) {
    return createValidBlockHeaderForV4(maybeWithdrawals);
  }

  @Override
  protected JsonRpcResponse resp(final EnginePayloadParameter payload) {
    final List<String> requestsWithoutRequestId =
        VALID_REQUESTS.stream()
            .sorted(Comparator.comparing(Request::getType))
            .map(
                r ->
                    Bytes.concatenate(Bytes.of(r.getType().getSerializedType()), r.getData())
                        .toHexString())
            .toList();
    Object[] params =
        maybeParentBeaconBlockRoot
            .map(
                bytes32 ->
                    new Object[] {
                      payload, emptyList(), bytes32.toHexString(), requestsWithoutRequestId
                    })
            .orElseGet(() -> new Object[] {payload});
    return method.response(
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", this.method.getName(), params)));
  }

  protected JsonRpcResponse respWithInvalidRequests(final EnginePayloadParameter payload) {
    Object[] params =
        maybeParentBeaconBlockRoot
            .map(
                bytes32 ->
                    new Object[] {payload, emptyList(), bytes32.toHexString()
                      // empty requests param is invalid
                    })
            .orElseGet(() -> new Object[] {payload});
    return method.response(
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", this.method.getName(), params)));
  }

  private void mockProhibitedRequestsValidator() {
    var validator = new ProhibitedRequestValidator();
    when(protocolSpec.getRequestsValidator()).thenReturn(validator);
  }

  private void mockAllowedRequestsValidator() {
    var validator = new MainnetRequestsValidator();
    when(protocolSpec.getRequestsValidator()).thenReturn(validator);
  }
}
