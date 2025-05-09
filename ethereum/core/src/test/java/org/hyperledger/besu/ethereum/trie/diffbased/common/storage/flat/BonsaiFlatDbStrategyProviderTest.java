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
package org.idnecology.idn.ethereum.trie.diffbased.common.storage.flat;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.flat.BonsaiFlatDbStrategyProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.flat.BonsaiFullFlatDbStrategy;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.flat.BonsaiPartialFlatDbStrategy;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.DiffBasedSubStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.FlatDbMode;
import org.idnecology.idn.ethereum.worldstate.ImmutableDataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.ImmutableDiffBasedSubStorageConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorageTransaction;
import org.idnecology.idn.services.kvstore.SegmentedInMemoryKeyValueStorage;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonsaiFlatDbStrategyProviderTest {
  private final BonsaiFlatDbStrategyProvider flatDbStrategyProvider =
      new BonsaiFlatDbStrategyProvider(
          new NoOpMetricsSystem(), DataStorageConfiguration.DEFAULT_CONFIG);
  private final SegmentedKeyValueStorage composedWorldStateStorage =
      new SegmentedInMemoryKeyValueStorage(
          List.of(
              KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE,
              KeyValueSegmentIdentifier.CODE_STORAGE));

  @ParameterizedTest
  @EnumSource(FlatDbMode.class)
  void loadsFlatDbStrategyForStoredFlatDbMode(final FlatDbMode flatDbMode) {
    updateFlatDbMode(flatDbMode);

    flatDbStrategyProvider.loadFlatDbStrategy(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.getFlatDbMode()).isEqualTo(flatDbMode);
  }

  @Test
  void loadsPartialFlatDbStrategyWhenNoFlatDbModeStored() {
    flatDbStrategyProvider.loadFlatDbStrategy(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.getFlatDbMode()).isEqualTo(FlatDbMode.FULL);
  }

  @Test
  void upgradesFlatDbStrategyToFullFlatDbMode() {
    updateFlatDbMode(FlatDbMode.PARTIAL);

    flatDbStrategyProvider.upgradeToFullFlatDbMode(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.flatDbMode).isEqualTo(FlatDbMode.FULL);
    assertThat(flatDbStrategyProvider.flatDbStrategy).isNotNull();
    assertThat(flatDbStrategyProvider.getFlatDbStrategy(composedWorldStateStorage))
        .isInstanceOf(BonsaiFullFlatDbStrategy.class);
    assertThat(flatDbStrategyProvider.flatDbStrategy.codeStorageStrategy)
        .isInstanceOf(CodeHashCodeStorageStrategy.class);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void emptyDbCreatesFlatDbStrategyUsingCodeByHashConfig(final boolean codeByHashEnabled) {
    final DataStorageConfiguration dataStorageConfiguration =
        ImmutableDataStorageConfiguration.builder()
            .dataStorageFormat(DataStorageFormat.BONSAI)
            .diffBasedSubStorageConfiguration(
                ImmutableDiffBasedSubStorageConfiguration.builder()
                    .maxLayersToLoad(DiffBasedSubStorageConfiguration.DEFAULT_MAX_LAYERS_TO_LOAD)
                    .unstable(
                        ImmutableDiffBasedSubStorageConfiguration.DiffBasedUnstable.builder()
                            .codeStoredByCodeHashEnabled(codeByHashEnabled)
                            .build())
                    .build())
            .build();
    final BonsaiFlatDbStrategyProvider flatDbStrategyProvider =
        new BonsaiFlatDbStrategyProvider(new NoOpMetricsSystem(), dataStorageConfiguration);

    flatDbStrategyProvider.loadFlatDbStrategy(composedWorldStateStorage);
    final Class<? extends CodeStorageStrategy> expectedCodeStorageClass =
        codeByHashEnabled
            ? CodeHashCodeStorageStrategy.class
            : AccountHashCodeStorageStrategy.class;
    assertThat(flatDbStrategyProvider.flatDbMode).isEqualTo(FlatDbMode.FULL);
    assertThat(flatDbStrategyProvider.flatDbStrategy.codeStorageStrategy)
        .isInstanceOf(expectedCodeStorageClass);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void existingAccountHashDbUsesAccountHash(final boolean codeByHashEnabled) {
    final DataStorageConfiguration dataStorageConfiguration =
        ImmutableDataStorageConfiguration.builder()
            .dataStorageFormat(DataStorageFormat.BONSAI)
            .diffBasedSubStorageConfiguration(
                ImmutableDiffBasedSubStorageConfiguration.builder()
                    .maxLayersToLoad(DiffBasedSubStorageConfiguration.DEFAULT_MAX_LAYERS_TO_LOAD)
                    .unstable(
                        ImmutableDiffBasedSubStorageConfiguration.DiffBasedUnstable.builder()
                            .codeStoredByCodeHashEnabled(codeByHashEnabled)
                            .build())
                    .build())
            .build();
    final BonsaiFlatDbStrategyProvider flatDbStrategyProvider =
        new BonsaiFlatDbStrategyProvider(new NoOpMetricsSystem(), dataStorageConfiguration);

    final SegmentedKeyValueStorageTransaction transaction =
        composedWorldStateStorage.startTransaction();
    final AccountHashCodeStorageStrategy accountHashCodeStorageStrategy =
        new AccountHashCodeStorageStrategy();
    // key representing account hash just needs to not be the code hash
    final Hash accountHash = Hash.wrap(Bytes32.fromHexString("0001"));
    accountHashCodeStorageStrategy.putFlatCode(transaction, accountHash, null, Bytes.of(2));
    transaction.commit();

    flatDbStrategyProvider.loadFlatDbStrategy(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.flatDbMode).isEqualTo(FlatDbMode.FULL);
    assertThat(flatDbStrategyProvider.flatDbStrategy.codeStorageStrategy)
        .isInstanceOf(AccountHashCodeStorageStrategy.class);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void existingCodeHashDbUsesCodeHash(final boolean codeByHashEnabled) {
    final DataStorageConfiguration dataStorageConfiguration =
        ImmutableDataStorageConfiguration.builder()
            .dataStorageFormat(DataStorageFormat.BONSAI)
            .diffBasedSubStorageConfiguration(
                ImmutableDiffBasedSubStorageConfiguration.builder()
                    .maxLayersToLoad(DiffBasedSubStorageConfiguration.DEFAULT_MAX_LAYERS_TO_LOAD)
                    .unstable(
                        ImmutableDiffBasedSubStorageConfiguration.DiffBasedUnstable.builder()
                            .codeStoredByCodeHashEnabled(codeByHashEnabled)
                            .build())
                    .build())
            .build();
    final BonsaiFlatDbStrategyProvider flatDbStrategyProvider =
        new BonsaiFlatDbStrategyProvider(new NoOpMetricsSystem(), dataStorageConfiguration);

    final SegmentedKeyValueStorageTransaction transaction =
        composedWorldStateStorage.startTransaction();

    final CodeHashCodeStorageStrategy codeHashCodeStorageStrategy =
        new CodeHashCodeStorageStrategy();
    codeHashCodeStorageStrategy.putFlatCode(transaction, null, Hash.hash(Bytes.of(1)), Bytes.of(1));
    transaction.commit();

    flatDbStrategyProvider.loadFlatDbStrategy(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.flatDbMode).isEqualTo(FlatDbMode.FULL);
    assertThat(flatDbStrategyProvider.flatDbStrategy.codeStorageStrategy)
        .isInstanceOf(CodeHashCodeStorageStrategy.class);
  }

  @Test
  void downgradesFlatDbStrategyToPartiallyFlatDbMode() {
    updateFlatDbMode(FlatDbMode.FULL);

    flatDbStrategyProvider.downgradeToPartialFlatDbMode(composedWorldStateStorage);
    assertThat(flatDbStrategyProvider.flatDbMode).isEqualTo(FlatDbMode.PARTIAL);
    assertThat(flatDbStrategyProvider.flatDbStrategy).isNotNull();
    assertThat(flatDbStrategyProvider.getFlatDbStrategy(composedWorldStateStorage))
        .isInstanceOf(BonsaiPartialFlatDbStrategy.class);
  }

  private void updateFlatDbMode(final FlatDbMode flatDbMode) {
    final SegmentedKeyValueStorageTransaction transaction =
        composedWorldStateStorage.startTransaction();
    transaction.put(
        KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE,
        FlatDbStrategyProvider.FLAT_DB_MODE,
        flatDbMode.getVersion().toArrayUnsafe());
    transaction.commit();
  }
}
