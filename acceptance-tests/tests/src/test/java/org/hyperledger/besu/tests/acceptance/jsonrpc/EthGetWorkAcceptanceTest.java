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
package org.idnecology.idn.tests.acceptance.jsonrpc;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class EthGetWorkAcceptanceTest extends AcceptanceTestBase {

  private Node minerNode;
  private Node fullNode;

  @BeforeEach
  public void setUp() throws Exception {
    minerNode = idn.createMinerNode("node1");
    fullNode = idn.createArchiveNode("node2");
    cluster.start(minerNode, fullNode);
  }

  @Test
  @Disabled("Genuinely Flakey")
  public void shouldReturnSuccessResponseWhenMining() {
    minerNode.verify(eth.getWork());
  }

  @Test
  public void shouldReturnErrorResponseWhenNotMining() {
    fullNode.verify(eth.getWorkExceptional("No mining work available yet"));
  }
}
