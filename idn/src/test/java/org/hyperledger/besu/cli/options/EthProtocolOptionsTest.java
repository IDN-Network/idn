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
package org.idnecology.idn.cli.options;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EthProtocolOptionsTest
    extends AbstractCLIOptionsTest<EthProtocolConfiguration, EthProtocolOptions> {

  @Test
  public void parsesValidMaxMessageSizeOptions() {

    final TestIdnCommand cmd = parseCommand("--Xeth-max-message-size", "4");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxMessageSize()).isEqualTo(4);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesInvalidMaxMessageSizeOptionsShouldFail() {
    parseCommand("--Xeth-max-message-size", "-4");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8))
        .contains(
            "Invalid value for option '--Xeth-max-message-size': cannot convert '-4' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetHeadersOptions() {

    final TestIdnCommand cmd = parseCommand("--Xewp-max-get-headers", "13");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetBlockHeaders()).isEqualTo(13);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetHeadersOptionsShouldFail() {
    parseCommand("--Xewp-max-get-headers", "-13");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8))
        .contains(
            "Invalid value for option '--Xewp-max-get-headers': cannot convert '-13' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetBodiesOptions() {
    final TestIdnCommand cmd = parseCommand("--Xewp-max-get-bodies", "14");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetBlockBodies()).isEqualTo(14);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetBodiesOptionsShouldFail() {
    parseCommand("--Xewp-max-get-bodies", "-14");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8))
        .contains(
            "Invalid value for option '--Xewp-max-get-bodies': cannot convert '-14' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetReceiptsOptions() {
    final TestIdnCommand cmd = parseCommand("--Xewp-max-get-receipts", "15");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetReceipts()).isEqualTo(15);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetReceiptsOptionsShouldFail() {
    parseCommand("--Xewp-max-get-receipts", "-15");

    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8))
        .contains(
            "Invalid value for option '--Xewp-max-get-receipts': cannot convert '-15' to PositiveNumber");
  }

  @Test
  public void parsesValidEwpMaxGetNodeDataOptions() {
    final TestIdnCommand cmd = parseCommand("--Xewp-max-get-node-data", "16");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxGetNodeData()).isEqualTo(16);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesInvalidEwpMaxGetNodeDataOptionsShouldFail() {
    parseCommand("--Xewp-max-get-node-data", "-16");
    verifyNoInteractions(mockRunnerBuilder);
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8))
        .contains(
            "Invalid value for option '--Xewp-max-get-node-data': cannot convert '-16' to PositiveNumber");
  }

  @Test
  public void parsesValidEthMaxProtocol() {
    final TestIdnCommand cmd = parseCommand("--Xeth-capability-max", "66");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMaxEthCapability()).isEqualTo(66);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void parsesValidEthMinProtocol() {
    final TestIdnCommand cmd = parseCommand("--Xeth-capability-min", "66");

    final EthProtocolOptions options = getOptionsFromIdnCommand(cmd);
    final EthProtocolConfiguration config = options.toDomainObject();
    assertThat(config.getMinEthCapability()).isEqualTo(66);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  @Override
  protected EthProtocolConfiguration createDefaultDomainObject() {
    return EthProtocolConfiguration.builder().build();
  }

  @Override
  protected EthProtocolConfiguration createCustomizedDomainObject() {
    return EthProtocolConfiguration.builder()
        .maxMessageSize(EthProtocolConfiguration.DEFAULT_MAX_MESSAGE_SIZE * 2)
        .maxGetBlockHeaders(EthProtocolConfiguration.DEFAULT_MAX_GET_BLOCK_HEADERS + 2)
        .maxGetBlockBodies(EthProtocolConfiguration.DEFAULT_MAX_GET_BLOCK_BODIES + 2)
        .maxGetReceipts(EthProtocolConfiguration.DEFAULT_MAX_GET_RECEIPTS + 2)
        .maxGetNodeData(EthProtocolConfiguration.DEFAULT_MAX_GET_NODE_DATA + 2)
        .maxGetPooledTransactions(EthProtocolConfiguration.DEFAULT_MAX_GET_POOLED_TRANSACTIONS + 2)
        .maxEthCapability(EthProtocolConfiguration.DEFAULT_MAX_CAPABILITY)
        .minEthCapability(EthProtocolConfiguration.DEFAULT_MIN_CAPABILITY)
        .build();
  }

  @Override
  protected EthProtocolOptions optionsFromDomainObject(
      final EthProtocolConfiguration domainObject) {
    return EthProtocolOptions.fromConfig(domainObject);
  }

  @Override
  protected EthProtocolOptions getOptionsFromIdnCommand(final TestIdnCommand idnCommand) {
    return idnCommand.getEthProtocolOptions();
  }
}
