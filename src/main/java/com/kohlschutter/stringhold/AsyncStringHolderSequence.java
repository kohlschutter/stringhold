/*
 * stringhold
 *
 * Copyright 2022 Christian Kohlschütter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kohlschutter.stringhold;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An appendable sequence of strings; {@link StringHolder}s are automatically converted upon append,
 * with appends being run asynchronously, using temporary intermediate storage, if
 * possible/necessary.
 *
 * @author Christian Kohlschütter
 */
public class AsyncStringHolderSequence extends StringHolderSequence {
  private final Executor executor;

  /**
   * Constructs a new, empty {@link AsyncStringHolderSequence}.
   */
  public AsyncStringHolderSequence() {
    this(null);
  }

  /**
   * Constructs a new, empty {@link AsyncStringHolderSequence}.
   *
   * @param executor The executor to use.
   */
  public AsyncStringHolderSequence(Executor executor) {
    this(10, executor);
  }

  /**
   * Constructs a new, empty {@link AsyncStringHolderSequence}.
   *
   * @param estimatedNumberOfAppends Estimated number of calls to {@link #append(Object)}, etc.
   * @param executor The executor to use.
   */
  public AsyncStringHolderSequence(int estimatedNumberOfAppends, Executor executor) {
    super(estimatedNumberOfAppends);
    this.executor = executor;
  }

  @FunctionalInterface
  private interface Append {
    void append(CharSequence s) throws IOException;
  }

  @FunctionalInterface
  private interface EnsureCapacity {
    void ensureExtraCapacity(int extraLen);
  }

  @Override
  protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
    return appendToAndReturnLengthImpl(out, out::append, (extraLen) -> {
    }, false);
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuilder out) {
    try {
      final int len = out.length();
      return appendToAndReturnLengthImpl(out, out::append, (extraLen) -> out.ensureCapacity(len
          + extraLen), true);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuffer out) {
    try {
      final int len = out.length();
      return appendToAndReturnLengthImpl(out, out::append, (extraLen) -> out.ensureCapacity(len
          + extraLen), true);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("PMD.CognitiveComplexity")
  private int appendToAndReturnLengthImpl(Appendable out, Append append,
      EnsureCapacity ensureCapacity, boolean reuseFirst) throws IOException {
    ensureCapacity.ensureExtraCapacity(getMinimumLength());

    Object[] tempBuilds = new Object[sequence.size()];
    if (reuseFirst) {
      tempBuilds[0] = out;
    }

    List<CompletableFuture<Integer>> futures = new ArrayList<>();

    int len = 0;
    int i = 0;
    int tempBuildMax = 0;
    boolean needScatterGather = false;
    for (Object obj : sequence) {
      if (obj instanceof StringHolder) {
        StringHolder holder = (StringHolder) obj;
        if (holder.isKnownEmpty()) {
          continue;
        } else if (holder.isString()) {
          String s = holder.toString();
          if (needScatterGather) {
            tempBuildMax = i;
            tempBuilds[i] = s;
          } else {
            append.append(s);
          }
          len += s.length();
        } else {
          needScatterGather = true;
          tempBuildMax = i;

          StringBuilder sb;
          if (i > 0 || !reuseFirst) {
            sb = new StringBuilder(holder.getMinimumLength());
            tempBuilds[i] = sb;
          } else {
            sb = (StringBuilder) tempBuilds[0];
          }

          Supplier<Integer> suppl = () -> holder.appendToAndReturnLength(sb);

          CompletableFuture<Integer> future;
          if (executor == null) {
            future = CompletableFuture.supplyAsync(suppl);
          } else {
            future = CompletableFuture.supplyAsync(suppl, executor);
          }

          futures.add(future);
        }
      } else {
        String s = (String) obj;
        len += s.length();
        if (needScatterGather) {
          tempBuildMax = i;
          tempBuilds[i] = s;
        } else {
          append.append(s);
        }
      }
      i++;
    }

    if (!futures.isEmpty()) {
      len += futures.stream().map(CompletableFuture::join).collect(Collectors.summingInt((s) -> s));
    }

    ensureCapacity.ensureExtraCapacity(len);
    for (i = reuseFirst ? 1 : 0; i <= tempBuildMax; i++) {
      CharSequence sb = (CharSequence) tempBuilds[i];
      if (sb != null) {
        append.append(sb);
      }
    }

    return len;
  }
}
