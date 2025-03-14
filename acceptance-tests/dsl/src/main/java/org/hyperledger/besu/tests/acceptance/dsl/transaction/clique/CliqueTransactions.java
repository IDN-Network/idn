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
package org.idnecology.idn.tests.acceptance.dsl.transaction.clique;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

public class CliqueTransactions {
  public static final String LATEST = "latest";

  public CliquePropose createRemoveProposal(final IdnNode node) {
    return propose(node.getAddress().toString(), false);
  }

  public CliquePropose createAddProposal(final IdnNode node) {
    return propose(node.getAddress().toString(), true);
  }

  private CliquePropose propose(final String address, final boolean auth) {
    return new CliquePropose(address, auth);
  }

  public CliqueProposals createProposals() {
    return new CliqueProposals();
  }

  public CliqueGetSigners createGetSigners(final String blockNumber) {
    return new CliqueGetSigners(blockNumber);
  }

  public CliqueGetSignersAtHash createGetSignersAtHash(final Hash blockHash) {
    return new CliqueGetSignersAtHash(blockHash);
  }

  public CliqueDiscard createDiscardProposal(final IdnNode node) {
    return new CliqueDiscard(node.getAddress().toString());
  }
}
