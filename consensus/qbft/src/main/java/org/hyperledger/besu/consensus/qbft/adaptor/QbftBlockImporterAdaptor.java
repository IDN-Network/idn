/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.consensus.qbft.adaptor;

import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockImporter;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.mainnet.BlockImportResult;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;

/** Adaptor class to allow a {@link BlockImporter} to be used as a {@link QbftBlockImporter}. */
public class QbftBlockImporterAdaptor implements QbftBlockImporter {

  private final BlockImporter blockImporter;
  private final ProtocolContext context;

  /**
   * Constructs a new Qbft block importer.
   *
   * @param blockImporter The Idn block importer
   * @param context The protocol context
   */
  public QbftBlockImporterAdaptor(
      final BlockImporter blockImporter, final ProtocolContext context) {
    this.blockImporter = blockImporter;
    this.context = context;
  }

  @Override
  public boolean importBlock(final QbftBlock block) {
    final BlockImportResult blockImportResult =
        blockImporter.importBlock(context, BlockUtil.toIdnBlock(block), HeaderValidationMode.FULL);
    return blockImportResult.isImported();
  }
}
