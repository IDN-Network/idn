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
package org.idnecology.idn.ethereum.trie.diffbased.bonsai.worldview;

import org.idnecology.idn.datatypes.AccountValue;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StorageSlotKey;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.BonsaiAccount;
import org.idnecology.idn.ethereum.trie.diffbased.common.DiffBasedValue;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.DiffBasedWorldView;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.accumulator.DiffBasedWorldStateUpdateAccumulator;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.accumulator.preload.Consumer;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.worldstate.UpdateTrackingAccount;

public class BonsaiWorldStateUpdateAccumulator
    extends DiffBasedWorldStateUpdateAccumulator<BonsaiAccount> {
  public BonsaiWorldStateUpdateAccumulator(
      final DiffBasedWorldView world,
      final Consumer<DiffBasedValue<BonsaiAccount>> accountPreloader,
      final Consumer<StorageSlotKey> storagePreloader,
      final EvmConfiguration evmConfiguration) {
    super(world, accountPreloader, storagePreloader, evmConfiguration);
  }

  @Override
  public DiffBasedWorldStateUpdateAccumulator<BonsaiAccount> copy() {
    final BonsaiWorldStateUpdateAccumulator copy =
        new BonsaiWorldStateUpdateAccumulator(
            wrappedWorldView(),
            getAccountPreloader(),
            getStoragePreloader(),
            getEvmConfiguration());
    copy.cloneFromUpdater(this);
    return copy;
  }

  @Override
  protected BonsaiAccount copyAccount(final BonsaiAccount account) {
    return new BonsaiAccount(account);
  }

  @Override
  protected BonsaiAccount copyAccount(
      final BonsaiAccount toCopy, final DiffBasedWorldView context, final boolean mutable) {
    return new BonsaiAccount(toCopy, context, mutable);
  }

  @Override
  protected BonsaiAccount createAccount(
      final DiffBasedWorldView context,
      final Address address,
      final AccountValue stateTrieAccount,
      final boolean mutable) {
    return new BonsaiAccount(context, address, stateTrieAccount, mutable);
  }

  @Override
  protected BonsaiAccount createAccount(
      final DiffBasedWorldView context,
      final Address address,
      final Hash addressHash,
      final long nonce,
      final Wei balance,
      final Hash storageRoot,
      final Hash codeHash,
      final boolean mutable) {
    return new BonsaiAccount(
        context, address, addressHash, nonce, balance, storageRoot, codeHash, mutable);
  }

  @Override
  protected BonsaiAccount createAccount(
      final DiffBasedWorldView context, final UpdateTrackingAccount<BonsaiAccount> tracked) {
    return new BonsaiAccount(context, tracked);
  }

  @Override
  protected void assertCloseEnoughForDiffing(
      final BonsaiAccount source, final AccountValue account, final String context) {
    BonsaiAccount.assertCloseEnoughForDiffing(source, account, context);
  }
}
