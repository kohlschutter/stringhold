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

/**
 * Describes something that can return its exact length.
 *
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface HasLength extends HasExpectedLength, HasMinimumLength {
  /**
   * Returns the exact length.
   *
   * @return The exact length.
   */
  int length();

  @Override
  default int getMinimumLength() {
    return length();
  }

  @Override
  default int getExpectedLength() {
    return length();
  }

  /**
   * Checks if this instance is considered empty, i.e., equivalent to {@code length() == 0}.
   *
   * @return {@code true} if empty.
   */
  default boolean isEmpty() {
    return length() == 0;
  }

  /**
   * Checks if this instance is considered known empty, i.e., equivalent to
   * {@code getMinimumLength() == 0 && isLengthKnown() && isEmpty()}.
   *
   * @return {@code true} if known empty.
   */
  default boolean isKnownEmpty() {
    return getMinimumLength() == 0 //
        && isLengthKnown() //
        && isEmpty();
  }
}
