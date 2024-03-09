/*
 * stringhold
 *
 * Copyright 2022-2024 Christian Kohlschütter
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

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

/**
 * A scope that can be associated with {@link StringHolder} instances.
 *
 * The methods are callbacks from {@link StringHolder} code. {@link StringHolder}s can be
 * associated/removed via {@link StringHolder#updateScope(StringHolderScope)}.
 *
 * @author Christian Kohlschütter
 */
public interface StringHolderScope {
  /**
   * Placeholder to be used to indicate "no scope" where {@code null} is not allowed.
   */
  StringHolderScope NONE = new StringHolderScope() {
    @Override
    @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
    public void add(StringHolder sh) {
    }

    @Override
    @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
    public void remove(StringHolder sh) {
    }
  };

  /**
   * Called upon adding a {@link StringHolder} with this {@link StringHolderScope}.
   *
   * @param sh The {@link StringHolder}.
   */
  void add(StringHolder sh);

  /**
   * Called upon removing a {@link StringHolder} from this {@link StringHolderScope}.
   *
   * @param sh The {@link StringHolder}.
   */
  void remove(StringHolder sh);

  /**
   * Called upon a size change regarding the given {@link StringHolder}.
   *
   * @param minLengthBy The relative size change for the minimum length, can also be negative.
   * @param expectedLengthBy The relative size change for the expected length, can also be negative.
   */
  default void resizeBy(int minLengthBy, int expectedLengthBy) {
  }

  /**
   * Called upon a {@link StringHolder} entering error state, e.g., when a length constraint was
   * violated.
   *
   * @param sh The {@link StringHolder}.
   */
  default void setError(StringHolder sh) {
  }

  /**
   * Called upon a {@link StringHolder} leaving error state.
   *
   * @param sh The {@link StringHolder}.
   */
  default void clearError(StringHolder sh) {
  }
}
