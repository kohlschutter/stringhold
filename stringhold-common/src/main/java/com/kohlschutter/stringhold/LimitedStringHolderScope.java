/*
 * stringhold
 *
 * Copyright 2022, 2023 Christian Kohlschütter
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

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A {@link StringHolderScope} that keeps track of the aggregate minimum length of all associated
 * {@link StringHolder}s and, optionally, invokes a given callback whenever the limit is exceeded.
 *
 * NOTE: This implementation does not keep track of {@link StringHolder}s being garbage-collected
 * and thus removed implicitly from memory without triggering {@link #remove(StringHolder)}. If
 * desired, you may want to implement a {@link WeakReference}-based wrapper.
 *
 * @author Christian Kohlschütter
 */
public final class LimitedStringHolderScope implements StringHolderScope {
  private final AtomicInteger minimumLength = new AtomicInteger();
  private final AtomicInteger expectedLength = new AtomicInteger();
  private final int minLengthLimit;
  private final int expectedLengthLimit;
  private final Consumer<StringHolder> onLimitExceeded;

  private LimitedStringHolderScope() {
    this(Integer.MAX_VALUE, Integer.MAX_VALUE, null);
  }

  private LimitedStringHolderScope(int minLengthLimit, int expectedLengthLimit,
      Consumer<StringHolder> onLimitExceeded) {
    this.minLengthLimit = minLengthLimit;
    this.expectedLengthLimit = expectedLengthLimit;
    this.onLimitExceeded = onLimitExceeded;
  }

  /**
   * Constructs a new {@link LimitedStringHolderScope} with no length limits.
   *
   * Useful if you want to occasionally call {@link #getMinimumLength()} or
   * {@link #getExpectedLength()}.
   *
   * @return The new scope.
   */
  public static LimitedStringHolderScope withNoLimits() {
    return new LimitedStringHolderScope();
  }

  /**
   * Constructs a new {@link LimitedStringHolderScope} with an upper limit for the minimum length.
   *
   * The given callback is invoked when the limit is exceeded.
   *
   * @param minLengthLimit The limit for the minimum length.
   * @param onLimitExceeded Called when adding/resizing a {@link StringHolder} that exceeds the
   *          limit.
   * @return The new scope.
   */
  public static LimitedStringHolderScope withUpperLimitForMinimumLength(int minLengthLimit,
      Consumer<StringHolder> onLimitExceeded) {
    return new LimitedStringHolderScope(minLengthLimit, Integer.MAX_VALUE, Objects.requireNonNull(
        onLimitExceeded));
  }

  /**
   * Constructs a new {@link LimitedStringHolderScope} with an upper limit for the expected length.
   *
   * The given callback is invoked when the limit is exceeded.
   *
   * @param expectedLengthLimit The limit for the expected length.
   * @param onLimitExceeded Called when adding/resizing a {@link StringHolder} that exceeds the
   *          limit.
   * @return The new scope.
   */
  public static LimitedStringHolderScope withUpperLimitForExpectedLength(int expectedLengthLimit,
      Consumer<StringHolder> onLimitExceeded) {
    return new LimitedStringHolderScope(expectedLengthLimit, expectedLengthLimit, Objects
        .requireNonNull(onLimitExceeded));
  }

  /**
   * Constructs a new {@link LimitedStringHolderScope} with separate upper limits for the minimum
   * length and expected length.
   *
   * The given callback is invoked when the limit is exceeded.
   *
   * @param minLengthLimit The limit for the minimum length.
   * @param expectedLengthLimit The limit for the expected length.
   * @param onLimitExceeded Called when adding/resizing a {@link StringHolder} that exceeds the
   *          limit.
   * @return The new scope.
   */
  public static LimitedStringHolderScope withUpperLimitForMinimumAndExpectedLength(
      int minLengthLimit, int expectedLengthLimit, Consumer<StringHolder> onLimitExceeded) {
    return new LimitedStringHolderScope(minLengthLimit, expectedLengthLimit, Objects.requireNonNull(
        onLimitExceeded));
  }

  /**
   * Returns the currently recorded aggregate minimum length for all associated
   * {@link StringHolder}s.
   *
   * @return The aggregate minimum length.
   */
  public int getMinimumLength() {
    return minimumLength.get();
  }

  /**
   * Returns the currently recorded aggregate expected length for all associated
   * {@link StringHolder}s.
   *
   * @return The aggregate expected length.
   */
  public int getExpectedLength() {
    return expectedLength.get();
  }

  @Override
  public void add(StringHolder sh) {
    int ml = minimumLength.addAndGet(sh.getMinimumLength());
    int el = expectedLength.addAndGet(sh.getExpectedLength());

    if (ml > minLengthLimit || el > expectedLengthLimit) {
      onLimitExceeded.accept(sh);
    }
  }

  @Override
  public void remove(StringHolder sh) {
    minimumLength.addAndGet(-sh.getMinimumLength());
    expectedLength.addAndGet(-sh.getExpectedLength());
  }

  @Override
  public void resizeBy(int minBy, int expBy) {
    int ml = minimumLength.addAndGet(minBy);
    int el = expectedLength.addAndGet(expBy);

    if (ml > minLengthLimit || el > expectedLengthLimit) {
      onLimitExceeded.accept(null);
    }
  }
}
