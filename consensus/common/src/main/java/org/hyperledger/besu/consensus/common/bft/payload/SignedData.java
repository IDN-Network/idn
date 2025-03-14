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
package org.idnecology.idn.consensus.common.bft.payload;

import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

import org.apache.tuweni.bytes.Bytes;

/**
 * The type Signed data.
 *
 * @param <M> the type parameter of Payload
 */
public class SignedData<M extends Payload> implements Authored {

  private final Address sender;
  private final SECPSignature signature;
  private final M unsignedPayload;

  /**
   * Create signed data.
   *
   * @param <T> the type parameter
   * @param payload the payload
   * @param signature the signature
   * @return the signed data
   */
  public static <T extends Payload> SignedData<T> create(
      final T payload, final SECPSignature signature) {
    final Hash msgHash = payload.hashForSignature();
    return new SignedData<>(payload, Util.signatureToAddress(signature, msgHash), signature);
  }

  private SignedData(final M unsignedPayload, final Address sender, final SECPSignature signature) {
    this.unsignedPayload = unsignedPayload;
    this.sender = sender;
    this.signature = signature;
  }

  @Override
  public Address getAuthor() {
    return sender;
  }

  /**
   * Gets payload.
   *
   * @return the payload
   */
  public M getPayload() {
    return unsignedPayload;
  }

  /**
   * Write to.
   *
   * @param output the output
   */
  public void writeTo(final RLPOutput output) {
    output.startList();
    unsignedPayload.writeTo(output);
    output.writeBytes(signature.encodedBytes());
    output.endList();
  }

  /**
   * Encode.
   *
   * @return the bytes
   */
  public Bytes encode() {
    final BytesValueRLPOutput rlpEncode = new BytesValueRLPOutput();
    writeTo(rlpEncode);
    return rlpEncode.encoded();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SignedData<?> that = (SignedData<?>) o;
    return Objects.equals(sender, that.sender)
        && Objects.equals(signature, that.signature)
        && Objects.equals(unsignedPayload, that.unsignedPayload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sender, signature, unsignedPayload);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SignedData.class.getSimpleName() + "[", "]")
        .add("sender=" + sender)
        .add("signature=" + signature)
        .add("unsignedPayload=" + unsignedPayload)
        .toString();
  }
}
