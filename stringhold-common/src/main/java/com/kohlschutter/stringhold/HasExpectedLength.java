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

/**
 * Describes something that can return its expected length.
 *
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface HasExpectedLength {

  /**
   * Returns the expected length.
   *
   * @return The expected length.
   */
  int getExpectedLength();

  /**
   * Checks if the length is known, i.e., available without additional computation.
   *
   * @return {@code true} if length is known.
   */
  default boolean isLengthKnown() {
    return false;
  }
}
