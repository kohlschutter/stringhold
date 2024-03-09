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
 * Wraps another {@link CharSequence}. Used to forcibly de-optimize code when testing.
 *
 * @author Christian Kohlschütter
 */
final class WrappedCharSequence implements CharSequence {
  private final CharSequence wrapped;

  WrappedCharSequence(CharSequence wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public int length() {
    return wrapped.length();
  }

  @Override
  public char charAt(int index) {
    return wrapped.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return new WrappedCharSequence(wrapped.subSequence(start, end));
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }
}