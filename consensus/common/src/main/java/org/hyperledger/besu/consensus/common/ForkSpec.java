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
package org.idnecology.idn.consensus.common;

import java.util.Comparator;
import java.util.Objects;

/**
 * The Fork spec.
 *
 * @param <C> the type parameter
 */
public class ForkSpec<C> {

  /** The constant COMPARATOR. */
  public static final Comparator<ForkSpec<?>> COMPARATOR = Comparator.comparing(ForkSpec::getBlock);

  private final long block;
  private final C value;

  /**
   * Instantiates a new Fork spec.
   *
   * @param block the block
   * @param value the value
   */
  public ForkSpec(final long block, final C value) {
    this.block = block;
    this.value = value;
  }

  /**
   * Gets block.
   *
   * @return the block
   */
  public long getBlock() {
    return block;
  }

  /**
   * Gets value.
   *
   * @return the value
   */
  public C getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ForkSpec<?> that = (ForkSpec<?>) o;
    return block == that.block && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(block, value);
  }
}
