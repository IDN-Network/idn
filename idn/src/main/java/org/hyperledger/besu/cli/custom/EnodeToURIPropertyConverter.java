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
package org.idnecology.idn.cli.custom;

import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;

import java.net.URI;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import picocli.CommandLine.ITypeConverter;

/** The Enode to uri property converter. */
public class EnodeToURIPropertyConverter implements ITypeConverter<URI> {

  private final Function<String, URI> converter;

  /** Instantiates a new Enode to uri property converter. */
  EnodeToURIPropertyConverter() {
    this.converter = (s) -> EnodeURLImpl.fromString(s).toURI();
  }

  /**
   * Instantiates a new Enode to uri property converter.
   *
   * @param converter the converter
   */
  @VisibleForTesting
  EnodeToURIPropertyConverter(final Function<String, URI> converter) {
    this.converter = converter;
  }

  @Override
  public URI convert(final String value) {
    return converter.apply(value);
  }
}
