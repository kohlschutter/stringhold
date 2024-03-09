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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class StringHolderSequenceTest {

  private StringHolderSequence foo12barSeq() {
    StringHolderSequence seq = StringHolder.newSequence();
    seq.append("foo");

    StringHolderSequence seq1 = StringHolder.newSequence();
    seq.append(seq1);
    seq1.append(".1");

    StringHolderSequence seq2 = StringHolder.newSequence();
    seq2.append(".2");
    seq.append((Object) seq2);

    seq.append("bar");

    seq.append('3');

    return seq;
  }

  @SuppressWarnings("null")
  @Test
  public void testEmptySequence() throws Exception {
    assertEquals(StringHolder.newSequence(), "");
    assertEquals(StringHolder.newSequence().appendAll(""), "");
    assertEquals(StringHolder.newSequence().appendAll("", ""), "");
    assertEquals(StringHolder.newSequence().appendAll(new StringBuilder(), ""), "");
    assertEquals(StringHolder.newSequence().appendAll(List.of()), "");
    assertEquals(StringHolder.newSequence().appendAll(List.of(new StringBuilder(), "")), "");
  }

  @Test
  public void testNull() throws Exception {
    // a value that is null -> String.valueOf(null)=="null"
    assertEquals(StringHolder.newSequence().appendAll((Object) null), "null");

    // two values that are null -> String.valueOf(null)=="null", twice
    assertEquals(StringHolder.newSequence().appendAll(null, null), "nullnull");

    // null array is not permitted
    assertThrows(NullPointerException.class, //
        () -> StringHolder.newSequence().appendAll((Object[]) null));
  }

  @Test
  public void testAppendTo() throws IOException {
    StringHolderSequence seq = foo12barSeq();

    StringWriter out = new StringWriter();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToStringBuilder() {
    StringHolderSequence seq = foo12barSeq();

    StringBuilder out = new StringBuilder();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToStringBuffer() {
    StringHolderSequence seq = foo12barSeq();

    StringBuffer out = new StringBuffer();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToFlatList() {
    StringHolderSequence seq = foo12barSeq();
    seq.append(new StringBuilder("baz"));

    List<Object> list = new ArrayList<>();
    seq.appendToFlatList(list);

    // any nested StringSequences are flattened
    assertArrayEquals(new Object[] {"foo", ".1", ".2", "bar", "3", "baz"}, list.toArray(
        new Object[4]));
  }

  @Test
  public void testAppendToFlatListWithSupplier() {
    StringHolderSequence seq = foo12barSeq();

    StringHolder emptySupplier = StringHolder.withSupplier(() -> "");
    emptySupplier.toString();
    seq.append(emptySupplier);

    StringHolder supplier = StringHolder.withSupplier(() -> ":supply");
    seq.append(supplier);

    List<Object> list = new ArrayList<>();
    seq.appendToFlatList(list);

    // StringHolders that are not String yet are returned as-is
    // StringHolders that are String and empty are skipped
    assertArrayEquals(new Object[] {"foo", ".1", ".2", "bar", "3", supplier}, list.toArray(
        new Object[0]));
  }

  @Test
  public void testAppendToWithKnownEmpty() throws Exception {
    StringBuilder sb = new StringBuilder();

    StringHolder sh = StringHolder.withSupplier(() -> "");
    StringHolderSequence seq = StringHolder.newSequence().appendAll(sh);
    sh.toString();
    seq.appendTo(new WrappedAppendable(sb));
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_StringBuilder() throws Exception {
    StringBuilder sb = new StringBuilder();

    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = StringHolder.newSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());
    seq.appendTo(sb);
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_StringBuffer() throws Exception {
    StringBuffer sb = new StringBuffer();

    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = StringHolder.newSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());
    seq.appendTo(sb);
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_Writer() throws Exception {
    StringWriter sb = new StringWriter();

    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = StringHolder.newSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());
    seq.appendTo(sb);
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_Reader() throws Exception {
    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = StringHolder.newSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());

    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testEmptySequenceReader() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testReaderClose() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence().appendAll("A", "B", "C");
    try (Reader reader = seq.toReader()) {
      assertTrue(reader.ready());
      reader.close();
      assertThrows(IOException.class, () -> reader.ready());
    }
  }

  @Test
  public void testReaderRead() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence().appendAll("", StringHolder.withSupplier(
        () -> ""), "A", "", StringHolder.withSupplier(() -> ""), StringHolder
            .withSupplierMinimumLength(1, () -> "B"), StringHolder.newSequence().appendAll("CDE",
                "F", "GH"), StringHolder.withSupplier(() -> ""));
    seq.append(StringHolder.withSupplierMinimumLength(0, () -> ""));
    Reader r = seq.toReader();

    assertEquals('A', r.read());
    char[] cbuf = new char[3];
    assertEquals(1, r.read(cbuf, 0, 1));
    assertEquals('B', cbuf[0]);

    assertEquals(3, r.read(cbuf));
    assertArrayEquals("CDE".toCharArray(), cbuf);

    // Currently, we honor SequenceHolder boundaries, but do not rely upon them
    // assertEquals(1, r.read(cbuf));
    // assertEquals(2, r.read(cbuf));

    assertEquals('F', r.read());
    assertEquals('G', r.read());
    assertEquals('H', r.read());

    assertEquals(-1, r.read());
    assertEquals(-1, r.read(cbuf));
  }

  @Test
  public void testAppendCharSequenceRange() throws Exception {
    StringHolderSequence seq = new StringHolderSequence(5);

    seq.append("WAT", 2, 2); // empty range

    seq.append("Oh, Hello there", 4, 9); // Hello

    StringBuilder sb = new StringBuilder("1 2,3:4");
    seq.append(sb, 1, 2); // space

    StringBuffer sb2 = new StringBuffer("Wo");
    seq.append(sb2, 0, 2); // Wo

    WrappedCharSequence cs = new WrappedCharSequence(new StringBuilder("world"));
    seq.append(cs, 2, 5); // rld

    assertEquals("Hello World", seq.toString());
  }

  @Test
  public void testCharAtStrings() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(-1));

    seq.append("a");
    seq.append("b");
    seq.append("c");

    assertEquals('a', seq.charAt(0));
    assertEquals('b', seq.charAt(1));
    assertEquals('c', seq.charAt(2));
    assertEquals('a', seq.charAt(0));

    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(3));
  }

  @Test
  public void testCharAtStringHoldersSingleChars() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(-1));

    StringHolder sh1 = StringHolder.withSupplier(() -> "a");
    StringHolder sh2 = StringHolder.withSupplier(() -> "b");
    StringHolder sh3 = StringHolder.withSupplier(() -> "c");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('a', seq.charAt(0));
    assertTrue(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('b', seq.charAt(1));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('c', seq.charAt(2));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('a', seq.charAt(0));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(3));
  }

  @Test
  public void testCharAtStringHolders() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(-1));

    StringHolder sh1 = StringHolder.withSupplier(() -> "aA");
    StringHolder sh2 = StringHolder.withSupplier(() -> "bBBB");
    StringHolder shEmpty = StringHolder.withSupplier(() -> "");
    StringHolder sh3 = StringHolder.withSupplier(() -> "cCCCCC");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(shEmpty); // added because we didn't know it was empty
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('a', seq.charAt(0));
    assertTrue(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('b', seq.charAt(2));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('c', seq.charAt(6));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('a', seq.charAt(0));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('C', seq.charAt(11));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(12));
  }

  @Test
  public void testCharAtStringHolders2() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(-1));

    StringHolder sh1 = StringHolder.withSupplier(() -> "aA");
    StringHolder sh2 = StringHolder.withSupplier(() -> "bBBB");
    StringHolder sh3 = StringHolder.withSupplier(() -> "cCCCCC");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('A', seq.charAt(1));
    assertTrue(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('B', seq.charAt(3));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('C', seq.charAt(7));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('a', seq.charAt(0));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('c', seq.charAt(6));
    assertThrows(IndexOutOfBoundsException.class, () -> seq.charAt(12));
  }

  @Test
  public void testCharAtReversedOrder() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "aA");
    StringHolder sh2 = StringHolder.withSupplier(() -> "bBBB");
    StringHolder sh3 = StringHolder.withSupplier(() -> "cCCCCC");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('C', seq.charAt(7));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('B', seq.charAt(3));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('A', seq.charAt(1));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());
  }

  @Test
  public void testCharAtReversedOrderMinimumLength() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();

    StringHolder sh1 = StringHolder.withSupplierMinimumLength(2, () -> "aA");
    StringHolder sh2 = StringHolder.withSupplierMinimumLength(4, () -> "bBBB");
    StringHolder sh3 = StringHolder.withSupplierMinimumLength(6, () -> "cCCCCC");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());

    assertEquals('C', seq.charAt(7));
    assertTrue(sh1.isString()); // minimum length still means we have to check the full string.
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('B', seq.charAt(3));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());

    assertEquals('A', seq.charAt(1));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());
  }

  @Test
  public void testCharAtReversedOrderFixedLength() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();

    StringHolder sh1 = StringHolder.withSupplierFixedLength(2, () -> "aA");
    StringHolder sh2 = StringHolder.withSupplierFixedLength(4, () -> "bBBB");
    StringHolder sh3 = StringHolder.withSupplierFixedLength(6, () -> "cCCCCC");

    seq.append(sh1);
    seq.append(sh2);
    seq.append(sh3);

    assertFalse(sh1.isString());
    assertFalse(sh2.isString());
    assertFalse(sh3.isString());
    assertFalse(seq.isString());

    assertEquals('C', seq.charAt(7));
    assertFalse(sh1.isString()); // fixed length: we skip over these results without conversion
    assertFalse(sh2.isString());
    assertTrue(sh3.isString());
    assertFalse(seq.isString());

    assertEquals('B', seq.charAt(3));
    assertFalse(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());
    assertFalse(seq.isString());

    assertEquals('A', seq.charAt(1));
    assertTrue(sh1.isString());
    assertTrue(sh2.isString());
    assertTrue(sh3.isString());
    assertFalse(seq.isString()); // still three separate strings
  }

  @Test
  public void testLength() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    seq.append("Hello");
    seq.append(' ');
    seq.append(StringHolder.withContent("World"));
    assertEquals("Hello World".length(), seq.length());
    assertFalse(seq.isString());
  }

  @Test
  public void testCacheable() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    seq.append("Hello");
    seq.append(' ');
    seq.append(StringHolder.withContent("World"));
    assertTrue(seq.isCacheable());

    StringHolder sh = StringHolder.withUncacheableStringHolder(StringHolder.withContent("!!!"));
    seq.append(sh);
    assertFalse(seq.isCacheable());
    sh.toString();
    assertTrue(seq.isCacheable());
  }

  @Test
  public void testAppendCharSequenceHolder() throws Exception {
    StringHolderSequence seq = StringHolder.newSequence();
    CharSequence cs = StringHolder.withSupplier(() -> "hello");
    seq.append(cs);
    assertEquals("hello", seq.toString());
  }

  @Test
  public void testClone() throws Exception {
    AtomicInteger helloSupplies = new AtomicInteger();

    StringHolderSequence seq1 = StringHolder.newSequence();
    seq1.append(StringHolder.withSupplier(() -> {
      helloSupplies.incrementAndGet();
      return "hello";
    }));

    StringHolderSequence seq2 = seq1.clone();
    seq1.append(" there");
    seq2.append(" world");
    assertEquals("hello world", seq2.toString());
    assertEquals("hello there", seq1.toString());

    assertEquals(2, helloSupplies.get());
  }
}
