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
package org.idnecology.idn.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.UnmatchedArgumentException;

@ExtendWith(MockitoExtension.class)
public class PicoCLIOptionsImplTest {

  @Command
  static final class SimpleCommand {

    @Option(names = "--existing")
    String existingOption = "defaultexisting";
  }

  static final class MixinOptions {
    @Option(names = "--plugin-Test1-mixin")
    String mixinOption = "defaultmixin";
  }

  private SimpleCommand command;
  private MixinOptions mixin;
  private CommandLine commandLine;
  private PicoCLIOptionsImpl serviceImpl;

  @BeforeEach
  public void setUp() {
    command = new SimpleCommand();
    mixin = new MixinOptions();
    commandLine = new CommandLine(command);
    serviceImpl = new PicoCLIOptionsImpl(commandLine);

    serviceImpl.addPicoCLIOptions("Test1", mixin);
  }

  @Test
  public void testSimpleOptionParse() {
    commandLine.parseArgs("--existing", "1", "--plugin-Test1-mixin", "2");
    assertThat(command.existingOption).isEqualTo("1");
    assertThat(mixin.mixinOption).isEqualTo("2");
  }

  @Test
  public void testUnsetOptionLeavesDefault() {
    commandLine.parseArgs("--existing", "1");
    assertThat(command.existingOption).isEqualTo("1");
    assertThat(mixin.mixinOption).isEqualTo("defaultmixin");
  }

  @Test
  public void testMixinOptionOnly() {
    commandLine.parseArgs("--plugin-Test1-mixin", "2");
    assertThat(command.existingOption).isEqualTo("defaultexisting");
    assertThat(mixin.mixinOption).isEqualTo("2");
  }

  @Test
  public void testNotExistantOptionsFail() {
    assertThatExceptionOfType(UnmatchedArgumentException.class)
        .isThrownBy(() -> commandLine.parseArgs("--does-not-exist", "1"));
  }
}
