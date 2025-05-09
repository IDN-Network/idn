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
package org.idnecology.idn.config;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

/** The Ibft legacy config options. */
public class IbftLegacyConfigOptions {

  /** The constant DEFAULT. */
  public static final IbftLegacyConfigOptions DEFAULT =
      new IbftLegacyConfigOptions(JsonUtil.createEmptyObjectNode());

  private static final long DEFAULT_EPOCH_LENGTH = 30_000;
  private static final int DEFAULT_BLOCK_PERIOD_SECONDS = 1;

  private final ObjectNode ibftConfigRoot;

  /**
   * Instantiates a new Ibft legacy config options.
   *
   * @param ibftConfigRoot the ibft config root
   */
  IbftLegacyConfigOptions(final ObjectNode ibftConfigRoot) {
    this.ibftConfigRoot = ibftConfigRoot;
  }

  /*
   */
  /**
   * Gets epoch length.
   *
   * @return the epoch length
   */
  public long getEpochLength() {
    return JsonUtil.getLong(ibftConfigRoot, "epochlength", DEFAULT_EPOCH_LENGTH);
  }

  /**
   * Gets block period seconds.
   *
   * @return the block period seconds
   */
  public int getBlockPeriodSeconds() {
    return JsonUtil.getPositiveInt(
        ibftConfigRoot, "blockperiodseconds", DEFAULT_BLOCK_PERIOD_SECONDS);
  }

  /**
   * As map.
   *
   * @return the map
   */
  Map<String, Object> asMap() {
    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    return builder.build();
  }
}
