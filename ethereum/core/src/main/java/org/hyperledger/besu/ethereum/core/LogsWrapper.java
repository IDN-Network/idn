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
package org.idnecology.idn.ethereum.core;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.log.Log;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class LogsWrapper implements org.idnecology.idn.plugin.data.Log {

  final Log delegate;

  public LogsWrapper(final Log delegate) {
    this.delegate = delegate;
  }

  @Override
  public Address getLogger() {
    return delegate.getLogger();
  }

  @Override
  public List<? extends Bytes32> getTopics() {
    return delegate.getTopics();
  }

  @Override
  public Bytes getData() {
    return delegate.getData();
  }
}
