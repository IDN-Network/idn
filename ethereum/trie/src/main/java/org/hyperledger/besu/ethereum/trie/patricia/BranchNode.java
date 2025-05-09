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
package org.idnecology.idn.ethereum.trie.patricia;

import static org.idnecology.idn.crypto.Hash.keccak256;

import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.LocationNodeVisitor;
import org.idnecology.idn.ethereum.trie.Node;
import org.idnecology.idn.ethereum.trie.NodeFactory;
import org.idnecology.idn.ethereum.trie.NodeVisitor;
import org.idnecology.idn.ethereum.trie.NullNode;
import org.idnecology.idn.ethereum.trie.PathNodeVisitor;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;

public class BranchNode<V> implements Node<V> {

  @SuppressWarnings("rawtypes")
  protected static final Node NULL_NODE = NullNode.instance();

  private final Optional<Bytes> location;
  private final List<Node<V>> children;
  private final Optional<V> value;
  protected final NodeFactory<V> nodeFactory;
  private final Function<V, Bytes> valueSerializer;
  protected WeakReference<Bytes> encodedBytes;
  private SoftReference<Bytes32> hash;
  private boolean dirty = false;
  private boolean needHeal = false;

  public BranchNode(
      final Bytes location,
      final List<Node<V>> children,
      final Optional<V> value,
      final NodeFactory<V> nodeFactory,
      final Function<V, Bytes> valueSerializer) {
    assert (children.size() == maxChild());
    this.location = Optional.ofNullable(location);
    this.children = children;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  public BranchNode(
      final List<Node<V>> children,
      final Optional<V> value,
      final NodeFactory<V> nodeFactory,
      final Function<V, Bytes> valueSerializer) {
    assert (children.size() == maxChild());
    this.location = Optional.empty();
    this.children = children;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public void accept(final Bytes location, final LocationNodeVisitor<V> visitor) {
    visitor.visit(location, this);
  }

  @Override
  public Optional<Bytes> getLocation() {
    return location;
  }

  @Override
  public Bytes getPath() {
    return Bytes.EMPTY;
  }

  @Override
  public Optional<V> getValue() {
    return value;
  }

  @Override
  public List<Node<V>> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public Node<V> child(final byte index) {
    return children.get(index);
  }

  @Override
  public Bytes getEncodedBytes() {
    if (encodedBytes != null) {
      final Bytes encoded = encodedBytes.get();
      if (encoded != null) {
        return encoded;
      }
    }
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    for (int i = 0; i < maxChild(); ++i) {
      out.writeRaw(children.get(i).getEncodedBytesRef());
    }
    if (value.isPresent()) {
      out.writeBytes(valueSerializer.apply(value.get()));
    } else {
      out.writeNull();
    }
    out.endList();
    final Bytes encoded = out.encoded();
    encodedBytes = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public Bytes getEncodedBytesRef() {
    if (isReferencedByHash()) {
      return RLP.encodeOne(getHash());
    } else {
      return getEncodedBytes();
    }
  }

  @Override
  public Bytes32 getHash() {
    if (hash != null) {
      final Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    final Bytes32 hashed = keccak256(getEncodedBytes());
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  @Override
  public Node<V> replacePath(final Bytes newPath) {
    return nodeFactory.createExtension(newPath, this);
  }

  public Node<V> replaceChild(final byte index, final Node<V> updatedChild) {
    return replaceChild(index, updatedChild, true);
  }

  public Node<V> replaceChild(
      final byte index, final Node<V> updatedChild, final boolean allowFlatten) {
    final ArrayList<Node<V>> newChildren = new ArrayList<>(children);
    newChildren.set(index, updatedChild);

    if (updatedChild == NULL_NODE) {
      if (value.isPresent() && !hasChildren()) {
        return nodeFactory.createLeaf(Bytes.of(index), value.get());
      } else if (value.isEmpty() && allowFlatten) {
        final Optional<Node<V>> flattened = maybeFlatten(newChildren);
        if (flattened.isPresent()) {
          return flattened.get();
        }
      }
    }

    return nodeFactory.createBranch(newChildren, value);
  }

  public Node<V> replaceValue(final V value) {
    return nodeFactory.createBranch(children, Optional.of(value));
  }

  public Node<V> removeValue() {
    return maybeFlatten(children).orElse(nodeFactory.createBranch(children, Optional.empty()));
  }

  protected boolean hasChildren() {
    for (final Node<V> child : children) {
      if (child != NULL_NODE) {
        return true;
      }
    }
    return false;
  }

  protected Optional<Node<V>> maybeFlatten(final List<Node<V>> children) {
    final int onlyChildIndex = findOnlyChild(children);
    if (onlyChildIndex >= 0) {
      // replace the path of the only child and return it
      final Node<V> onlyChild = children.get(onlyChildIndex);
      final Bytes onlyChildPath = onlyChild.getPath();
      final MutableBytes completePath = MutableBytes.create(1 + onlyChildPath.size());
      completePath.set(0, (byte) onlyChildIndex);
      onlyChildPath.copyTo(completePath, 1);
      return Optional.of(onlyChild.replacePath(completePath));
    }
    return Optional.empty();
  }

  private int findOnlyChild(final List<Node<V>> children) {
    int onlyChildIndex = -1;
    assert (children.size() == maxChild());
    for (int i = 0; i < maxChild(); ++i) {
      if (children.get(i) != NULL_NODE) {
        if (onlyChildIndex >= 0) {
          return -1;
        }
        onlyChildIndex = i;
      }
    }
    return onlyChildIndex;
  }

  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Branch:");
    builder.append("\n\tRef: ").append(getEncodedBytesRef());
    for (int i = 0; i < maxChild(); i++) {
      final Node<V> child = child((byte) i);
      if (!Objects.equals(child, NullNode.instance())) {
        final String branchLabel = "[" + Integer.toHexString(i) + "] ";
        final String childRep = child.print().replaceAll("\n\t", "\n\t\t");
        builder.append("\n\t").append(branchLabel).append(childRep);
      }
    }
    builder.append("\n\tValue: ").append(getValue().map(Object::toString).orElse("empty"));
    return builder.toString();
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }

  @Override
  public boolean isHealNeeded() {
    return needHeal;
  }

  @Override
  public void markHealNeeded() {
    this.needHeal = true;
  }

  public int maxChild() {
    return 16;
  }
}
