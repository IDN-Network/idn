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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class MinerGetExtraDataTest {

  @Test
  public void shouldReturnDefaultExtraData() {
    final MiningConfiguration miningConfiguration = ImmutableMiningConfiguration.newDefault();
    final MinerGetExtraData method = new MinerGetExtraData(miningConfiguration);
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", method.getName(), new Object[] {}));

    final JsonRpcResponse expected = new JsonRpcSuccessResponse(request.getRequest().getId(), "0x");

    final JsonRpcResponse actual = method.response(request);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void shouldReturnSetAtRuntimeExtraData() {
    final MiningConfiguration miningConfiguration = ImmutableMiningConfiguration.newDefault();
    final MinerGetExtraData method = new MinerGetExtraData(miningConfiguration);
    final var extraData = "0x123456";
    final Bytes extraDataAtRuntime = Bytes.fromHexString(extraData);

    miningConfiguration.setExtraData(extraDataAtRuntime);

    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", method.getName(), new Object[] {}));

    final JsonRpcResponse expected =
        new JsonRpcSuccessResponse(request.getRequest().getId(), extraData);

    final JsonRpcResponse actual = method.response(request);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
