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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

/**
 * A StringHolder that holds a String, plain and simple.
 *
 * @author Christian Kohlschütter
 */
final class SimpleStringHolder extends StringHolder implements HasKnownLength {
  static final Map<Object, StringHolder> COMMON_STRINGS;

  static {
    Object[] items = {
        "", " ", "null", //
        ",", ".", //
        "\n", "\n ", "\n  ", "\n   ", "\n    ", "\n     ", "\n      ", "\n       ", "\n        ",
        "\n         ", "\n          ", "\n           ", "\n            ", //
        "\n\n", "\n\n  ", //
        true, false, //
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, //
        "src", "alt", "read", "width", "height", "style", "small", //
        "1600", "800", "480", //
        "<h", "</h" //
    };

    COMMON_STRINGS = new HashMap<>(items.length + 16);
    boolean putObj;
    for (Object obj : items) {
      String s;
      if (obj instanceof String) {
        s = (String) obj;
        putObj = false;
      } else {
        s = String.valueOf(obj);
        putObj = true;
      }
      StringHolder sh = new SimpleStringHolder(s);
      COMMON_STRINGS.put(s, sh);
      if (putObj) {
        COMMON_STRINGS.put(obj, sh);
      }
    }
  }

  static final StringHolder EMPTY_STRING = Objects.requireNonNull(COMMON_STRINGS.get(""));
  static final StringHolder NULL_STRING = Objects.requireNonNull(COMMON_STRINGS.get("null"));

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
}
