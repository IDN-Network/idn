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
package org.idnecology.idn.tests.acceptance.dsl.transaction.eth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.web3j.protocol.core.DefaultBlockParameterName.LATEST;

import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.protocol.core.methods.response.EthGetCode;

public class EthGetCodeTransaction implements Transaction<Bytes> {

  private final Account account;

  public EthGetCodeTransaction(final Account account) {
    this.account = account;
  }

  @Override
  public Bytes execute(final NodeRequests node) {
    try {
      final EthGetCode result = node.eth().ethGetCode(account.getAddress(), LATEST).send();
      assertThat(result).isNotNull();
      assertThat(result.hasError()).isFalse();

      return Bytes.fromHexString(result.getCode());

    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
