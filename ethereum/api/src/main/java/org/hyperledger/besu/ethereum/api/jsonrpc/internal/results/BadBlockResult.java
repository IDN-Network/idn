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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.results;

import org.idnecology.idn.ethereum.core.Block;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true)
@JsonSerialize(as = ImmutableBadBlockResult.class)
@JsonDeserialize(as = ImmutableBadBlockResult.class)
@JsonPropertyOrder({"block", "hash", "rlp"})
public interface BadBlockResult {

  @JsonProperty("block")
  BlockResult getBlockResult();

  @JsonProperty("hash")
  String getHash();

  @JsonProperty("rlp")
  String getRlp();

  static BadBlockResult from(final BlockResult blockResult, final Block block) {
    return ImmutableBadBlockResult.of(
        blockResult, block.getHash().toHexString(), block.toRlp().toHexString());
  }
}
