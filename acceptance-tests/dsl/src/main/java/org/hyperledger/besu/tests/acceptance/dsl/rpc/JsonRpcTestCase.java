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
package org.idnecology.idn.tests.acceptance.dsl.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcTestCase {
  private final JsonNode request;
  private final JsonNode response;
  private final int statusCode;
  private final long waitTime;

  @JsonCreator
  public JsonRpcTestCase(
      @JsonProperty("request") final JsonNode request,
      @JsonProperty("response") final JsonNode response,
      @JsonProperty("statusCode") final int statusCode,
      @JsonProperty(value = "waitTime", defaultValue = "0") final long waitTime) {
    this.request = request;
    this.response = response;
    this.statusCode = statusCode;
    this.waitTime = waitTime;
  }

  public JsonNode getRequest() {
    return request;
  }

  public JsonNode getResponse() {
    return response;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public long getWaitTime() {
    return waitTime;
  }
}
