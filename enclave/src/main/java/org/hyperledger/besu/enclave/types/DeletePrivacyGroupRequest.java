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
package org.idnecology.idn.enclave.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The Delete privacy group request. */
public class DeletePrivacyGroupRequest {

  private final String privacyGroupId;
  private final String from;

  /**
   * Instantiates a new Delete privacy group request.
   *
   * @param privacyGroupId the privacy group id
   * @param from the from
   */
  @JsonCreator
  public DeletePrivacyGroupRequest(
      @JsonProperty("privacyGroupId") final String privacyGroupId,
      @JsonProperty("from") final String from) {
    this.privacyGroupId = privacyGroupId;
    this.from = from;
  }

  /**
   * Privacy group id.
   *
   * @return the string
   */
  @JsonProperty("privacyGroupId")
  public String privacyGroupId() {
    return privacyGroupId;
  }

  /**
   * From string.
   *
   * @return the string
   */
  @JsonProperty("from")
  public String from() {
    return from;
  }
}
