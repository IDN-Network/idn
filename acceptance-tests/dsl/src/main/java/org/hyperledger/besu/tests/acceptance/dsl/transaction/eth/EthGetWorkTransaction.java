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
package org.idnecology.idn.tests.acceptance.dsl.transaction.eth;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

import org.web3j.protocol.core.methods.response.EthGetWork;

public class EthGetWorkTransaction implements Transaction<String[]> {

  EthGetWorkTransaction() {}

  @Override
  public String[] execute(final NodeRequests node) {
    try {
      final EthGetWork result = node.eth().ethGetWork().send();
      assertThat(result).isNotNull();
      if (result.hasError()) {
        throw new RuntimeException(result.getError().getMessage());
      }
      return new String[] {
        result.getCurrentBlockHeaderPowHash(),
        result.getSeedHashForDag(),
        result.getBoundaryCondition()
      };
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
