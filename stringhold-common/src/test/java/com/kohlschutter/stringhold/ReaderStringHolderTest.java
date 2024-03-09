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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;

public class ReaderStringHolderTest {
  @Test
  public void testReaderToString() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplier(() -> new StringReader("hello"), (
        e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);

    assertFalse(rsh.isString());
    assertEquals("hello", rsh.toString());
    assertTrue(rsh.isString());
  }

  @Test
  public void testMinimumLength() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(1, () -> new StringReader(
        "hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    assertEquals(1, rsh.getExpectedLength());
    assertEquals("hello", rsh.toString());
    assertEquals(5, rsh.getExpectedLength());
  }

  @Test
  public void testExpectedLength() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierExpectedLength(555, () -> new StringReader(
        "hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    assertEquals(555, rsh.getExpectedLength());
    assertEquals("hello", rsh.toString());
    assertEquals(5, rsh.getExpectedLength());
  }

  @Test
  public void testMinimumAndExpectedLength() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumAndExpectedLength(3, 555,
        () -> new StringReader("hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    assertEquals(3, rsh.getMinimumLength());
    assertEquals(555, rsh.getExpectedLength());
    assertEquals("hello", rsh.toString());
    assertEquals(5, rsh.getMinimumLength());
    assertEquals(5, rsh.getExpectedLength());
  }

  @Test
  public void testReaderToWriter() throws Exception {
    StringWriter out = new StringWriter();

    AtomicInteger supplies = new AtomicInteger(0);
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(0, () -> {
      supplies.incrementAndGet();
      return new StringReader("hello");
    }, (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);

    assertEquals(0, supplies.get());
    // we can write from Reader to Writer without materializing it as a String
    assertFalse(rsh.isString());
    rsh.appendTo(out);
    assertEquals(1, supplies.get());
    assertFalse(rsh.isString());
    assertEquals("hello", out.toString());
    assertFalse(rsh.isString());
    assertEquals(1, supplies.get());

    // since we can supply the ReaderStringHolder with new Reader instances from the same content,
    // we can write to the Writer again and again
    rsh.appendTo((Appendable) out);
    assertEquals("hellohello", out.toString());
    assertEquals(2, supplies.get());

    // ... or convert it to a String
    assertFalse(rsh.isString());
    assertEquals("hello", rsh.toString());
    assertTrue(rsh.isString());
    assertEquals(3, supplies.get());

    // append to an unknown Appendable wrapper around out for good measure
    rsh.appendTo(new WrappedAppendable(out));
  }

  @Test
  public void testCantAppend() throws Exception {
    Appendable cantAppendable = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        throw new IOException("Can't append");
      }

      @Override
      public Appendable append(char c) throws IOException {
        throw new IOException("Can't append");
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        throw new IOException("Can't append");
      }
    };

    assertThrows(IOException.class, () -> StringHolder.withReaderSupplierMinimumLength(5,
        () -> new StringReader("hello"), (e) -> IOExceptionHandler.ExceptionResponse.FLUSH)
        .appendTo(cantAppendable));

    assertThrows(IllegalStateException.class, () -> StringHolder.withReaderSupplierMinimumLength(5,
        () -> new StringReader("hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE)
        .appendTo(cantAppendable));
  }

  @Test
  public void testCantAppendAgain() throws Exception {
    Appendable cantAppendable = new Appendable() {

      int n = 0;

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        if (++n != 0) {
          throw new IOException("Can't append");
        }
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        if (++n != 0) {
          throw new IOException("Can't append");
        }
        return this;
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        if (++n != 0) {
          throw new IOException("Can't append");
        }
        return this;
      }
    };

    assertThrows(IOException.class, () -> StringHolder.withReaderSupplierMinimumLength(5,
        () -> new StringReader("hello"), (
            e) -> IOExceptionHandler.ExceptionResponse.EXCEPTION_MESSAGE).appendTo(cantAppendable));

    assertThrows(IOException.class, () -> StringHolder.withReaderSupplierMinimumLength(5,
        () -> new StringReader("hello"), (e) -> IOExceptionHandler.ExceptionResponse.STACKTRACE)
        .appendTo(cantAppendable));
  }

  @Test
  public void testReaderToStringBuilder() throws Exception {
    StringBuilder sb = new StringBuilder();
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(5, () -> new StringReader(
        "hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    rsh.appendTo(sb);
    assertEquals("hello", sb.toString());
  }

  @Test
  public void testReaderToStringBuffer() throws Exception {
    StringBuffer sb = new StringBuffer();
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(5, () -> new StringReader(
        "hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    rsh.appendTo(sb);
    assertEquals("hello", sb.toString());
  }

  @Test
  public void testReaderToOtherAppendable() throws Exception {
    StringBuilder sb = new StringBuilder();
    Appendable app = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        sb.append(csq, start, end);
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        sb.append(c);
        return this;
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        sb.append(csq);
        return this;
      }
    };
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(5, () -> new StringReader(
        "hello"), (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    rsh.appendTo(app);
    assertEquals("hello", sb.toString());
  }

  @Test
  public void testExceptionOnSupply_empty() throws Exception {
    // Note that we claim a minimum length of 123

    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> {
      throw new MyIOException("No reader");
    }, (e) -> IOExceptionHandler.ExceptionResponse.EMPTY);

    assertEquals(123, rsh.getMinimumLength());
    assertEquals("", rsh.toString());

    // error condition permits re-adjusting minimumLength
    assertTrue(rsh.checkError());
    assertEquals(0, rsh.getMinimumLength());
  }

  @Test
  public void testExceptionOnSupply_exceptionMessage() throws Exception {
    // Note that we claim a minimum length of 123

    IOException exc = new MyIOException("No reader");
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> {
      throw exc;
    }, (e) -> IOExceptionHandler.ExceptionResponse.EXCEPTION_MESSAGE);
    assertEquals(exc.toString(), rsh.toString());

    // error condition permits re-adjusting minimumLength
    assertTrue(rsh.checkError());
    assertEquals(rsh.toString().length(), rsh.getMinimumLength());
  }

  @Test
  public void testExceptionOnSupply_flush() throws Exception {
    // Note that we claim a minimum length of 123

    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> {
      throw new MyIOException("No reader");
    }, (e) -> IOExceptionHandler.ExceptionResponse.FLUSH);
    assertEquals("", rsh.toString());

    // error condition permits re-adjusting minimumLength
    assertTrue(rsh.checkError());
    assertEquals(0, rsh.getMinimumLength());
  }

  @Test
  public void testExceptionOnSupply_stacktrace() throws Exception {
    // Note that we claim a minimum length of 123

    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> {
      throw new MyIOException("No reader");
    }, (e) -> IOExceptionHandler.ExceptionResponse.STACKTRACE);
    assertNotEquals("", rsh.toString());
    assertTrue(rsh.toString().contains(MyIOException.class.getName()));
    assertTrue(rsh.toString().contains(ReaderStringHolderTest.class.getName()));

    // error condition permits re-adjusting minimumLength
    assertTrue(rsh.checkError());
    assertEquals(rsh.toString().length(), rsh.getMinimumLength());
  }

  @Test
  public void testExceptionOnSupply_illegalState() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> {
      throw new MyIOException("No reader");
    }, (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    try (Reader r = rsh.toReader()) {
      // lazy-initialized; should not throw an exception

      // if an exception is thrown in here, {@code false} is returned and the exception is swallowed
      assertFalse(r.markSupported());

      //
      assertThrows(MyIOException.class, () -> r.read());
    }
    assertThrows(IllegalStateException.class, () -> {
      rsh.toString();
    });

    assertThrows(IllegalStateException.class, () -> {
      rsh.appendTo(new StringBuilder());
    });
  }

  private static final class MyIOException extends IOException {
    private static final long serialVersionUID = 1L;

    MyIOException(String message) {
      super(message);
    }
  }

  @Test
  public void testExceptionOnAppend_flush() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> new Reader() {

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        throw new MyIOException("Error during read");
      }

      @Override
      public void close() throws IOException {
      }

    }, (e) -> IOExceptionHandler.ExceptionResponse.FLUSH);

    // first round: not a String
    // second round: a String
    for (int loop = 0; loop < 2; loop++) {
      {
        StringWriter sb = new StringWriter();

        if (loop == 0) {
          // the exception is thrown _and_ handled
          assertThrows(MyIOException.class, () -> rsh.appendTo(sb));
        } else {
          // no exception when it's a String
          rsh.appendTo(sb);

          // but make sure it's empty
          StringWriter sw2 = new StringWriter();
          rsh.appendTo(sw2);
          assertEquals("", sw2.toString());
        }

        assertEquals("", sb.toString());
      }
      {
        StringBuilder sb = new StringBuilder();
        rsh.appendTo(sb);

        assertEquals("", sb.toString());
      }
      {
        StringBuffer sb = new StringBuffer();
        rsh.appendTo(sb);

        assertEquals("", sb.toString());
      }
      if (loop == 0) {
        assertFalse(rsh.isString());
      }
      rsh.toString();
      assertTrue(rsh.isString());
    }
  }

  @Test
  public void testExceptionOnAppend_empty() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(123, () -> new Reader() {

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        throw new MyIOException("Error during read");
      }

      @Override
      public void close() throws IOException {
      }

    }, (e) -> IOExceptionHandler.ExceptionResponse.EMPTY);

    // first round: not a String
    // second round: a String
    for (int loop = 0; loop < 2; loop++) {
      {
        StringWriter sb = new StringWriter();

        if (loop == 0) {
          // the exception is thrown _and_ handled
          assertThrows(MyIOException.class, () -> rsh.appendTo(sb));
        } else {
          // no exception when it's a String
          rsh.appendTo(sb);

          // but make sure it's empty
          StringWriter sw2 = new StringWriter();
          rsh.appendTo(sw2);
          assertEquals("", sw2.toString());
        }

        assertEquals("", sb.toString());
      }
      {
        StringBuilder sb = new StringBuilder();
        rsh.appendTo(sb);

        assertEquals("", sb.toString());
      }
      {
        StringBuffer sb = new StringBuffer();
        rsh.appendTo(sb);

        assertEquals("", sb.toString());
      }
      if (loop == 0) {
        assertFalse(rsh.isString());
      }
      rsh.toString();
      assertTrue(rsh.isString());
    }
  }

  @Test
  public void testExceptionOnAppend_exceptionMessage() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(5, () -> new Reader() {

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        throw new EOFException("Error during read");
      }

      @Override
      public void close() throws IOException {
      }

    }, (e) -> IOExceptionHandler.ExceptionResponse.EXCEPTION_MESSAGE);

    // first round: not a String
    // second round: a String
    for (int loop = 0; loop < 2; loop++) {
      {
        StringWriter sb = new StringWriter();

        if (loop == 0) {
          // the exception is thrown _and_ handled
          assertThrows(EOFException.class, () -> rsh.appendTo(sb));
        } else {
          // no exception when it's a String
          rsh.appendTo(sb);

          // but make sure it contains the output desired by ExceptionResponse.EXCEPTION_MESSAGE
        }

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertFalse(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }
      {
        StringBuilder sb = new StringBuilder();
        rsh.appendTo(sb);

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertFalse(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }
      {
        StringBuffer sb = new StringBuffer();
        rsh.appendTo(sb);

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertFalse(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }
      if (loop == 0) {
        assertFalse(rsh.isString());
      }
      rsh.toString();
      assertTrue(rsh.isString());
    }
  }

  @Test
  public void testExceptionOnAppend_stacktrace() throws Exception {
    StringHolder rsh = StringHolder.withReaderSupplierMinimumLength(5, () -> new Reader() {

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        throw new EOFException("Error during read");
      }

      @Override
      public void close() throws IOException {
      }

    }, (e) -> IOExceptionHandler.ExceptionResponse.STACKTRACE);

    // first round: not a String
    // second round: a String
    for (int loop = 0; loop < 2; loop++) {
      {
        StringWriter sb = new StringWriter();

        if (loop == 0) {
          // the exception is thrown _and_ handled
          assertThrows(EOFException.class, () -> rsh.appendTo(sb));
        } else {
          // no exception when it's a String
          rsh.appendTo(sb);
        }

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertTrue(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }
      {
        StringBuilder sb = new StringBuilder();
        rsh.appendTo(sb);

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertTrue(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }
      {
        StringBuffer sb = new StringBuffer();
        rsh.appendTo(sb);

        assertNotEquals("", sb.toString());
        assertTrue(sb.toString().contains(EOFException.class.getName()));
        assertTrue(sb.toString().contains(ReaderStringHolderTest.class.getName()));
      }

      if (loop == 0) {
        assertFalse(rsh.isString());
      }
      rsh.toString();
      assertTrue(rsh.isString());
    }
  }

  @Test
  public void testEqualsWithTrouble() throws Exception {
    // holder initially claims a minimum length of 5
    StringHolder sh1 = StringHolder.withReaderSupplierMinimumLength(5, () -> {
      throw new IOException();
    }, (e) -> ExceptionResponse.EMPTY);

    assertEquals(5, sh1.getMinimumLength());
    // conversion via toString() fails, minimum length is adjusted
    sh1.toString();
    assertTrue(sh1.checkError());
    assertEquals(0, sh1.getMinimumLength());
    assertEquals(sh1, "");

    StringHolder sh2 = StringHolder.withSupplierMinimumLength(0, () -> "");
    assertEquals(sh2, sh1);

    ((AbstractStringHolder) sh1).resizeBy(3, -1000);
    assertEquals(3, sh1.getMinimumLength());
    assertEquals(3, sh1.getExpectedLength());
  }
}
