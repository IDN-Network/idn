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
package org.idnecology.idn.services.kvstore;

import static java.util.stream.Collectors.toUnmodifiableSet;

import org.idnecology.idn.plugin.services.exception.StorageException;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;
import org.idnecology.idn.plugin.services.storage.KeyValueStorageTransaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;

/**
 * This KeyValueStorage will keep data in memory up to some maximum number of elements. Elements are
 * evicted as the maximum limit is approached, evicting least-recently-used elements first.
 */
public class LimitedInMemoryKeyValueStorage implements KeyValueStorage {

  private final Cache<Bytes, byte[]> storage;
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  /**
   * Instantiates a new Limited in memory key value storage.
   *
   * @param maxSize the max size
   */
  public LimitedInMemoryKeyValueStorage(final long maxSize) {
    storage = CacheBuilder.newBuilder().maximumSize(maxSize).build();
  }

  @Override
  public void clear() {
    final Lock lock = rwLock.writeLock();
    lock.lock();
    try {
      storage.invalidateAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean containsKey(final byte[] key) throws StorageException {
    return get(key).isPresent();
  }

  @Override
  public void close() {}

  @Override
  public Optional<byte[]> get(final byte[] key) {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return Optional.ofNullable(storage.getIfPresent(Bytes.wrap(key)));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Set<byte[]> getAllKeysThat(final Predicate<byte[]> returnCondition) {
    return stream()
        .filter(pair -> returnCondition.test(pair.getKey()))
        .map(Pair::getKey)
        .collect(toUnmodifiableSet());
  }

  @Override
  public Set<byte[]> getAllValuesFromKeysThat(final Predicate<byte[]> returnCondition) {
    return stream()
        .filter(pair -> returnCondition.test(pair.getKey()))
        .map(Pair::getValue)
        .collect(toUnmodifiableSet());
  }

  @Override
  public Stream<Pair<byte[], byte[]>> stream() {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return ImmutableSet.copyOf(storage.asMap().entrySet()).stream()
          .map(bytesEntry -> Pair.of(bytesEntry.getKey().toArrayUnsafe(), bytesEntry.getValue()));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Stream<Pair<byte[], byte[]>> streamFromKey(final byte[] startKey) {
    return stream().filter(e -> Bytes.wrap(startKey).compareTo(Bytes.wrap(e.getKey())) <= 0);
  }

  @Override
  public Stream<Pair<byte[], byte[]>> streamFromKey(final byte[] startKey, final byte[] endKey) {
    return stream()
        .filter(e -> Bytes.wrap(startKey).compareTo(Bytes.wrap(e.getKey())) <= 0)
        .takeWhile(e -> Bytes.wrap(endKey).compareTo(Bytes.wrap(e.getKey())) >= 0);
  }

  @Override
  public Stream<byte[]> streamKeys() {
    final Lock lock = rwLock.readLock();
    lock.lock();
    try {
      return ImmutableSet.copyOf(storage.asMap().entrySet()).stream()
          .map(bytesEntry -> bytesEntry.getKey().toArrayUnsafe());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean tryDelete(final byte[] key) {
    final Lock lock = rwLock.writeLock();
    if (lock.tryLock()) {
      try {
        storage.invalidate(Bytes.wrap(key));
      } finally {
        lock.unlock();
      }
      return true;
    }
    return false;
  }

  @Override
  public KeyValueStorageTransaction startTransaction() throws StorageException {
    return new KeyValueStorageTransactionValidatorDecorator(
        new MemoryTransaction(), this::isClosed);
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  private class MemoryTransaction implements KeyValueStorageTransaction {

    private Map<Bytes, byte[]> updatedValues = new HashMap<>();
    private Set<Bytes> removedKeys = new HashSet<>();

    @Override
    public void put(final byte[] key, final byte[] value) {
      updatedValues.put(Bytes.wrap(key), value);
      removedKeys.remove(Bytes.wrap(key));
    }

    @Override
    public void remove(final byte[] key) {
      removedKeys.add(Bytes.wrap(key));
      updatedValues.remove(Bytes.wrap(key));
    }

    @Override
    public void commit() throws StorageException {
      final Lock lock = rwLock.writeLock();
      lock.lock();
      try {
        storage.putAll(updatedValues);
        storage.invalidateAll(removedKeys);
        updatedValues = null;
        removedKeys = null;
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void rollback() {
      updatedValues.clear();
      removedKeys.clear();
    }
  }
}
