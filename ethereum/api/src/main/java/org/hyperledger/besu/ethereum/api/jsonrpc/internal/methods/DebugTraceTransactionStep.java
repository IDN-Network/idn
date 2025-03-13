/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DebugTraceTransactionStep
    implements Function<TransactionTrace, CompletableFuture<DebugTraceTransactionResult>> {

  @Override
  public CompletableFuture<DebugTraceTransactionResult> apply(
      final TransactionTrace transactionTrace) {
    return CompletableFuture.completedFuture(new DebugTraceTransactionResult(transactionTrace));
  }
}
