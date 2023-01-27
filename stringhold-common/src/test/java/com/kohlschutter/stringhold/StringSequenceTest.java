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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

  @Test
  public void testAsContent() throws Exception {
    StringHolderSequence seq;
    Object content;

    seq = new StringHolderSequence();
    content = seq.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("", content);

    seq = new StringHolderSequence().append("test");
    content = seq.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("test", content);

    seq = new StringHolderSequence().append(StringHolder.withContent("test"));
    content = seq.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("test", content);

    seq = new StringHolderSequence().append(StringHolder.withSupplier(() -> "test"));
    content = seq.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertEquals("test", content.toString());

    seq = new StringHolderSequence().append("hello").append("world");
    content = seq.asContent();
    assertInstanceOf(StringHolderSequence.class, content);
    assertEquals("helloworld", content.toString());
    // after calling toString on the StringHolderSequence, content is a string
    content = seq.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("helloworld", content);
  }

  @Test
  public void testAppends() throws Exception {
    StringHolderSequence seq = new StringHolderSequence(5);
    assertEquals(0, seq.numberOfAppends());
    seq.append(""); // doesn't count
    assertEquals(0, seq.numberOfAppends());
    seq.append(StringHolder.withContent("")); // doesn't count
    assertEquals(0, seq.numberOfAppends());
    seq.append(StringHolder.withSupplier(() -> "")); // does count
    assertEquals(1, seq.numberOfAppends());

    StringHolder sh = StringHolder.withSupplier(() -> "");
    sh.toString();
    seq.append(sh); // doesn't count
    assertEquals(1, seq.numberOfAppends());

    seq.append("Hello");
    assertEquals(2, seq.numberOfAppends());
    seq.append(' ');
    assertEquals(3, seq.numberOfAppends());
    seq.append(new WrappedCharSequence("World"));
    assertEquals(4, seq.numberOfAppends());

    assertEquals("Hello World", seq.toString());
  }
}
