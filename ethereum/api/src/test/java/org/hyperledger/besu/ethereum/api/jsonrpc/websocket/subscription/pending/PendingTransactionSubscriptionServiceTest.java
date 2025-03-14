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
package org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.pending;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.SimpleTestTransactionBuilder;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.Subscription;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionType;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PendingTransactionSubscriptionServiceTest {

  private static final Hash TX_ONE =
      Hash.fromHexString("0x15876958423545c3c7b0fcf9be8ffb543305ee1b43db87ed380dcf0cd16589f7");

  @Mock private SubscriptionManager subscriptionManager;
  @Mock private Blockchain blockchain;
  @Mock private Block block;

  private PendingTransactionSubscriptionService service;

  @BeforeEach
  public void setUp() {
    service = new PendingTransactionSubscriptionService(subscriptionManager);
  }

  @Test
  public void onTransactionAddedMustSendMessage() {
    final long[] subscriptionIds = new long[] {5, 56, 989};
    setUpSubscriptions(Boolean.FALSE, subscriptionIds);
    final Transaction pending = SimpleTestTransactionBuilder.transaction(TX_ONE);

    service.onTransactionAdded(pending);

    verifyNoInteractions(block);
    verifyNoInteractions(blockchain);
    verifySubscriptionMangerInteractions(messages(TX_ONE, subscriptionIds));
  }

  @Test
  public void onTransactionAddedMustSendMessageWithDetails() {
    final long[] subscriptionIds = new long[] {5, 56, 989};
    setUpSubscriptions(Boolean.TRUE, subscriptionIds);
    final Transaction pending = SimpleTestTransactionBuilder.transaction(TX_ONE);

    service.onTransactionAdded(pending);

    verifyNoInteractions(block);
    verifyNoInteractions(blockchain);
    verifySubscriptionMangerDetailInteractions(messages(pending, subscriptionIds));
  }

  private void verifySubscriptionMangerInteractions(final Map<Long, Hash> expected) {
    verify(subscriptionManager)
        .subscriptionsOfType(SubscriptionType.NEW_PENDING_TRANSACTIONS, Subscription.class);

    for (final Map.Entry<Long, Hash> message : expected.entrySet()) {
      verify(subscriptionManager)
          .sendMessage(
              eq(message.getKey()), refEq(new PendingTransactionResult(message.getValue())));
    }

    verifyNoMoreInteractions(subscriptionManager);
  }

  private void verifySubscriptionMangerDetailInteractions(final Map<Long, Transaction> expected) {
    verify(subscriptionManager)
        .subscriptionsOfType(SubscriptionType.NEW_PENDING_TRANSACTIONS, Subscription.class);

    for (final Map.Entry<Long, Transaction> message : expected.entrySet()) {
      PendingTransactionDetailResult value = new PendingTransactionDetailResult(message.getValue());
      verify(subscriptionManager).sendMessage(eq(message.getKey()), refEq(value));
    }

    verifyNoMoreInteractions(subscriptionManager);
  }

  private Map<Long, Hash> messages(final Hash result, final long... subscriptionIds) {
    final Map<Long, Hash> messages = new HashMap<>();

    for (final long subscriptionId : subscriptionIds) {
      messages.put(subscriptionId, result);
    }

    return messages;
  }

  private Map<Long, Transaction> messages(final Transaction result, final long... subscriptionIds) {
    final Map<Long, Transaction> messages = new HashMap<>();

    for (final long subscriptionId : subscriptionIds) {
      messages.put(subscriptionId, result);
    }

    return messages;
  }

  private void setUpSubscriptions(
      final Boolean includeTransactions, final long... subscriptionsIds) {
    when(subscriptionManager.subscriptionsOfType(any(), any()))
        .thenReturn(
            Arrays.stream(subscriptionsIds)
                .mapToObj(
                    id ->
                        new Subscription(
                            id,
                            "conn",
                            SubscriptionType.NEW_PENDING_TRANSACTIONS,
                            includeTransactions))
                .collect(Collectors.toList()));
  }
}
