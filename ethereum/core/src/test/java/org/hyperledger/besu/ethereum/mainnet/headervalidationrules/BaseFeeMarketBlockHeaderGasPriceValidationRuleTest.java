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
package org.idnecology.idn.ethereum.mainnet.headervalidationrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.mainnet.headervalidationrules.EIP1559Helper.blockHeader;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseFeeMarketBlockHeaderGasPriceValidationRuleTest {

  private static final long FORK_BLOCK = 800L;
  private final BaseFeeMarket baseFeeMarket = FeeMarket.london(FORK_BLOCK);
  private BaseFeeMarketBlockHeaderGasPriceValidationRule validationRule;
  private final BaseFeeMarket feeMarket = FeeMarket.london(FORK_BLOCK);

  @BeforeEach
  public void setUp() {
    validationRule = new BaseFeeMarketBlockHeaderGasPriceValidationRule(baseFeeMarket);
  }

  @Test
  public void shouldTReturnFalseIfParentMissingBaseFeePostFork() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK - 1, 0, Optional.of(Wei.of(10_000L))),
                blockHeader(FORK_BLOCK - 2, 0, Optional.empty())))
        .isFalse();
  }

  @Test
  public void shouldThrowIfMissingBaseFee() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK - 2, 0, Optional.empty()),
                blockHeader(FORK_BLOCK - 1, 0, Optional.of(Wei.of(10_000L)))))
        .isFalse();
  }

  @Test
  public void shouldReturnTrueIfInitialBaseFeeAtForkBlock() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK, 0, Optional.of(feeMarket.getInitialBasefee())), null))
        .isTrue();
  }

  @Test
  public void shouldReturnFalseIfNotInitialBaseFeeAtForkBlock() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK, 0, Optional.of(feeMarket.getInitialBasefee().subtract(1L))),
                null))
        .isFalse();
  }

  @Test
  public void shouldReturnIfNoBaseFeeAfterForkBlock() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK + 2, 0, Optional.empty()),
                blockHeader(FORK_BLOCK + 1, 0, Optional.empty())))
        .isFalse();
  }

  @Test
  public void shouldReturnTrueIfCurrentBaseFeeEqualsExpected() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK + 2, 0, Optional.of(feeMarket.getInitialBasefee())),
                blockHeader(FORK_BLOCK + 1, 0, Optional.of(feeMarket.getInitialBasefee()))))
        .isTrue();
  }

  @Test
  public void shouldReturnFalseIfCurrentBaseFeeDoesNotEqualExpected() {
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK + 2, 0, Optional.of(feeMarket.getInitialBasefee())),
                blockHeader(FORK_BLOCK + 1, 0, Optional.of(feeMarket.getInitialBasefee()), 2)))
        .isFalse();
  }

  @Test
  public void shouldReturnTrueIfUsingZeroBaseFeeMarket() {
    // covers scenario where chain is converted from a non-zero base fee to a zero base fee
    final BaseFeeMarket zeroBaseFeeMarket = FeeMarket.zeroBaseFee(FORK_BLOCK);
    final var validationRule =
        new BaseFeeMarketBlockHeaderGasPriceValidationRule(zeroBaseFeeMarket);
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK + 2, 0, Optional.of(zeroBaseFeeMarket.getInitialBasefee())),
                blockHeader(FORK_BLOCK + 1, 0, Optional.of(feeMarket.getInitialBasefee()), 2)))
        .isTrue();
  }

  @Test
  public void shouldReturnTrueIfUsingZeroBaseFeeMarketOnNonZeroLondonForkBlock() {
    // syncing across a london fork where baseFee wasn't zeroed,
    // but is now using a ZeroBaseFeeMarket
    final BaseFeeMarket zeroBaseFeeMarket = FeeMarket.zeroBaseFee(FORK_BLOCK);
    final var validationRule =
        new BaseFeeMarketBlockHeaderGasPriceValidationRule(zeroBaseFeeMarket);
    final Wei londonFeeMarketBaseFee = feeMarket.getInitialBasefee();
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK, 0, Optional.of(londonFeeMarketBaseFee)),
                blockHeader(FORK_BLOCK - 1, 0, Optional.of(londonFeeMarketBaseFee))))
        .isTrue();
  }

  @Test
  public void shouldReturnTrueIfUsingFixedBaseFeeMarket() {
    final BaseFeeMarket fixedBaseFeeMarket = FeeMarket.fixedBaseFee(FORK_BLOCK, Wei.ONE);
    final var validationRule =
        new BaseFeeMarketBlockHeaderGasPriceValidationRule(fixedBaseFeeMarket);
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK + 2, 0, Optional.of(fixedBaseFeeMarket.getInitialBasefee())),
                blockHeader(FORK_BLOCK + 1, 0, Optional.of(feeMarket.getInitialBasefee()), 2)))
        .isTrue();
  }

  @Test
  public void shouldReturnTrueIfUsingFixedBaseFeeMarketOnNonZeroLondonForkBlock() {
    final BaseFeeMarket zeroBaseFeeMarket = FeeMarket.fixedBaseFee(FORK_BLOCK, Wei.ONE);
    final var validationRule =
        new BaseFeeMarketBlockHeaderGasPriceValidationRule(zeroBaseFeeMarket);
    final Wei londonFeeMarketBaseFee = feeMarket.getInitialBasefee();
    assertThat(
            validationRule.validate(
                blockHeader(FORK_BLOCK, 0, Optional.of(londonFeeMarketBaseFee)),
                blockHeader(FORK_BLOCK - 1, 0, Optional.of(londonFeeMarketBaseFee))))
        .isTrue();
  }
}
