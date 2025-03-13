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
package org.idnecology.idn.ethereum.p2p.discovery.internal;

import org.idnecology.idn.ethereum.p2p.discovery.internal.PeerDiscoveryController.AsyncExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BlockingAsyncExecutor implements AsyncExecutor {

  @Override
  public <T> CompletableFuture<T> execute(final Supplier<T> action) {
    return CompletableFuture.completedFuture(action.get());
  }
}
