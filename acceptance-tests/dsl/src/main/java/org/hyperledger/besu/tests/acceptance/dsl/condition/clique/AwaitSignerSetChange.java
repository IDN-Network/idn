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
package org.idnecology.idn.tests.acceptance.dsl.condition.clique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.tests.acceptance.dsl.transaction.clique.CliqueTransactions.LATEST;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.tests.acceptance.dsl.WaitUtils;
import org.idnecology.idn.tests.acceptance.dsl.condition.Condition;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.transaction.clique.CliqueTransactions;

import java.util.List;

public class AwaitSignerSetChange implements Condition {

  private final CliqueTransactions clique;
  private final List<Address> initialSigners;

  public AwaitSignerSetChange(final List<Address> initialSigners, final CliqueTransactions clique) {
    this.initialSigners = initialSigners;
    this.clique = clique;
  }

  @Override
  public void verify(final Node node) {
    WaitUtils.waitFor(
        60,
        () ->
            assertThat(node.execute(clique.createGetSigners(LATEST))).isNotEqualTo(initialSigners));
  }
}
