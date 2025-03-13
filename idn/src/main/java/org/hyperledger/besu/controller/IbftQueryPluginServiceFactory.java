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
package org.idnecology.idn.controller;

import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.queries.BftQueryServiceImpl;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.ibft.queries.IbftQueryServiceImpl;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.plugin.services.metrics.PoAMetricsService;
import org.idnecology.idn.plugin.services.query.BftQueryService;
import org.idnecology.idn.plugin.services.query.IbftQueryService;
import org.idnecology.idn.plugin.services.query.PoaQueryService;
import org.idnecology.idn.services.IdnPluginContextImpl;

/** The IBFT query plugin service factory. */
public class IbftQueryPluginServiceFactory implements PluginServiceFactory {

  private final Blockchain blockchain;
  private final BftBlockInterface blockInterface;
  private final ValidatorProvider validatorProvider;
  private final NodeKey nodeKey;

  /**
   * Instantiates a new Ibft query plugin service factory.
   *
   * @param blockchain the blockchain
   * @param blockInterface the block interface
   * @param validatorProvider the validator provider
   * @param nodeKey the node key
   */
  public IbftQueryPluginServiceFactory(
      final Blockchain blockchain,
      final BftBlockInterface blockInterface,
      final ValidatorProvider validatorProvider,
      final NodeKey nodeKey) {
    this.blockchain = blockchain;
    this.blockInterface = blockInterface;
    this.validatorProvider = validatorProvider;
    this.nodeKey = nodeKey;
  }

  @Override
  public void appendPluginServices(final IdnPluginContextImpl idnContext) {
    final IbftQueryServiceImpl service =
        new IbftQueryServiceImpl(blockInterface, blockchain, nodeKey);
    idnContext.addService(IbftQueryService.class, service);
    idnContext.addService(PoaQueryService.class, service);
    idnContext.addService(PoAMetricsService.class, service);

    final BftQueryServiceImpl bftService =
        new BftQueryServiceImpl(blockInterface, blockchain, validatorProvider, nodeKey, "ibft");
    idnContext.addService(BftQueryService.class, bftService);
  }
}
