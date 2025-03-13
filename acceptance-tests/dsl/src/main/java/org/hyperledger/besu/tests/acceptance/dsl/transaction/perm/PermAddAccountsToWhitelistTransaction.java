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
package org.idnecology.idn.tests.acceptance.dsl.transaction.perm;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;
import org.idnecology.idn.tests.acceptance.dsl.transaction.perm.PermissioningJsonRpcRequestFactory.AddAccountsToWhitelistResponse;

import java.io.IOException;
import java.util.List;

public class PermAddAccountsToWhitelistTransaction implements Transaction<String> {

  private final List<String> accounts;

  public PermAddAccountsToWhitelistTransaction(final List<String> accounts) {
    this.accounts = accounts;
  }

  @Override
  public String execute(final NodeRequests node) {
    try {
      AddAccountsToWhitelistResponse response = node.perm().addAccountsToWhitelist(accounts).send();
      assertThat(response.getResult()).isEqualTo("Success");
      return response.getResult();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
