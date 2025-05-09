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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import static org.idnecology.idn.datatypes.HardforkId.MainnetHardforkId.CANCUN;
import static org.idnecology.idn.datatypes.HardforkId.MainnetHardforkId.PRAGUE;

import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineExchangeCapabilities;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineExchangeTransitionConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineForkchoiceUpdatedV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineForkchoiceUpdatedV2;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineForkchoiceUpdatedV3;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetBlobsV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetClientVersionV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadBodiesByHashV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadBodiesByRangeV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadV2;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadV3;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineGetPayloadV4;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineNewPayloadV1;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineNewPayloadV2;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineNewPayloadV3;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineNewPayloadV4;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EnginePreparePayloadDebug;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine.EngineQosTimer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.Vertx;

public class ExecutionEngineJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockResultFactory blockResultFactory = new BlockResultFactory();

  private final Optional<MergeMiningCoordinator> mergeCoordinator;
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthPeers ethPeers;
  private final Vertx consensusEngineServer;
  private final String clientVersion;
  private final String commit;
  private final TransactionPool transactionPool;
  private final MetricsSystem metricsSystem;

  ExecutionEngineJsonRpcMethods(
      final MiningCoordinator miningCoordinator,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthPeers ethPeers,
      final Vertx consensusEngineServer,
      final String clientVersion,
      final String commit,
      final TransactionPool transactionPool,
      final MetricsSystem metricsSystem) {
    this.mergeCoordinator =
        Optional.ofNullable(miningCoordinator)
            .filter(mc -> mc.isCompatibleWithEngineApi())
            .map(MergeMiningCoordinator.class::cast);
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethPeers = ethPeers;
    this.consensusEngineServer = consensusEngineServer;
    this.clientVersion = clientVersion;
    this.commit = commit;
    this.transactionPool = transactionPool;
    this.metricsSystem = metricsSystem;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.ENGINE.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final EngineQosTimer engineQosTimer = new EngineQosTimer(consensusEngineServer);
    if (mergeCoordinator.isPresent()) {
      List<JsonRpcMethod> executionEngineApisSupported = new ArrayList<>();
      executionEngineApisSupported.addAll(
          Arrays.asList(
              new EngineGetPayloadV1(
                  consensusEngineServer,
                  protocolContext,
                  mergeCoordinator.get(),
                  blockResultFactory,
                  engineQosTimer),
              new EngineGetPayloadV2(
                  consensusEngineServer,
                  protocolContext,
                  mergeCoordinator.get(),
                  blockResultFactory,
                  engineQosTimer,
                  protocolSchedule),
              new EngineNewPayloadV1(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  ethPeers,
                  engineQosTimer,
                  metricsSystem),
              new EngineNewPayloadV2(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  ethPeers,
                  engineQosTimer,
                  metricsSystem),
              new EngineNewPayloadV3(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  ethPeers,
                  engineQosTimer,
                  metricsSystem),
              new EngineForkchoiceUpdatedV1(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  engineQosTimer),
              new EngineForkchoiceUpdatedV2(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  engineQosTimer),
              new EngineForkchoiceUpdatedV3(
                  consensusEngineServer,
                  protocolSchedule,
                  protocolContext,
                  mergeCoordinator.get(),
                  engineQosTimer),
              new EngineExchangeTransitionConfiguration(
                  consensusEngineServer, protocolContext, engineQosTimer),
              new EngineGetPayloadBodiesByHashV1(
                  consensusEngineServer, protocolContext, blockResultFactory, engineQosTimer),
              new EngineGetPayloadBodiesByRangeV1(
                  consensusEngineServer, protocolContext, blockResultFactory, engineQosTimer),
              new EngineExchangeCapabilities(
                  consensusEngineServer, protocolContext, engineQosTimer),
              new EnginePreparePayloadDebug(
                  consensusEngineServer, protocolContext, engineQosTimer, mergeCoordinator.get()),
              new EngineGetClientVersionV1(
                  consensusEngineServer, protocolContext, engineQosTimer, clientVersion, commit),
              new EngineGetBlobsV1(
                  consensusEngineServer, protocolContext, engineQosTimer, transactionPool)));

      if (protocolSchedule.milestoneFor(CANCUN).isPresent()) {
        executionEngineApisSupported.add(
            new EngineGetPayloadV3(
                consensusEngineServer,
                protocolContext,
                mergeCoordinator.get(),
                blockResultFactory,
                engineQosTimer,
                protocolSchedule));
      }

      if (protocolSchedule.milestoneFor(PRAGUE).isPresent()) {
        executionEngineApisSupported.add(
            new EngineGetPayloadV4(
                consensusEngineServer,
                protocolContext,
                mergeCoordinator.get(),
                blockResultFactory,
                engineQosTimer,
                protocolSchedule));

        executionEngineApisSupported.add(
            new EngineNewPayloadV4(
                consensusEngineServer,
                protocolSchedule,
                protocolContext,
                mergeCoordinator.get(),
                ethPeers,
                engineQosTimer,
                metricsSystem));
      }

      return mapOf(executionEngineApisSupported);
    } else {
      return mapOf(
          new EngineExchangeTransitionConfiguration(
              consensusEngineServer, protocolContext, engineQosTimer));
    }
  }
}
