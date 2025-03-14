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
package org.idnecology.idn.crypto;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

/** The Idn Security provider. */
public final class IdnProvider extends Provider {

  private static final String info = "Idn Security Provider v1.0";

  /** The constant PROVIDER_NAME. */
  public static final String PROVIDER_NAME = "Idn";

  /** Instantiates a new Idn provider. */
  @SuppressWarnings({"unchecked", "removal"})
  public IdnProvider() {
    super(PROVIDER_NAME, "1.0", info);
    AccessController.doPrivileged(
        (PrivilegedAction)
            () -> {
              put("MessageDigest.Blake2bf", Blake2bfMessageDigest.class.getName());
              return null;
            });
  }
}
