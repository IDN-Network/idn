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
package org.idnecology.idn.tests.acceptance.clique;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class CliqueProposalRpcAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void shouldReturnProposals() throws IOException {
    final String[] initialValidators = {"miner1", "miner2"};
    final IdnNode minerNode1 = idn.createCliqueNodeWithValidators("miner1", initialValidators);
    final IdnNode minerNode2 = idn.createCliqueNodeWithValidators("miner2", initialValidators);
    final IdnNode minerNode3 = idn.createCliqueNodeWithValidators("miner3", initialValidators);
    cluster.start(minerNode1, minerNode2, minerNode3);

    cluster.verify(clique.noProposals());
    minerNode1.execute(cliqueTransactions.createAddProposal(minerNode3));
    minerNode1.execute(cliqueTransactions.createRemoveProposal(minerNode2));
    minerNode2.execute(cliqueTransactions.createRemoveProposal(minerNode3));

    minerNode1.verify(
        clique.proposalsEqual().addProposal(minerNode3).removeProposal(minerNode2).build());
    minerNode2.verify(clique.proposalsEqual().removeProposal(minerNode3).build());
    minerNode3.verify(clique.noProposals());
  }
}
