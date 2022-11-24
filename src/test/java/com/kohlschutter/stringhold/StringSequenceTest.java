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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class StringSequenceTest {

  @Test
  public void testAppendSupplied() {
    testAppendSupplied(new StringHolderSequence(), false);
    testAppendSupplied(new StringOnlySequence(), true);
  }

  private void testAppendSupplied(StringHolderSequence seq, boolean isStringAfterAppend) {
    StringHolder sh = StringHolder.withSupplier(() -> "Hello");
    assertFalse(sh.isString());
    seq.append(sh);
    assertEquals(isStringAfterAppend, sh.isString()); // not true for a regular StringHolderSequence
    seq.append(" ");
    seq.append(StringHolder.withContent("World"));

    assertEquals("Hello World", seq.toString());
  }
}
