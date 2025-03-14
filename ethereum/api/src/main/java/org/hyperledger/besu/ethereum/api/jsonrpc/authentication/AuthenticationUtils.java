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
package org.idnecology.idn.ethereum.api.jsonrpc.authentication;

import java.util.Optional;

public class AuthenticationUtils {

  public static String getJwtTokenFromAuthorizationHeaderValue(final String value) {
    if (value != null) {
      final String bearerSchemaName = "Bearer ";
      if (value.startsWith(bearerSchemaName)) {
        return value.substring(bearerSchemaName.length());
      }
    }
    return null;
  }

  public static String truncToken(final String jwtToken) {
    return Optional.ofNullable(jwtToken)
        .map(
            token ->
                token
                    .substring(0, 8)
                    .concat("...")
                    .concat(token.substring(token.length() - 8, token.length())))
        .orElse("Invalid JWT");
  }
}
