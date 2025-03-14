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
package org.idnecology.idn.evm.worldstate;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.evm.account.AccountState;
import org.idnecology.idn.evm.account.AccountStorageEntry;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/**
 * A specific state of the world.
 *
 * <p>Note that while this interface represents an immutable view of a world state (it doesn't have
 * mutation methods), it does not guarantee in and of itself that the underlying implementation is
 * not mutable. In other words, objects implementing this interface are not guaranteed to be
 * thread-safe, though some particular implementations may provide such guarantees.
 */
public interface WorldState extends WorldView, AutoCloseable {

  /**
   * The root hash of the world state this represents.
   *
   * @return the world state root hash.
   */
  Hash rootHash();

  /**
   * Get the mid-block root hash for Frontier transactions.
   *
   * @return the world state root hash.
   */
  Hash frontierRootHash();

  /**
   * A stream of all the accounts in this world state.
   *
   * @param startKeyHash The trie key at which to start iterating
   * @param limit The maximum number of results to return
   * @return a stream of all the accounts (in no particular order) contained in the world state
   *     represented by the root hash of this object at the time of the call.
   */
  Stream<StreamableAccount> streamAccounts(Bytes32 startKeyHash, int limit);

  /** The Streamable account. */
  class StreamableAccount implements AccountState {
    private final Optional<Address> address;
    private final AccountState accountState;

    /**
     * Instantiates a new Streamable account.
     *
     * @param address the address
     * @param accountState the account state
     */
    public StreamableAccount(final Optional<Address> address, final AccountState accountState) {
      this.address = address;
      this.accountState = accountState;
    }

    /**
     * Gets address.
     *
     * @return the address
     */
    public Optional<Address> getAddress() {
      return address;
    }

    @Override
    public Hash getAddressHash() {
      return accountState.getAddressHash();
    }

    @Override
    public long getNonce() {
      return accountState.getNonce();
    }

    @Override
    public Wei getBalance() {
      return accountState.getBalance();
    }

    @Override
    public Bytes getCode() {
      return accountState.getCode();
    }

    @Override
    public Hash getCodeHash() {
      return accountState.getCodeHash();
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {
      return accountState.getStorageValue(key);
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      return accountState.getOriginalStorageValue(key);
    }

    @Override
    public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
        final Bytes32 startKeyHash, final int limit) {
      return accountState.storageEntriesFrom(startKeyHash, limit);
    }
  }

  @Override
  default void close() throws Exception {
    // default no-op
  }
}
