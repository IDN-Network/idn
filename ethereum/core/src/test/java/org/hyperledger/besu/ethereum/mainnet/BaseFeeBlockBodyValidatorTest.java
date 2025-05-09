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
package org.idnecology.idn.ethereum.mainnet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BaseFeeBlockBodyValidatorTest {

  private static final KeyPair keyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();

  @Mock ProtocolSpec protocolSpec;
  @Mock ProtocolSchedule protocolSchedule;
  @Mock Block block;
  @Mock BlockHeader blockHeader;

  BaseFeeBlockBodyValidator blockBodyValidator;

  @BeforeEach
  public void setup() {
    when(protocolSpec.getFeeMarket()).thenReturn(FeeMarket.london(0L));

    when(protocolSchedule.getByBlockHeader(any())).thenReturn(protocolSpec);

    when(block.getHeader()).thenReturn(blockHeader);

    blockBodyValidator = new BaseFeeBlockBodyValidator(protocolSchedule);
  }

  @Test
  public void BlockBodyValidatorSucceed() {
    when(blockHeader.getBaseFee()).thenReturn(Optional.of(Wei.of(10L)));
    when(block.getBody())
        .thenReturn(
            new BlockBody(
                List.of(
                    // eip1559 transaction
                    new TransactionTestFixture()
                        .maxFeePerGas(Optional.of(Wei.of(10L)))
                        .maxPriorityFeePerGas(Optional.of(Wei.of(1L)))
                        .type(TransactionType.EIP1559)
                        .createTransaction(keyPair),
                    // frontier transaction
                    new TransactionTestFixture().gasPrice(Wei.of(10L)).createTransaction(keyPair)),
                Collections.emptyList()));

    assertThat(blockBodyValidator.validateTransactionGasPrice(block)).isTrue();
  }

  @Test
  public void BlockBodyValidatorFail_GasPrice() {
    when(blockHeader.getBaseFee()).thenReturn(Optional.of(Wei.of(10L)));
    when(block.getBody())
        .thenReturn(
            new BlockBody(
                List.of(
                    // underpriced frontier transaction
                    new TransactionTestFixture().gasPrice(Wei.of(9L)).createTransaction(keyPair)),
                Collections.emptyList()));

    assertThat(blockBodyValidator.validateTransactionGasPrice(block)).isFalse();
  }

  @Test
  public void BlockBodyValidatorFail_MaxFeePerGas() {
    when(blockHeader.getBaseFee()).thenReturn(Optional.of(Wei.of(10L)));
    when(block.getBody())
        .thenReturn(
            new BlockBody(
                List.of(
                    // underpriced eip1559 transaction
                    new TransactionTestFixture()
                        .maxFeePerGas(Optional.of(Wei.of(1L)))
                        .maxPriorityFeePerGas(Optional.of(Wei.of(10L)))
                        .type(TransactionType.EIP1559)
                        .createTransaction(keyPair)),
                Collections.emptyList()));

    assertThat(blockBodyValidator.validateTransactionGasPrice(block)).isFalse();
  }
}
