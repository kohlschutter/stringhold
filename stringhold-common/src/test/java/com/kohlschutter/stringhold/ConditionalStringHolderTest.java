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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class ConditionalStringHolderTest {

  @Test
  public void testExcluded() throws Exception {
    AtomicInteger conditionChecked = new AtomicInteger(0);

    StringHolder csh;
    StringHolder seq = StringHolder.withContent("Hello", (csh = StringHolder
        .withConditionalStringHolder(StringHolder.withContent(" World"), (o) -> {
          conditionChecked.incrementAndGet();
          return false;
        })));

    assertEquals(0, conditionChecked.get());
    assertFalse(csh.isKnownEmpty());

    assertEquals(0, conditionChecked.get());
    assertTrue(csh.isEmpty());
    assertEquals(1, conditionChecked.get());

    StringBuilder out = new StringBuilder();
    csh.appendTo((Appendable) out);
    csh.appendTo(out);
    assertEquals(0, csh.appendToAndReturnLength((Appendable) out));
    assertEquals(0, csh.appendToAndReturnLength(out));
    assertEquals("", out.toString());

    StringBuffer buf = new StringBuffer();
    csh.appendTo(buf);
    assertEquals(0, csh.appendToAndReturnLength(buf));
    assertEquals("", buf.toString());

    StringWriter wri = new StringWriter();
    csh.appendTo(wri);
    assertEquals(0, csh.appendToAndReturnLength(wri));

    assertEquals("", csh.asContent());

    assertEquals(true, csh.isEmpty());
    assertEquals(0, csh.length());

    seq.appendTo(buf);
    assertEquals("Hello", buf.toString());
    assertEquals("Hello", seq.toString());

    assertEquals(1, conditionChecked.get());
  }

  @Test
  public void testIncluded() throws Exception {
    AtomicInteger conditionChecked = new AtomicInteger(0);

    StringHolder csh;
    StringHolder seq = StringHolder.withContent("Hello", (csh = StringHolder
        .withConditionalStringHolder(StringHolder.withContent(" World"), (o) -> {
          conditionChecked.incrementAndGet();
          return true;
        })));

    assertEquals(0, conditionChecked.get());
    assertFalse(csh.isKnownEmpty());

    assertEquals(0, conditionChecked.get());
    assertFalse(csh.isEmpty());
    assertEquals(1, conditionChecked.get());

    StringBuilder out = new StringBuilder();
    csh.appendTo((Appendable) out);
    csh.appendTo(out);
    assertEquals(" World".length(), csh.appendToAndReturnLength((Appendable) out));
    assertEquals(" World".length(), csh.appendToAndReturnLength(out));
    assertEquals(" World".repeat(4), out.toString());

    StringBuffer buf = new StringBuffer();
    csh.appendTo(buf);
    assertEquals(" World".length(), csh.appendToAndReturnLength(buf));
    assertEquals(" World".repeat(2), buf.toString());

    StringWriter wri = new StringWriter();
    csh.appendTo(wri);
    assertEquals(" World".length(), csh.appendToAndReturnLength(wri));

    assertEquals(" World", csh.asContent());

    assertEquals(false, csh.isEmpty());
    assertEquals(" World".length(), csh.length());

    buf.setLength(0);
    seq.appendTo(buf);
    assertEquals("Hello World", buf.toString());
    assertEquals("Hello World", seq.toString());

    assertEquals(1, conditionChecked.get());
  }

  @Test
  public void testEffectivelyImmutable() throws Exception {
    AtomicInteger conditionChecked = new AtomicInteger(0);

    StringHolder seq = StringHolder.withContent(StringHolder.withSupplier(() -> "Hello "),
        StringHolder.withSupplier(() -> "World"));
    StringHolder csh = StringHolder.withConditionalStringHolder(seq, (o) -> {
      conditionChecked.incrementAndGet();
      return false;
    });
    assertEquals(0, conditionChecked.get());
    assertFalse(csh.isEffectivelyImmutable());
    csh.markEffectivelyImmutable();
    assertTrue(csh.isEffectivelyImmutable());
    assertEquals(1, conditionChecked.get());
  }

  @Test
  public void testCharSequence() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> true);
    assertFalse(excl.isString());
    assertFalse(incl.isString());

    assertEquals('H', incl.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> excl.charAt(0));

    assertEquals("", incl.subSequence(0, 0));
    assertEquals("", excl.subSequence(0, 0));

    assertEquals("He", incl.subSequence(0, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> excl.subSequence(0, 2));

    assertTrue(excl.isString());
    assertTrue(incl.isString());
  }

  @Test
  public void testExpectedLength() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder
        .withSupplierExpectedLength(5, () -> "Hello"), (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder
        .withSupplierExpectedLength(5, () -> "Hello"), (o) -> true);

    assertEquals(5, excl.getExpectedLength());
    assertEquals(5, incl.getExpectedLength());

    excl.setExpectedLength(10);
    incl.setExpectedLength(10);
    assertEquals(10, excl.getExpectedLength());
    assertEquals(10, incl.getExpectedLength());

    assertFalse(excl.isKnownEmpty());
    assertFalse(incl.isKnownEmpty());

    assertTrue(excl.isEmpty());
    assertFalse(incl.isEmpty());

    assertEquals(0, excl.getExpectedLength());
    assertEquals(5, incl.getExpectedLength());

    excl.setExpectedLength(10);
    incl.setExpectedLength(10);
    assertEquals(0, excl.getExpectedLength());
    assertEquals(5, incl.getExpectedLength());

    assertFalse(excl.checkError());
    assertFalse(incl.checkError());
  }

  @Test
  public void testLength() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder
        .withSupplierExpectedLength(5, () -> "Hello"), (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder
        .withSupplierExpectedLength(5, () -> "Hello"), (o) -> true);

    assertFalse(excl.isLengthKnown());
    assertFalse(incl.isLengthKnown());

    assertTrue(excl.isEmpty());
    assertFalse(incl.isEmpty());

    assertTrue(excl.isLengthKnown());
    assertTrue(incl.isLengthKnown());
    assertEquals(0, excl.length());
    assertEquals(5, incl.length());

    assertFalse(excl.checkError());
    assertFalse(incl.checkError());
  }

  @Test
  public void testReader() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> true);

    try (BufferedReader br = new BufferedReader(excl.toReader())) {
      assertEquals(null, br.readLine());
    }
    try (BufferedReader br = new BufferedReader(incl.toReader())) {
      assertEquals("Hello", br.readLine());
    }
  }

  @Test
  public void testScope() throws Exception {
    StringHolder sc;
    StringHolder excl = StringHolder.withConditionalStringHolder((sc = StringHolder.withContent(
        "Hello")), (o) -> false);

    LimitedStringHolderScope scope = LimitedStringHolderScope.withNoLimits();
    assertNull(excl.getScope());
    assertNull(excl.updateScope(scope));
    assertEquals(sc.getScope(), excl.getScope());
    assertEquals(scope, excl.updateScope(scope));
    assertEquals(sc.getScope(), excl.getScope());
  }

  @Test
  public void testToString() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> true);

    assertEquals("", excl.toString());
    assertEquals("Hello", incl.toString());
  }

  @Test
  public void testCompareToCharSequence() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> true);

    assertEquals(0, excl.compareTo(""));
    assertEquals(-1, excl.compareTo("Hello"));
    assertEquals(1, Math.min(1, incl.compareTo("")));
    assertEquals(0, incl.compareTo("Hello"));
  }

  @Test
  public void testCompareToStringHolder() throws Exception {
    StringHolder excl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> false);
    StringHolder incl = StringHolder.withConditionalStringHolder(StringHolder.withContent("Hello"),
        (o) -> true);

    assertEquals(0, excl.compareTo(StringHolder.withContent("")));
    assertEquals(-1, excl.compareTo(StringHolder.withContent("Hello")));
    assertEquals(1, Math.min(1, incl.compareTo(StringHolder.withContent(""))));
    assertEquals(0, incl.compareTo(StringHolder.withContent("Hello")));
  }

  @Test
  public void testUncacheableStringHolder() throws Exception {
    StringHolder sh;

    sh = StringHolder.withUncacheableStringHolder(StringHolder.withSupplier(() -> ""));
    assertFalse(sh.isCacheable());
    sh.toString();
    assertTrue(sh.isCacheable());

    sh = StringHolder.withUncacheableStringHolder(StringHolder.withContent(""));
    assertSame(sh, StringHolder.withUncacheableStringHolder(sh));
    assertFalse(sh.isCacheable());
    sh.toString();
    assertTrue(sh.isCacheable());
    assertNotSame(sh, StringHolder.withUncacheableStringHolder(sh));
  }
}
