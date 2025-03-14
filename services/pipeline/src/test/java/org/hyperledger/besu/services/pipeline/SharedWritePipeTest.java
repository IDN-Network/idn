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
package org.idnecology.idn.services.pipeline;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

public class SharedWritePipeTest {

  private static final int CLOSES_REQUIRED = 3;

  @SuppressWarnings("unchecked")
  private final WritePipe<String> delegate = mock(WritePipe.class);

  private final SharedWritePipe<String> pipe = new SharedWritePipe<>(delegate, CLOSES_REQUIRED);

  @Test
  public void shouldOnlyCloseDelegatePipeWhenCloseCalledSpecifiedNumberOfTimes() {
    for (int i = 0; i < CLOSES_REQUIRED - 1; i++) {
      pipe.close();
      verifyNoInteractions(delegate);
    }

    pipe.close();
    verify(delegate).close();
  }
}
