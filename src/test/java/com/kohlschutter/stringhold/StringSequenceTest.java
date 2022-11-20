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

public class StringSequenceTest {

  private StringSequence foo12barSeq() {
    StringSequence seq = new StringSequence();
    seq.append("foo");

    StringSequence seq1 = new StringSequence();
    seq.append(seq1);
    seq1.append(".1");

    StringSequence seq2 = new StringSequence();
    seq2.append(".2");
    seq.append((Object) seq2);

    seq.append("bar");

    seq.append('3');

    return seq;
  }

  @Test
  public void testEmptySequence() throws Exception {
    assertEquals(new StringSequence(), "");
    assertEquals(new StringSequence(""), "");
    assertEquals(new StringSequence("", ""), "");
    assertEquals(new StringSequence(new StringBuilder(), ""), "");

  }

  @Test
  public void testNull() throws Exception {
    // no values
    assertEquals(new StringSequence((Object[]) null), "");

    // a value that is null -> String.valueOf(null)=="null"
    assertEquals(new StringSequence((Object) null), "null");

    // two values that are null -> String.valueOf(null)=="null", twice
    assertEquals(new StringSequence(null, null), "nullnull");
  }

  @Test
  public void testAppendTo() throws IOException {
    StringSequence seq = foo12barSeq();

    StringWriter out = new StringWriter();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToStringBuilder() {
    StringSequence seq = foo12barSeq();

    StringBuilder out = new StringBuilder();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToStringBuffer() {
    StringSequence seq = foo12barSeq();

    StringBuffer out = new StringBuffer();
    seq.appendTo(out);

    assertEquals("foo.1.2bar3", out.toString());
  }

  @Test
  public void testAppendToFlatList() {
    StringSequence seq = foo12barSeq();
    seq.append(new StringBuilder("baz"));

    List<Object> list = new ArrayList<>();
    seq.appendToFlatList(list);

    // any nested StringSequences are flattened
    assertArrayEquals(new Object[] {"foo", ".1", ".2", "bar", "3", "baz"}, list.toArray(
        new Object[4]));
  }

  @Test
  public void testAppendToFlatListWithSupplier() {
    StringSequence seq = foo12barSeq();

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
    StringSequence seq = new StringSequence(sh);
    sh.toString();
    seq.appendTo(new AppendableWrapper(sb));
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_StringBuilder() throws Exception {
    StringBuilder sb = new StringBuilder();

    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringSequence seq = new StringSequence(sh);
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
    StringSequence seq = new StringSequence(sh);
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
    StringSequence seq = new StringSequence(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());
    seq.appendTo(sb);
    assertEquals("", sb.toString());
  }

  @Test
  public void testAppendToWithKnownEmpty_Reader() throws Exception {
    StringHolder sh = StringHolder.withSupplierExpectedLength(123, () -> "");
    assertEquals(123, sh.getExpectedLength());
    StringSequence seq = new StringSequence(sh);
    sh.toString();
    assertEquals(0, sh.getExpectedLength());

    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testEmptySequenceReader() throws Exception {
    StringSequence seq = new StringSequence();
    try (Reader reader = seq.toReader()) {
      assertEquals(-1, reader.read());
    }
  }

  @Test
  public void testReaderClose() throws Exception {
    StringSequence seq = new StringSequence("A", "B", "C");
    try (Reader reader = seq.toReader()) {
      assertTrue(reader.ready());
      reader.close();
      assertThrows(IOException.class, () -> reader.ready());
    }
  }

  @Test
  public void testReaderRead() throws Exception {
    StringSequence seq = new StringSequence("", StringHolder.withSupplier(() -> ""), "A", "",
        StringHolder.withSupplier(() -> ""), StringHolder.withSupplierMinimumLength(1, () -> "B"),
        new StringSequence("CDE", "F", "GH"), StringHolder.withSupplier(() -> ""));
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

}
