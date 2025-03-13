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
package org.idnecology.idn.ethereum.mainnet.requests;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.RequestType;
import org.idnecology.idn.ethereum.core.Request;
import org.idnecology.idn.ethereum.mainnet.systemcall.BlockContextProcessor;
import org.idnecology.idn.ethereum.mainnet.systemcall.SystemCallProcessor;

import org.apache.tuweni.bytes.Bytes;

/** Processes system call requests. */
public class SystemCallRequestProcessor
    implements RequestProcessor, BlockContextProcessor<Request, RequestProcessingContext> {

  private final Address callAddress;
  private final RequestType requestType;

  public SystemCallRequestProcessor(final Address callAddress, final RequestType requestType) {
    this.callAddress = callAddress;
    this.requestType = requestType;
  }

  /**
   * Processes a system call and converts the result as a Request.
   *
   * @param context The request context being processed.
   * @return A {@link Request} request
   */
  @Override
  public Request process(final RequestProcessingContext context) {

    final SystemCallProcessor systemCallProcessor =
        new SystemCallProcessor(context.getProtocolSpec().getTransactionProcessor());

    Bytes systemCallOutput = systemCallProcessor.process(callAddress, context, Bytes.EMPTY);

    return new Request(requestType, systemCallOutput);
  }
}
