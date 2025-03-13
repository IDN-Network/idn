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
package org.idnecology.idn.plugin.data;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/** A transaction receipt, containing information pertaining to a transaction execution. */
public interface TransactionReceipt {
  /**
   * Returns the total amount of gas consumed in the block after the transaction has been processed.
   *
   * @return the total amount of gas consumed in the block after the transaction has been processed
   */
  long getCumulativeGasUsed();

  /**
   * Returns the logs generated by the transaction.
   *
   * @return the logs generated by the transaction
   */
  List<? extends Log> getLogs();

  /**
   * Returns the Bytes for the logs generated by the transaction
   *
   * @return the Bytes for the logs generated by the transaction
   */
  Bytes getBloomFilter();

  /**
   * Returns the status code for the status-encoded transaction receipt
   *
   * @return the status code if the transaction receipt is status-encoded; otherwise {@code -1}
   */
  int getStatus();

  /**
   * Returns the ABI-encoded revert reason for the failed transaction (if applicable)
   *
   * @return the ABI-encoded revert reason for the failed transaction (if applicable)
   */
  Optional<Bytes> getRevertReason();
}
