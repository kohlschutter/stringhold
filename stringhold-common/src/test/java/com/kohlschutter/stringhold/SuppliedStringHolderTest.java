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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

public class SuppliedStringHolderTest {
  @Test
  public void testEmpty() throws Exception {
    assertEquals(StringHolder.withContent(""), StringHolder.withSupplier(() -> ""));
  }

  @Test
  public void testSimple() throws Exception {
    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplier(() -> "x"));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(0,
        () -> "x"));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(1,
        () -> "x"));

    // we claim that the minimum length for the second value is 2, so this check fails
    assertNotEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(2,
        () -> "x"));
  }

  @Test
  public void testIOSimple() throws Exception {
    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplier(() -> "x", (
        e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(0, () -> "x",
        (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(1, () -> "x",
        (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE));

    // we claim that the minimum length for the second value is 2, so this check fails
    assertNotEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(2,
        () -> "x", (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE));
  }

  @Test
  public void testIOSupplierIOExceptionThrows() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new IOException();
    }, (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    assertThrows(IllegalStateException.class, () -> sh.toString());
  }

  @Test
  public void testIOSupplierRuntimeException() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new UnsupportedOperationException();
    }, (e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);
    assertThrows(UnsupportedOperationException.class, () -> sh.toString());
  }

  @Test
  public void testIOSupplierExceptionHandleEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new IOException();
    }, (e) -> IOExceptionHandler.ExceptionResponse.EMPTY);
    assertEquals("", sh.toString());
  }

  @Test
  public void testIOSupplierExceptionHandleFlush() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new IOException();
    }, (e) -> IOExceptionHandler.ExceptionResponse.FLUSH);
    assertEquals("", sh.toString());
  }

  @Test
  public void testIOSupplierExceptionHandleExceptionMessage() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new IOException("message");
    }, (e) -> IOExceptionHandler.ExceptionResponse.EXCEPTION_MESSAGE);
    assertEquals(new IOException("message").toString(), sh.toString());
  }

  @Test
  public void testIOSupplierExceptionHandleExceptionStackTrace() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> {
      throw new IOException();
    }, (e) -> IOExceptionHandler.ExceptionResponse.STACKTRACE);

    String out = sh.toString();
    assertTrue(out.startsWith(new IOException().toString()), out);
    assertTrue(out.contains(getClass().getName()), out);
  }

  @Test
  public void testNull() throws Exception {
    assertThrows(NullPointerException.class, () -> StringHolder.withSupplier(null));
  }

  @Test
  public void testBadMinimumLength() throws Exception {
    // it is not permissible to specify a minimum length that is larger than the actual size
    assertThrows(IllegalStateException.class, () -> StringHolder.withSupplierMinimumLength(234,
        () -> "abc").toString());
  }

  @Test
  public void testExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierExpectedLength(7, () -> "hello");
    assertEquals(0, sh.getMinimumLength());
    assertEquals(7, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testIOExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierExpectedLength(7, () -> "hello",
        IOExceptionHandler.ILLEGAL_STATE);
    assertEquals(0, sh.getMinimumLength());
    assertEquals(7, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testMinimumAndExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierMinimumAndExpectedLength(2, 7, () -> "hello");
    assertEquals(2, sh.getMinimumLength());
    assertEquals(7, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testIOMinimumAndExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierMinimumAndExpectedLength(2, 7, () -> "hello",
        IOExceptionHandler.ILLEGAL_STATE);
    assertEquals(2, sh.getMinimumLength());
    assertEquals(7, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testFixedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierFixedLength(5, () -> "hello");
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testIOFixedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierFixedLength(5, () -> "hello",
        IOExceptionHandler.ILLEGAL_STATE);
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }

  @Test
  public void testFixedLengthEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplierFixedLength(0, () -> fail("should not be reached"));
    assertEquals(0, sh.getMinimumLength());
    assertEquals(0, sh.getExpectedLength());
    sh.toString();
    assertEquals(0, sh.getMinimumLength());
    assertEquals(0, sh.getExpectedLength());
  }

  @Test
  public void testIOFixedLengthEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplierFixedLength(0, () -> fail("shout not be reached"),
        IOExceptionHandler.ILLEGAL_STATE);
    assertEquals(0, sh.getMinimumLength());
    assertEquals(0, sh.getExpectedLength());
    sh.toString();
    assertEquals(0, sh.getMinimumLength());
    assertEquals(0, sh.getExpectedLength());
  }

  @Test
  public void testAsContent() throws Exception {
    Object content;

    StringHolder sh = StringHolder.withSupplier(() -> "test");
    assertFalse(sh.isString());
    content = sh.asContent();
    assertFalse(sh.isString());
    assertInstanceOf(StringHolder.class, content);
    assertEquals("test", content.toString());

    // only after calling toString, asContent will return the string
    assertTrue(sh.isString());
    content = sh.asContent();
    assertTrue(sh.isString());
    assertInstanceOf(String.class, content);

    // once more for code coverage
    content = sh.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("test", content);

    sh = StringHolder.withSupplier(() -> StringHolder.withSupplier(() -> "test2"));
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertEquals("test2", content.toString());

    StringWriter out = new StringWriter();
    sh.appendTo(out);

    sh.toString();
    content = sh.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("test2", content);

    sh = StringHolder.withSupplier(() -> new WrappedCharSequence("test"));
    assertFalse(sh.isString());
    sh.appendTo(out);
    assertFalse(sh.isString());
    content = sh.asContent();

    sh = StringHolder.withSupplier(() -> 123);
    assertFalse(sh.isString());
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertFalse(sh.isString());
    // once more
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertFalse(sh.isString());

    sh = StringHolder.withSupplier(() -> StringHolder.withSupplier(() -> 123));
    assertFalse(sh.isString());
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertFalse(sh.isString());

    sh = StringHolder.withSupplier(() -> new CustomAppendStringHolder());
    assertFalse(sh.isString());
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertFalse(sh.isString());
    sh.appendTo(out);
    assertFalse(sh.isString());
    content = sh.asContent();
    assertInstanceOf(CustomAppendStringHolder.class, content);
  }

  private static final class CustomAppendStringHolder extends AbstractStringHolder {

    public CustomAppendStringHolder() {
      super();
    }

    @Override
    protected int computeLength() {
      return 4;
    }

    @Override
    public boolean isLengthKnown() {
      return true;
    }

    @Override
    protected String getString() {
      fail("Should not be reached");
      return "test";
    }

    @Override
    protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
      out.append("test");
      return 4;
    }
  }

  @Test
  public void testCharAt() throws Exception {
    StringHolder sh;

    sh = StringHolder.withSupplier(() -> "test");
    assertEquals('e', sh.charAt(1));

    sh = StringHolder.withSupplier(() -> StringHolder.withSupplier(() -> "test"));
    assertEquals('e', sh.charAt(1));

    sh = StringHolder.withSupplier(() -> 123);
    assertEquals('2', sh.charAt(1));

    sh = StringHolder.withSupplier(() -> null);
    assertEquals('u', sh.charAt(1));
  }

  @Test
  public void testAppend() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> "Hello");

    Appendable app = new WrappedAppendable(new StringBuilder());
    sh.appendTo(app);
    assertEquals("Hello", app.toString());

    StringBuilder sb = new StringBuilder();
    sh.appendTo(sb);
    assertEquals("Hello", sb.toString());

    StringBuffer sbuf = new StringBuffer();
    sh.appendTo(sbuf);
    assertEquals("Hello", sbuf.toString());

    StringWriter sw = new StringWriter();
    sh.appendTo(sw);
    assertEquals("Hello", sw.toString());

    sw = new StringWriter();
    sh.toReader().transferTo(sw);
    assertEquals("Hello", sw.toString());
  }

  @Test
  public void testAppendNonString() throws Exception {
    Appendable app = new WrappedAppendable(new StringBuilder());
    StringHolder.withSupplier(() -> 123).appendTo(app);
    assertEquals("123", app.toString());

    StringBuilder sb = new StringBuilder();
    StringHolder.withSupplier(() -> 123).appendTo(sb);
    assertEquals("123", sb.toString());

    StringBuffer sbuf = new StringBuffer();
    StringHolder.withSupplier(() -> 123).appendTo(sbuf);
    assertEquals("123", sbuf.toString());

    StringWriter sw = new StringWriter();
    StringHolder.withSupplier(() -> 123).appendTo(sw);
    assertEquals("123", sw.toString());

    sw = new StringWriter();
    StringHolder.withSupplier(() -> 123).toReader().transferTo(sw);
    assertEquals("123", sw.toString());
  }

  @Test
  public void testAppendNull() throws Exception {
    Appendable app = new WrappedAppendable(new StringBuilder());
    StringHolder.withSupplier(() -> null).appendTo(app);
    assertEquals("null", app.toString());

    StringBuilder sb = new StringBuilder();
    StringHolder.withSupplier(() -> null).appendTo(sb);
    assertEquals("null", sb.toString());

    StringBuffer sbuf = new StringBuffer();
    StringHolder.withSupplier(() -> null).appendTo(sbuf);
    assertEquals("null", sbuf.toString());

    StringWriter sw = new StringWriter();
    StringHolder.withSupplier(() -> null).appendTo(sw);
    assertEquals("null", sw.toString());

    sw = new StringWriter();
    StringHolder.withSupplier(() -> null).toReader().transferTo(sw);
    assertEquals("null", sw.toString());
  }

  @Test
  public void testAppendCustom() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> new AbstractStringHolder(0) {

      @Override
      protected String getString() {
        fail("should not be reached");
        return "";
      }

      @Override
      protected Reader newReader() throws IOException {
        return new StringReader("Hello");
      }

      @Override
      protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
        out.append("Hello");
        return 5;
      }

      @Override
      protected int appendToAndReturnLengthImpl(StringBuilder out) {
        out.append("Hello");
        return 5;
      }

      @Override
      protected int appendToAndReturnLengthImpl(StringBuffer out) {
        out.append("Hello");
        return 5;
      }

      @Override
      protected int appendToAndReturnLengthImpl(Writer out) throws IOException {
        out.write("Hello");
        return 5;
      }
    });

    Appendable app = new WrappedAppendable(new StringBuilder());
    sh.appendTo(app);
    assertEquals("Hello", app.toString());

    StringBuilder sb = new StringBuilder();
    sh.appendTo(sb);
    assertEquals("Hello", sb.toString());

    StringBuffer sbuf = new StringBuffer();
    sh.appendTo(sbuf);
    assertEquals("Hello", sbuf.toString());

    StringWriter sw = new StringWriter();
    sh.appendTo(sw);
    assertEquals("Hello", sw.toString());

    sw = new StringWriter();
    sh.toReader().transferTo(sw);
    assertEquals("Hello", sw.toString());
  }

  @Test
  public void testLength() throws Exception {
    assertEquals(3, StringHolder.withSupplier(() -> "len").length());
    assertEquals(3, StringHolder.withSupplier(() -> StringHolder.withSupplier(() -> "len"))
        .length());
    assertEquals(3, StringHolder.withSupplier(() -> StringHolder.withContent("len")).length());
    assertEquals(3, StringHolder.withSupplier(() -> 123).length());

    // String.valueOf(null).length() == 4
    assertEquals(4, StringHolder.withSupplier(() -> null).length());
  }

  @Test
  public void testLengthKnown() throws Exception {
    StringHolder sh;
    sh = StringHolder.withSupplier(() -> "abc");
    assertFalse(sh.isLengthKnown());
    sh.length();
    assertTrue(sh.isLengthKnown());

    sh = StringHolder.withSupplier(() -> new CustomAppendStringHolder());
    assertFalse(sh.isLengthKnown());
    StringWriter out = new StringWriter();
    sh.appendTo(out);
    assertTrue(sh.isLengthKnown());
    assertFalse(sh.isString());
  }
}
