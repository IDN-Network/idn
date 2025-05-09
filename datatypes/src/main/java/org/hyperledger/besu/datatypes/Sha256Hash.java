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
package org.idnecology.idn.datatypes;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** A Sha256Hash is a Hash that has been generated using the SHA-256 algorithm. */
public class Sha256Hash extends Hash {

  /**
   * Construct a Sha256Hash from a Bytes32 value.
   *
   * @param bytes raw bytes of the hash
   */
  private Sha256Hash(final Bytes32 bytes) {
    super(bytes);
  }

  /**
   * Construct a Sha256Hash from a Bytes value.
   *
   * @param value The value to hash.
   * @return The Sha256Hash of the value.
   */
  public static Hash sha256(final Bytes value) {
    return new Sha256Hash(org.idnecology.idn.crypto.Hash.sha256(value));
  }
}
