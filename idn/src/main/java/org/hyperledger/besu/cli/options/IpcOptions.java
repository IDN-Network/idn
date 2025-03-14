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
package org.idnecology.idn.cli.options;

import static org.idnecology.idn.ethereum.api.jsonrpc.RpcApis.DEFAULT_RPC_APIS;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine;

/** The Ipc CLI options. */
public class IpcOptions {
  private static final String DEFAULT_IPC_FILE = "idn.ipc";

  /** Default constructor. */
  IpcOptions() {}

  /**
   * Create ipc options.
   *
   * @return the ipc options
   */
  public static IpcOptions create() {
    return new IpcOptions();
  }

  /**
   * Gets default path.
   *
   * @param dataDir the data dir
   * @return the default path
   */
  public static Path getDefaultPath(final Path dataDir) {
    return dataDir.resolve(DEFAULT_IPC_FILE);
  }

  @CommandLine.Option(
      names = {"--Xrpc-ipc-enabled"},
      hidden = true,
      description = "Set to start the JSON-RPC IPC service (default: ${DEFAULT-VALUE})")
  private final Boolean enabled = false;

  @CommandLine.Option(
      names = {"--Xrpc-ipc-path"},
      hidden = true,
      description =
          "IPC socket/pipe file (default: a file named \""
              + DEFAULT_IPC_FILE
              + "\" in the Idn data directory)")
  private Path ipcPath;

  @CommandLine.Option(
      names = {"--Xrpc-ipc-api", "--Xrpc-ipc-apis"},
      hidden = true,
      paramLabel = "<api name>",
      split = " {0,1}, {0,1}",
      arity = "1..*",
      description =
          "Comma separated list of APIs to enable on JSON-RPC IPC service (default: ${DEFAULT-VALUE})")
  private final List<String> rpcIpcApis = DEFAULT_RPC_APIS;

  /**
   * Whether IPC options are enabled.
   *
   * @return true for enabled, false otherwise.
   */
  public Boolean isEnabled() {
    return enabled;
  }

  /**
   * Gets ipc path.
   *
   * @return the ipc path
   */
  public Path getIpcPath() {
    return ipcPath;
  }

  /**
   * Gets rpc ipc apis.
   *
   * @return the rpc ipc apis
   */
  public List<String> getRpcIpcApis() {
    return rpcIpcApis;
  }
}
