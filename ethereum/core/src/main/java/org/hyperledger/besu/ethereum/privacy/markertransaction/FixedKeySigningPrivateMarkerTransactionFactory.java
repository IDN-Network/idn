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
package org.idnecology.idn.ethereum.privacy.markertransaction;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.plugin.data.PrivateTransaction;
import org.idnecology.idn.plugin.data.UnsignedPrivateMarkerTransaction;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import org.apache.tuweni.bytes.Bytes;

public class FixedKeySigningPrivateMarkerTransactionFactory
    extends SigningPrivateMarkerTransactionFactory implements PrivateMarkerTransactionFactory {

  private final KeyPair signingKey;
  private final Address sender;

  public FixedKeySigningPrivateMarkerTransactionFactory(final KeyPair signingKey) {
    this.signingKey = signingKey;
    this.sender = Util.publicKeyToAddress(signingKey.getPublicKey());
  }

  @Override
  public Address getSender(
      final PrivateTransaction privateTransaction, final String privacyUserId) {
    return this.sender;
  }

  @Override
  public Bytes create(
      final UnsignedPrivateMarkerTransaction unsignedPrivateMarkerTransaction,
      final PrivateTransaction privateTransaction,
      final String privacyUserId) {
    return signAndBuild(unsignedPrivateMarkerTransaction, signingKey);
  }
}
