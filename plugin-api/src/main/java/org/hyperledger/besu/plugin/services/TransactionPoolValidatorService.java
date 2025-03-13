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
package org.idnecology.idn.plugin.services;

import org.idnecology.idn.plugin.Unstable;
import org.idnecology.idn.plugin.services.txvalidator.PluginTransactionPoolValidator;
import org.idnecology.idn.plugin.services.txvalidator.PluginTransactionPoolValidatorFactory;

/** Transaction validator for addition of transactions to the transaction pool */
@Unstable
public interface TransactionPoolValidatorService extends IdnService {

  /**
   * Returns the transaction validator to be used in the txpool
   *
   * @return the transaction validator
   */
  PluginTransactionPoolValidator createTransactionValidator();

  /**
   * Registers the transaction validator factory with the service
   *
   * @param pluginTransactionPoolValidatorFactory transaction validator factory to be used
   */
  void registerPluginTransactionValidatorFactory(
      PluginTransactionPoolValidatorFactory pluginTransactionPoolValidatorFactory);
}
