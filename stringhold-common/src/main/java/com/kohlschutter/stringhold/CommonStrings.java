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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

final class CommonStrings {
  static final Map<Object, StringHolder> COMMON_STRINGS;

  static {
    Object[] items = {
        null, true, false, //
        "", " ", //
        ",", ".", //
        "\n  \n  \n", "\n  \n    \n  \n", "\n  \n    \n\n", "\n    \n    \n  ", "\n  \n\n  \n", //
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
      s = s.intern();
      StringHolder sh = new SimpleStringHolder(s);
      COMMON_STRINGS.put(s, sh);
      if (putObj) {
        COMMON_STRINGS.put(obj, sh);
      }
    }

    Set<String> entries = new HashSet<>();

    // Add common combinations of newline and spaces
    for (int spaces = 0; spaces <= 16; spaces++) {
      String s = String.format(Locale.ENGLISH, "%-" + (spaces + 1) + "s", "\n");
      entries.add(s);
      entries.add("\n" + s);
    }
    Set<String> newEntries = new HashSet<>();
    newEntries.addAll(entries);
    for (String en1 : entries) {
      for (String en2 : entries) {
        newEntries.add(en1 + en2);
      }
    }

    // Add sequences of newlines
    for (int i = 1; i < 20; i++) {
      newEntries.add(String.format(Locale.ENGLISH, "%" + i + "s", "\n").replace(' ', '\n'));
    }

    // Store interned versions only
    for (String en : newEntries) {
      String v = en.intern();
      COMMON_STRINGS.computeIfAbsent(v, (k) -> new SimpleStringHolder(v));
    }
  }

  static final StringHolder EMPTY_STRINGHOLDER = Objects.requireNonNull(COMMON_STRINGS.get(""));
  static final StringHolder NULL_STRINGHOLDER = Objects.requireNonNull(COMMON_STRINGS.get("null"));

  @ExcludeFromCodeCoverageGeneratedReport
  private CommonStrings() {
    throw new IllegalStateException("No instances");
  }

  static StringHolder lookup(Object s) {
    return COMMON_STRINGS.get(s);
  }

  static String lookupIfPossible(String s) {
    StringHolder common = lookup(s);
    if (common != null) {
      return common.toString();
    } else {
      return s;
    }
  }
}
