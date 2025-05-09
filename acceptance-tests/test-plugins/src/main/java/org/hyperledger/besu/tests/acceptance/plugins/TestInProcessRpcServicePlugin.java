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
package org.idnecology.idn.tests.acceptance.plugins;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.services.PicoCLIOptions;
import org.idnecology.idn.plugin.services.RpcEndpointService;
import org.idnecology.idn.plugin.services.rpc.RpcResponseType;

import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@AutoService(IdnPlugin.class)
public class TestInProcessRpcServicePlugin implements IdnPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(TestInProcessRpcServicePlugin.class);

  private RpcEndpointService rpcEndpointService;

  @CommandLine.Option(names = {"--plugin-test-set-min-gas-price"})
  long minGasPrice = -1;

  @Override
  public void register(final ServiceManager context) {
    final PicoCLIOptions cmdlineOptions =
        context
            .getService(PicoCLIOptions.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Failed to obtain PicoCLI options from the IdnContext"));

    cmdlineOptions.addPicoCLIOptions("test", this);

    rpcEndpointService =
        context
            .getService(RpcEndpointService.class)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Failed to obtain RpcEndpointService from the IdnContext."));
  }

  @Override
  public void start() {
    LOG.info("TestInProcessRpcServicePlugin minGasPrice option: {}", minGasPrice);
    if (minGasPrice >= 0) {
      callSetMinGasPrice(minGasPrice);
    }
  }

  @Override
  public void stop() {}

  private void callSetMinGasPrice(final long minGasPrice) {
    LOG.info("Setting minGasPrice via in-process RPC service");
    final var minGasPriceWei = Wei.of(minGasPrice);
    final var resp =
        rpcEndpointService.call(
            "miner_setMinGasPrice", new Object[] {minGasPriceWei.toShortHexString()});
    LOG.info("miner_setMinGasPrice response: {}", resp);
    if (!resp.getType().equals(RpcResponseType.SUCCESS)) {
      throw new RuntimeException("Internal setMinGasPrice method failed: " + resp);
    }
  }
}
