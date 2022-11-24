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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class StringHolderSequenceTest {

  private StringHolderSequence foo12barSeq() {
    StringHolderSequence seq = new StringHolderSequence();
    seq.append("foo");

    StringHolderSequence seq1 = new StringHolderSequence();
    seq.append(seq1);
    seq1.append(".1");

    StringHolderSequence seq2 = new StringHolderSequence();
    seq2.append(".2");
    seq.append((Object) seq2);

    seq.append("bar");

    seq.append('3');

    return seq;
  }

  @Test
  public void testEmptySequence() throws Exception {
    assertEquals(new StringHolderSequence(), "");
    assertEquals(new StringHolderSequence().appendAll(""), "");
    assertEquals(new StringHolderSequence().appendAll("", ""), "");
    assertEquals(new StringHolderSequence().appendAll(new StringBuilder(), ""), "");
    assertEquals(new StringHolderSequence().appendAll(List.of()), "");
    assertEquals(new StringHolderSequence().appendAll(List.of(new StringBuilder(), "")), "");
  }

  @Test
  public void testNull() throws Exception {
    // a value that is null -> String.valueOf(null)=="null"
    assertEquals(new StringHolderSequence().appendAll((Object) null), "null");

    // two values that are null -> String.valueOf(null)=="null", twice
    assertEquals(new StringHolderSequence().appendAll(null, null), "nullnull");

    // null array is not permitted
    assertThrows(NullPointerException.class, //
        () -> new StringHolderSequence().appendAll((Object[]) null));
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
    StringHolderSequence seq = new StringHolderSequence().appendAll(sh);
    sh.toString();
    seq.appendTo(new WrappedAppendable(sb));
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_StringBuilder() throws Exception {
    StringBuilder sb = new StringBuilder();

    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = new StringHolderSequence().appendAll(sh);
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
    StringHolderSequence seq = new StringHolderSequence().appendAll(sh);
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
    StringHolderSequence seq = new StringHolderSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());
    seq.appendTo(sb);
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_Reader() throws Exception {
    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringHolderSequence seq = new StringHolderSequence().appendAll(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());

    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testEmptySequenceReader() throws Exception {
    StringHolderSequence seq = new StringHolderSequence();
    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testReaderClose() throws Exception {
    StringHolderSequence seq = new StringHolderSequence().appendAll("A", "B", "C");
    try (Reader reader = seq.toReader()) {
      assertTrue(reader.ready());
      reader.close();
      assertThrows(IOException.class, () -> reader.ready());
    }
  }

  @Test
  public void testReaderRead() throws Exception {
    StringHolderSequence seq = new StringHolderSequence().appendAll("", StringHolder.withSupplier(
        () -> ""), "A", "", StringHolder.withSupplier(() -> ""), StringHolder
            .withSupplierMinimumLength(1, () -> "B"), new StringHolderSequence().appendAll("CDE",
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
}
