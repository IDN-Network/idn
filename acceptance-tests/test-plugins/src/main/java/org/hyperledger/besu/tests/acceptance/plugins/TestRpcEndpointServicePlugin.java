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

import static com.google.common.base.Preconditions.checkArgument;

import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.services.RpcEndpointService;
import org.idnecology.idn.plugin.services.rpc.PluginRpcRequest;

import java.util.concurrent.atomic.AtomicReference;

import com.google.auto.service.AutoService;

@AutoService(IdnPlugin.class)
public class TestRpcEndpointServicePlugin implements IdnPlugin {

  private static final String NAMESPACE_ENABLED = "tests";
  private static final String NAMESPACE_NOT_ENABLED = "notEnabled";
  private final AtomicReference<String> stringStorage = new AtomicReference<>("InitialValue");
  private final AtomicReference<Object[]> arrayStorage = new AtomicReference<>();

  private String setValue(final PluginRpcRequest request) {
    checkArgument(request.getParams().length == 1, "Only one parameter accepted");
    return stringStorage.updateAndGet(x -> request.getParams()[0].toString());
  }

  private String getValue(final PluginRpcRequest request) {
    return stringStorage.get();
  }

  private Object[] replaceValueList(final PluginRpcRequest request) {
    return arrayStorage.updateAndGet(x -> request.getParams());
  }

  private String throwException(final PluginRpcRequest request) {
    throw new RuntimeException("Kaboom");
  }

  @Override
  public void register(final ServiceManager context) {
    context
        .getService(RpcEndpointService.class)
        .ifPresent(
            rpcEndpointService -> {
              rpcEndpointService.registerRPCEndpoint(NAMESPACE_ENABLED, "getValue", this::getValue);
              rpcEndpointService.registerRPCEndpoint(NAMESPACE_ENABLED, "setValue", this::setValue);
              rpcEndpointService.registerRPCEndpoint(
                  NAMESPACE_ENABLED, "replaceValueList", this::replaceValueList);
              rpcEndpointService.registerRPCEndpoint(
                  NAMESPACE_ENABLED, "throwException", this::throwException);
              rpcEndpointService.registerRPCEndpoint(
                  NAMESPACE_NOT_ENABLED, "getValue", this::getValue);
            });
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}
}
