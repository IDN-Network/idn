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
package org.idnecology.idn.tests.acceptance.dsl.transaction.bft;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;
import java.util.List;

public class BftGetValidators implements Transaction<List<Address>> {
  private final String blockNumber;

  public BftGetValidators(final String blockNumber) {
    this.blockNumber = blockNumber;
  }

  @Override
  public List<Address> execute(final NodeRequests node) {
    try {
      final BftRequestFactory.SignersBlockResponse result =
          node.bft().validatorsAtBlock(blockNumber).send();
      assertThat(result).isNotNull();
      assertThat(result.hasError()).isFalse();
      return result.getResult();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
