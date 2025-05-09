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
package org.idnecology.idn.evm.internal;

import org.apache.tuweni.bytes.Bytes;

/** The Memory entry. */
public class MemoryEntry {
  private final long offset;
  private final Bytes value;

  /**
   * Instantiates a new Memory entry.
   *
   * @param offset the offset
   * @param value the value
   */
  public MemoryEntry(final long offset, final Bytes value) {
    this.offset = offset;
    this.value = value;
  }

  /**
   * Gets offset.
   *
   * @return the offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Gets value.
   *
   * @return the value
   */
  public Bytes getValue() {
    return value;
  }
}
