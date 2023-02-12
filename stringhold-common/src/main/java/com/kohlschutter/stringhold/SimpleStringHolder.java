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

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

/**
 * A {@link StringHolder} that holds a String, plain and simple.
 *
 * @author Christian Kohlschütter
 */
final class SimpleStringHolder extends AbstractStringHolder implements HasKnownLength {
  SimpleStringHolder(String s) {
    super(s.length());
    this.theString = s;
  }

  @Override
  @ExcludeFromCodeCoverageGeneratedReport
  protected String getString() {
    // should not be reachable; just defensive a measure
    return theString;
  }

  @Override
  @ExcludeFromCodeCoverageGeneratedReport
  protected void uncache() {
    // should not be reachable; just defensive a measure
  }

  @Override
  public boolean isEffectivelyImmutable() {
    return true;
  }
}
