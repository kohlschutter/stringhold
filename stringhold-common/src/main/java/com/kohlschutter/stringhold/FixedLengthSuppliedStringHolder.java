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

import java.util.function.Supplier;

class FixedLengthSuppliedStringHolder extends SuppliedStringHolder {
  private final int len;

  FixedLengthSuppliedStringHolder(int len, Supplier<?> supplier) {
    super(len, len, supplier);
    this.len = len;
  }

  FixedLengthSuppliedStringHolder(int len, IOSupplier<?> supplier, IOExceptionHandler onError) {
    super(len, len, supplier, onError);
    this.len = len;
  }

  @Override
  public boolean isLengthKnown() {
    return true;
  }

  @Override
  protected int computeLength() {
    return len;
  }

  @Override
  protected void stringSanityCheck(String s) {
    if (s.length() != len) {
      setError();
      throw new IllegalStateException("String supplied with unexpected length: " + s.length()
          + "; expected: " + len);
    }
  }
}
