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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.PluginsReloadConfiguration;
import org.idnecology.idn.plugin.IdnPlugin;

import java.util.Map;

public class PluginsJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final Map<String, IdnPlugin> namedPlugins;

  public PluginsJsonRpcMethods(final Map<String, IdnPlugin> namedPlugins) {
    this.namedPlugins = namedPlugins;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.PLUGINS.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    return mapOf(new PluginsReloadConfiguration(namedPlugins));
  }
}
