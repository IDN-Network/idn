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
package org.idnecology.idn.ethereum.eth.messages.snap;

import org.idnecology.idn.ethereum.p2p.rlpx.wire.AbstractSnapMessageData;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPInput;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import kotlin.collections.ArrayDeque;
import org.apache.tuweni.bytes.Bytes;
import org.immutables.value.Value;

public final class ByteCodesMessage extends AbstractSnapMessageData {

  public ByteCodesMessage(final Bytes data) {
    super(data);
  }

  public static ByteCodesMessage readFrom(final MessageData message) {
    if (message instanceof ByteCodesMessage) {
      return (ByteCodesMessage) message;
    }
    final int code = message.getCode();
    if (code != SnapV1.BYTECODES) {
      throw new IllegalArgumentException(
          String.format("Message has code %d and thus is not a ByteCodesMessage.", code));
    }
    return new ByteCodesMessage(message.getData());
  }

  public static ByteCodesMessage create(final List<Bytes> codes) {
    return create(Optional.empty(), codes);
  }

  public static ByteCodesMessage create(
      final Optional<BigInteger> requestId, final List<Bytes> codes) {
    final BytesValueRLPOutput tmp = new BytesValueRLPOutput();
    tmp.startList();
    requestId.ifPresent(tmp::writeBigIntegerScalar);
    tmp.writeList(codes, (code, rlpOutput) -> rlpOutput.writeBytes(code));
    tmp.endList();
    return new ByteCodesMessage(tmp.encoded());
  }

  @Override
  protected Bytes wrap(final BigInteger requestId) {
    final ByteCodes bytecodes = bytecodes(false);
    return create(Optional.of(requestId), bytecodes.codes()).getData();
  }

  @Override
  public int getCode() {
    return SnapV1.BYTECODES;
  }

  public ByteCodes bytecodes(final boolean withRequestId) {
    final ArrayDeque<Bytes> codes = new ArrayDeque<>();
    final RLPInput input = new BytesValueRLPInput(data, false);
    input.enterList();
    if (withRequestId) input.skipNext();
    input.enterList();
    while (!input.isEndOfCurrentList()) {
      codes.add(input.readBytes());
    }
    input.leaveList();
    input.leaveList();
    return ImmutableByteCodes.builder().codes(codes).build();
  }

  @Value.Immutable
  public interface ByteCodes {

    ArrayDeque<Bytes> codes();
  }
}
