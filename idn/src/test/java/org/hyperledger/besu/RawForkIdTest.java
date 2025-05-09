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
package org.idnecology.idn;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.forkid.ForkId;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class RawForkIdTest {
  @Test
  public void testFromRaw() {
    final ForkId forkId = new ForkId(Bytes.ofUnsignedInt(0xfe3366e7L), 1735371L);
    final List<List<Bytes>> forkIdAsBytesList = List.of(forkId.getForkIdAsBytesList());
    assertThat(ForkId.fromRawForkId(forkIdAsBytesList).get()).isEqualTo(forkId);
  }
}
