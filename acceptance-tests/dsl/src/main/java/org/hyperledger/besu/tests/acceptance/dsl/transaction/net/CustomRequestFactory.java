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
package org.idnecology.idn.tests.acceptance.dsl.transaction.net;

import java.util.Collections;
import java.util.Map;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class CustomRequestFactory {
  private final Web3jService web3jService;

  public static class NetServicesResponse extends Response<Map<String, Map<String, String>>> {}

  public static class TransactionReceiptWithRevertReason extends TransactionReceipt {
    private String revertReason;

    public TransactionReceiptWithRevertReason() {}

    @Override
    public void setRevertReason(final String revertReason) {
      this.revertReason = revertReason;
    }

    @Override
    public String getRevertReason() {
      return revertReason;
    }
  }

  public static class EthGetTransactionReceiptWithRevertReasonResponse
      extends Response<TransactionReceiptWithRevertReason> {}

  public CustomRequestFactory(final Web3jService web3jService) {
    this.web3jService = web3jService;
  }

  public Request<?, NetServicesResponse> netServices() {
    return new Request<>(
        "net_services", Collections.emptyList(), web3jService, NetServicesResponse.class);
  }

  public Request<?, EthGetTransactionReceiptWithRevertReasonResponse>
      ethGetTransactionReceiptWithRevertReason(final String transactionHash) {
    return new Request<>(
        "eth_getTransactionReceipt",
        Collections.singletonList(transactionHash),
        web3jService,
        EthGetTransactionReceiptWithRevertReasonResponse.class);
  }
}
