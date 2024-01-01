/*
 * stringhold
 *
 * Copyright 2022, 2023 Christian Kohlschütter
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;

public class StringHolderTest {

  @Test
  public void testString() throws Exception {
    assertTrue(StringHolder.withContent("Some string").isString());
    assertTrue(StringHolder.withContent("").isString());
    assertTrue(StringHolder.withContent((Object) null).isString());
    assertTrue(StringHolder.withContent((Object[]) null).isString());
    assertTrue(StringHolder.withContent(new StringBuilder("foo")).isString());

    // StringSequences are not supplied as String immediately
    assertFalse(StringHolder.withContent(new StringHolderSequence()).isString());

    StringHolder sh = StringHolder.withContent(new StringHolderSequence().appendAll("Foo", "bar"));
    assertFalse(sh.isString());
    assertEquals("Foobar", sh.toString());
    assertTrue(sh.isString());
  }

  @Test
  public void testEquality() throws Exception {
    // StringHolder #equals works with String (but not vice versa)
    assertEquals(StringHolder.withContent("Some string"), "Some string");

    assertNotEquals("Some string", StringHolder.withContent("Some string"));
    assertEquals("Some string", StringHolder.withContent("Some string").toString());

    // StringHolder #equals works with other StringHolder implementations
    // estimated length doesn't matter since we will call #toString() when in doubt
    assertEquals(StringHolder.withContent("Test"), StringHolder.withSupplier(() -> "Test"));

    assertEquals(StringHolder.withContent("Test"), StringHolder.withContent(new StringBuilder(
        "Test")));
    assertEquals(StringHolder.withContent(new StringBuffer("Test")), StringHolder.withContent(
        new StringBuilder("Test")));

    assertEquals(StringHolder.withContent(123), "123");
    assertEquals(StringHolder.withContent(Float.NEGATIVE_INFINITY), "-Infinity");

    assertEquals(StringHolder.withContent(""), StringHolder.withContent(""));

    assertNotEquals(StringHolder.withContent(""), null);
    assertNotEquals(StringHolder.withContent((Object) null), null);
    assertNotEquals(StringHolder.withContent((Object[]) null), null);

    assertNotEquals(StringHolder.withContent(""), new StringBuilder());
    assertEquals(StringHolder.withContent(""), new StringBuilder().toString());

    // holder claims a minimum length of 5 -> not equal because we don't even check toString()
    assertNotEquals(StringHolder.withSupplierMinimumLength(5, () -> ""), "");

    // holder 1 claims minimum length of 5 -> not equal because we don't even check toString()
    // even if sh2 isString()
    {
      StringHolder sh1 = StringHolder.withSupplierMinimumLength(5, () -> "");
      StringHolder sh2 = StringHolder.withSupplierMinimumLength(0, () -> "");
      sh2.toString();
      assertNotEquals(sh1, sh2);
    }

    // holder claims minimum length of 5 -> error because we have to check both with toString()
    assertThrows(IllegalStateException.class, () -> assertEquals(StringHolder
        .withSupplierMinimumLength(5, () -> ""), StringHolder.withSupplier(() -> "")));

    // holder 1 claims minimum length of 5, but has checkError() == true
    // holder 2 claims minimum length of 5
    {
      StringHolder sh1 = StringHolder.withReaderSupplierMinimumLength(5, () -> {
        throw new IOException();
      }, (e) -> ExceptionResponse.EMPTY);
      StringHolder sh2 = StringHolder.withSupplierMinimumLength(5, () -> "12345");
      assertNotEquals(sh1, sh2);
      assertEquals(0, sh1.getMinimumLength());
      assertTrue(sh1.checkError());
    }

    // holder 1 claims minimum length of 5, but has checkError() == true
    {
      StringHolder sh1 = StringHolder.withReaderSupplierMinimumLength(5, () -> {
        throw new IOException();
      }, (e) -> ExceptionResponse.ILLEGAL_STATE);

      StringHolder sh2 = StringHolder.withSupplier(() -> "123");
      sh2.toString();

      // since holder 2 is a string with a shorter length than holder 1's minimum, we won't check
      // the contents of holder 1, and thus get no IllegalStateException
      assertNotEquals(sh1, sh2);
    }

    {
      // holder 1 is in unclear error state, but not String yet, claiming a minimum length of 10
      StringHolder brokenSh = new AbstractStringHolder(10) {
        {
          setError();
        }

        @Override
        protected String getString() {
          return "";
        }
      };

      assertEquals(10, brokenSh.getMinimumLength());
      StringHolder sh2 = StringHolder.withContent("sh2");

      // can't use optimized route, need to convert brokenSh to string
      assertNotEquals(brokenSh, sh2);

      // which reveals actual minimum length of 0
      assertEquals(0, brokenSh.getMinimumLength());
    }
  }

  @Test
  public void testIdentity() throws Exception {
    assertSame(StringHolder.withContent(""), StringHolder.withContent(""));
    assertSame(StringHolder.withContent((Object) null), StringHolder.withContent((Object) null));
    assertSame(StringHolder.withContent((Object[]) null), StringHolder.withContent(
        (Object[]) null));
    assertSame(StringHolder.withContent((Object) null), StringHolder.withContent((Object[]) null));

    StringHolder x = StringHolder.withContent("X");
    assertSame(StringHolder.withContent(x), x);

    // NOTE: This may be optimized in the future
    // assertNotSame(StringHolder.withContent("A"), StringHolder.withContent("A"));

    // empty CharSequences are optimized
    assertSame(StringHolder.withContent(""), StringHolder.withContent(new StringBuilder()));

    assertSame(StringHolder.withContent(""), StringHolder.withContent(
        new BrokenEmptyCharSequence()));
  }

  @Test
  public void testCachedCommonResults() throws Exception {
    // cached common result
    assertSame(StringHolder.withContent((Object) null), StringHolder.withContent("null"));
    assertSame(StringHolder.withContent((Object[]) null), StringHolder.withContent("null"));
    assertSame(StringHolder.withContent('\n'), StringHolder.withContent("\n"));
    assertSame(StringHolder.withContent(1), StringHolder.withContent("1"));
    assertSame(StringHolder.withContent(true), StringHolder.withContent("true"));

    // not a cached common result
    assertNotSame(StringHolder.withContent(1234569), StringHolder.withContent("1234569"));
  }

  @Test
  public void testEqualitySmart() throws Exception {
    StringHolder longer = StringHolder.withSupplierMinimumLength(5, () -> "longer");
    assertNotEquals(StringHolder.withContent("long"), longer);
    assertFalse(longer.isString()); // 5 is longer than 4, no string necessary
  }

  @Test
  public void testEqualitySmartButStill() throws Exception {
    StringHolder longer = StringHolder.withSupplierMinimumLength(4, () -> "longer");
    assertNotEquals(StringHolder.withContent("long"), longer);
    assertTrue(longer.isString()); // 4==4; we still had convert it to string
  }

  @Test
  public void testNull() throws Exception {
    assertEquals("null", StringHolder.withContent((Object) null).toString());
    assertEquals("null", StringHolder.withContent((Object[]) null).toString());
  }

  @Test
  public void testCharSequenceTimeOfConversion() throws Exception {
    TestCharSequence tcs = new TestCharSequence();
    assertFalse(tcs.accessed);

    StringHolder sh = StringHolder.withContent(tcs);
    // CharSequences are converted upon creating StringHolders, as their content may change
    assertTrue(tcs.accessed);

    assertEquals("AAA", sh.toString());
  }

  @Test
  public void testAppendTo() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withContent("defg");

    StringWriter out = new StringWriter();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    defg.appendTo(out);
    assertEquals(abc + defg.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testEmptyAppendToAndReturnLength() throws Exception {
    Appendable app = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        fail();
        return this;
      }
    };
    assertEquals(0, StringHolder.withSupplier(() -> "").appendToAndReturnLength(app));

    StringBuilder sb = new StringBuilder();
    assertEquals(0, StringHolder.withSupplier(() -> "").appendToAndReturnLength(sb));
    assertTrue(sb.isEmpty());

    StringBuffer sbuf = new StringBuffer();
    assertEquals(0, StringHolder.withSupplier(() -> "").appendToAndReturnLength(sbuf));
    assertTrue(sbuf.isEmpty());

    StringWriter sw = new StringWriter();
    assertEquals(0, StringHolder.withSupplier(() -> "").appendToAndReturnLength(sw));
    assertTrue(sw.toString().isEmpty());
  }

  @Test
  public void testEmptySequenceAppendToAndReturnLength() throws Exception {
    Appendable app = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        fail();
        return this;
      }
    };
    assertEquals(0, new StringHolderSequence().appendToAndReturnLength(app));

    StringBuilder sb = new StringBuilder();
    assertEquals(0, new StringHolderSequence().appendToAndReturnLength(sb));
    assertTrue(sb.isEmpty());

    StringBuffer sbuf = new StringBuffer();
    assertEquals(0, new StringHolderSequence().appendToAndReturnLength(sbuf));
    assertTrue(sbuf.isEmpty());

    StringWriter sw = new StringWriter();
    assertEquals(0, new StringHolderSequence().appendToAndReturnLength(sw));
    assertTrue(sw.toString().isEmpty());
  }

  @Test
  public void testCustomEmptyAppendToAndReturnLength() throws Exception {
    Appendable app = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(char c) throws IOException {
        fail();
        return this;
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        fail();
        return this;
      }
    };
    assertEquals(0, new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "";
      }
    }.appendToAndReturnLength(app));

    StringBuilder sb = new StringBuilder();
    assertEquals(0, new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "";
      }
    }.appendToAndReturnLength(sb));
    assertTrue(sb.isEmpty());

    StringBuffer sbuf = new StringBuffer();
    assertEquals(0, new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "";
      }
    }.appendToAndReturnLength(sbuf));
    assertTrue(sbuf.isEmpty());

    StringWriter sw = new StringWriter();
    assertEquals(0, new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "";
      }
    }.appendToAndReturnLength(sw));
    assertTrue(sw.toString().isEmpty());
  }

  @Test
  public void testNegativeLength() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> StringHolder.withSupplierMinimumLength(-1,
        () -> ""));
  }

  @Test
  public void testAppendToStringBuilder() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withContent("defg");

    StringBuilder out = new StringBuilder();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    defg.appendTo(out);
    assertEquals(abc + defg.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToStringBuffer() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringBuffer out = new StringBuffer();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    defg.appendTo(out);
    assertEquals(abc + defg.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthAppendable() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    Appendable out = new WrappedAppendable(new StringBuilder());
    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthStringBuilder() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringBuilder out = new StringBuilder();
    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthStringBuilder_defaultImpl() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringBuilder out = new StringBuilder();
    assertEquals(3, abc.appendToAndReturnLength((Appendable) out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthStringBuffer() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringBuffer out = new StringBuffer();
    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthStringBuffer_defaultImpl() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringBuffer out = new StringBuffer();
    assertEquals(3, abc.appendToAndReturnLength((Appendable) out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthWriter() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringWriter out = new StringWriter();
    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testAppendToAndReturnLengthWriter_defaultImpl() throws IOException {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder defg = StringHolder.withSupplier(() -> "defg");

    StringWriter out = new StringWriter();
    assertEquals(3, abc.appendToAndReturnLength((Appendable) out));
    assertEquals(abc.toString(), out.toString());

    assertEquals(4, defg.appendToAndReturnLength(out));
    assertEquals(abc + defg.toString(), out.toString());

    assertEquals(3, abc.appendToAndReturnLength(out));
    assertEquals("" + abc + defg + abc, out.toString());
  }

  @Test
  public void testMinimumLength() throws Exception {
    assertEquals(3, StringHolder.withContent(new TestCharSequence()).getMinimumLength());
    assertEquals(0, StringHolder.withContent(new BrokenEmptyCharSequence()).getMinimumLength());
  }

  @Test
  public void testReaderWithConstantString() throws Exception {
    StringWriter out = new StringWriter();
    StringHolder.withContent("foo").toReader().transferTo(out);
    assertEquals("foo", out.toString());
  }

  @Test
  public void testReaderWithStringSequence() throws Exception {
    StringWriter out = new StringWriter();
    StringHolder.withContent(new StringHolderSequence().appendAll("foo")).toReader().transferTo(
        out);
    assertEquals("foo", out.toString());
  }

  @Test
  public void testReaderWithSuppliedString() throws Exception {
    StringWriter out = new StringWriter();
    StringHolder.withSupplier(() -> "foo").toReader().transferTo(out);
    assertEquals("foo", out.toString());
  }

  @Test
  public void testHashMap() throws Exception {
    Map<Object, String> map = new HashMap<>();
    map.put("foo", "bar");
    map.put(StringHolder.withContent("abc"), "def");

    assertEquals("bar", map.get("foo"));
    assertEquals("bar", map.get(StringHolder.withContent("foo")));

    // String equality is not symmetric!
    assertEquals(null, map.get("abc"));
    assertEquals("def", map.get(StringHolder.withContent("abc")));
  }

  @Test
  public void testLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierMinimumLength(3, () -> "abcde");

    assertEquals(3, sh.getMinimumLength());
    assertEquals(5, sh.length());

    // the estimate is updated now that we know the correct length
    assertEquals(5, sh.getMinimumLength());

    // same for objects that are already String
    assertEquals(StringHolder.withContent("abcde").getMinimumLength(), StringHolder.withContent(
        "abcde").length());
  }

  private static class TestCharSequence implements CharSequence {
    boolean accessed = false;

    @Override
    public int length() {
      return 3;
    }

    @Override
    public char charAt(int index) {
      accessed = true;
      return 'A';
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
      accessed = true;
      return "AAA";
    }
  }

  /**
   * This {@link CharSequence} claims a length of 3, but when toString() is called, it is actually
   * empty.
   */
  private static final class BrokenEmptyCharSequence extends TestCharSequence {
    @Override
    public String toString() {
      super.toString();
      return "";
    }
  }

  @Test
  public void testIncreaseLength() throws Exception {
    StringHolder customHolder = new AbstractStringHolder(0, 8) {
      {
        resizeBy(1, -1);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    };
    assertEquals(1, customHolder.getMinimumLength());
    assertEquals(7, customHolder.getExpectedLength());

    assertEquals("hello", customHolder.toString());

    assertEquals(5, customHolder.getMinimumLength());
    assertEquals(5, customHolder.getExpectedLength());
  }

  @Test
  public void testIncreaseLengthNegativeValue() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> new AbstractStringHolder(6, 0) {
      {
        // using a negative increment for the minimum length is not allowed, unless checkError() is
        // true.
        resizeBy(-1, 0);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    });
  }

  @Test
  public void testIncreaseLengthNegativeValueTrouble() throws Exception {
    new AbstractStringHolder(6, 0) {
      {
        // using a negative increment for the minimum length is not allowed
        // unless checkError() is true.
        setError();
        resizeBy(-1, 0);
        assertEquals(5, getMinimumLength());
        resizeBy(-1000, 0);
        assertEquals(0, getMinimumLength());
      }

      @Override
      protected String getString() {
        return "hello";
      }
    };
  }

  @Test
  public void testUpdateLengthLesserValue() throws Exception {
    assertThrows(IllegalStateException.class, () -> new AbstractStringHolder(6, 0) {
      {
        // using a value smaller than the current minimum is not allowed, unless checkError() is
        // true
        resizeTo(3, 0);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    });
  }

  @Test
  public void testUpdateLengthLesserValueTrouble() throws Exception {
    new AbstractStringHolder(6, 0) {
      {
        //
        assertEquals(6, getMinimumLength());
        assertEquals(getMinimumLength(), getExpectedLength());

        // using a value smaller than the current minimum is not allowed
        // unless checkError() is true
        setError();
        resizeTo(3, 0);
        assertEquals(3, getMinimumLength());
        assertEquals(3, getExpectedLength());
      }

      @Override
      protected String getString() {
        return "hello";
      }
    };
  }

  @Test
  public void testUpdateLengthLesserValueTroubleCleared() throws Exception {
    assertThrows(IllegalStateException.class, () -> new AbstractStringHolder(6, 0) {
      {
        // using a value smaller than the current minimum is not allowed
        // unless checkError() is true
        setError();
        resizeTo(3, 0);

        clearError();
        // not allowed, since checkError is false again
        resizeTo(2, 0);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    });
  }

  @Test
  public void testMinLengthOverflow() throws Exception {
    StringHolder sh = StringHolder.withSupplierMinimumLength(Integer.MAX_VALUE - 1, () -> "");
    assertEquals(Integer.MAX_VALUE - 1, sh.getMinimumLength());

    ((AbstractStringHolder) sh).resizeBy(1, 0);
    assertEquals(Integer.MAX_VALUE, sh.getMinimumLength());

    // we increase beyond Integer.MAX_VALUE, and don't overflow
    ((AbstractStringHolder) sh).resizeBy(1, 0);
    assertEquals(Integer.MAX_VALUE, sh.getMinimumLength());
  }

  private static final class BrokenStringHolder extends AbstractStringHolder {
    protected BrokenStringHolder() {
      super(0);
    }

    void triggerError() {
      setError();
      theString = "error";
    }

    @Override
    protected String getString() {
      fail("Unexpected");
      return "";
    }
  }

  @Test
  public void testKnownEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> "");
    // we don't know yet if the string is empty
    assertFalse(sh.isKnownEmpty());
    sh.length();
    // we've asked for the length, so now we know it's empty
    assertTrue(sh.isKnownEmpty());

    // we know it's empty because it's a fixed string
    assertTrue(StringHolder.withContent("").isKnownEmpty());

    StringHolder sh2 = StringHolder.withSupplier(() -> "not empty");
    assertEquals(0, sh2.getMinimumLength());
    assertFalse(sh2.isKnownEmpty());
    sh2.toString();
    // we've asked for the length, so now we know for sure it's NOT empty
    assertFalse(sh2.isKnownEmpty());

    // a broken implementation sets "theString" directly without updating "minLength"
    BrokenStringHolder shBroken = new BrokenStringHolder();
    assertFalse(shBroken.isKnownEmpty());
    shBroken.triggerError();
    assertTrue(shBroken.isKnownEmpty());
    shBroken.length();
    assertTrue(shBroken.isKnownEmpty());
  }

  @Test
  public void testIsEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> "");
    assertFalse(sh.isKnownEmpty());
    assertFalse(sh.isString());
    assertTrue(sh.isEmpty()); // triggers string conversion
    assertTrue(sh.isString());
    assertTrue(sh.isKnownEmpty());
  }

  @Test
  public void testIsNotEmpty() throws Exception {
    StringHolder sh = StringHolder.withSupplier(() -> "not empty");
    assertFalse(sh.isKnownEmpty());
    assertFalse(sh.isString());
    assertFalse(sh.isEmpty()); // triggers string conversion
    assertTrue(sh.isString());
    assertFalse(sh.isKnownEmpty());
  }

  @Test
  public void testIsEmptyWhenKnownEmpty() throws Exception {
    StringHolder sh = new AbstractStringHolder(0) {

      @Override
      protected int computeLength() {
        return 0;
      }

      @Override
      public boolean isLengthKnown() {
        return true;
      }

      @Override
      protected String getString() {
        return "";
      }
    };
    assertTrue(sh.isKnownEmpty());
    assertFalse(sh.isString());
    assertTrue(sh.isEmpty()); // won't trigger string conversion
    assertFalse(sh.isString());
    assertTrue(sh.isKnownEmpty());
  }

  @Test
  public void testKnownEmpty_customZero() throws Exception {
    assertFalse(new AbstractStringHolder(0) {

      @Override
      protected String getString() {
        setError();
        return "error";
      }
    }.isKnownEmpty());
  }

  @Test
  public void testKnownEmpty_customNonZero() throws Exception {
    StringHolder sh = new AbstractStringHolder(0) {

      @Override
      protected String getString() {
        return "";
      }
    };
    assertFalse(sh.isKnownEmpty());
    sh.toString();
    assertTrue(sh.isKnownEmpty());
  }

  @Test
  public void testKnownEmpty_custom_broken() throws Exception {
    StringHolder sh = new AbstractStringHolder(0) {

      @Override
      protected int computeLength() {
        return 5;
      }

      @Override
      protected String getString() {
        return "";
      }
    };

    // sh.length() // <-- since we don't call length, the error doesn't matter
    assertFalse(sh.isKnownEmpty());
    sh.toString();
    assertTrue(sh.isKnownEmpty());
    assertFalse(sh.checkError());
  }

  @Test
  public void testKnownEmpty_custom_broken_computeLength() throws Exception {
    StringHolder sh = new AbstractStringHolder(0, 5) {

      @Override
      protected int computeLength() {
        return 5;
      }

      @Override
      protected String getString() {
        return "";
      }
    };

    assertEquals(0, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
    assertEquals(5, sh.length());
    assertFalse(sh.isKnownEmpty());

    // triggers a "Detected mispredicted minLength" error due to sh.length()'s returned minLength=5
    assertThrows(IllegalStateException.class, () -> sh.toString());
    assertEquals(0, sh.getMinimumLength());
    assertEquals(0, sh.getExpectedLength());
    assertEquals("", sh.toString());
    assertTrue(sh.isKnownEmpty());
  }

  @Test
  public void testKnownEmpty_custom_setTheStringDirectly() throws Exception {
    StringHolder customSh = new AbstractStringHolder(0) {
      {
        theString = "foo";
      }

      @Override
      public boolean isLengthKnown() {
        return false;
      }

      @Override
      protected String getString() {
        fail("Should not be reached");
        return theString;
      }
    };

    assertFalse(customSh.isLengthKnown());
    assertFalse(customSh.isKnownEmpty());
  }

  @Test
  public void testKnownEmpty_customEmpty_setTheStringDirectly() throws Exception {
    StringHolder customSh = new AbstractStringHolder(0) {
      {
        theString = "";
      }

      @Override
      public boolean isLengthKnown() {
        return false;
      }

      @Override
      protected String getString() {
        fail("Should not be reached");
        return theString;
      }
    };

    assertFalse(customSh.isLengthKnown());
    assertTrue(customSh.isKnownEmpty());
  }

  @Test
  public void testLengthKnown() throws Exception {
    assertTrue(StringHolder.withContent("yo").isLengthKnown());

    {
      StringHolder sh = StringHolder.withSupplier(() -> "yo");
      assertFalse(sh.isLengthKnown());
      sh.length();
      assertTrue(sh.isLengthKnown());
      assertTrue(sh.isString());
    }

    {
      StringHolder sh = new AbstractStringHolder(4) {
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
          return "test";
        }
      };
      assertTrue(sh.isLengthKnown());
      sh.length();
      assertTrue(sh.isLengthKnown());
      assertFalse(sh.isString());
      sh.toString();
      assertTrue(sh.isLengthKnown());
      assertTrue(sh.isString());
    }
  }

  @Test
  public void testLengthKnown_customZero() throws Exception {
    StringHolder customSh = new AbstractStringHolder(0) {
      @Override
      protected int computeLength() {
        return 0;
      }

      @Override
      public boolean isLengthKnown() {
        return true;
      }

      @Override
      protected String getString() {
        return "";
      }
    };

    assertTrue(customSh.isLengthKnown());
    assertTrue(customSh.isKnownEmpty());
  }

  @Test
  public void testLengthKnown_custom_broken() throws Exception {
    StringHolder customSh = new AbstractStringHolder(0) {
      @Override
      protected int computeLength() {
        return 5;
      }

      @Override
      public boolean isLengthKnown() {
        return true;
      }

      @Override
      protected String getString() {
        return "foo";
      }
    };

    assertTrue(customSh.isLengthKnown());
    assertFalse(customSh.isKnownEmpty());
  }

  @Test
  public void testCharAt() throws Exception {
    StringHolder sh = StringHolder.withContent("abc");
    assertEquals('c', sh.charAt(2));
    assertEquals('b', sh.charAt(1));
    assertEquals('a', sh.charAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> sh.charAt(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> sh.charAt(3));
  }

  @Test
  public void testSubSequence() throws Exception {
    StringHolder sh = StringHolder.withContent("abcdef");
    assertEquals("cde", sh.subSequence(2, 5));
    assertEquals("", sh.subSequence(4, 4));

    // we can return the same instance in this case
    assertTrue(sh.isString());
    assertSame(sh, sh.subSequence(0, 6));
    assertTrue(sh.isString());

    StringHolder sh4 = StringHolder.withSupplierFixedLength(6, () -> "abcdef");
    assertFalse(sh4.isString());
    assertNotEquals(sh4, sh4.subSequence(0, 5)); // one character too short
    assertTrue(sh4.isString());
    assertEquals("abcdef", sh4.toString());

    StringHolder sh2 = StringHolder.withSupplier(() -> "abcdef");
    assertFalse(sh2.isString());
    assertSame(sh2, sh2.subSequence(0, 6)); // the same, but converted to string
    assertTrue(sh2.isString());

    StringHolder sh3 = StringHolder.withSupplierFixedLength(6, () -> "abcdef");
    assertFalse(sh3.isString());
    assertSame(sh3, sh3.subSequence(0, 6)); // the same, not converted to string
    assertFalse(sh3.isString());
  }

  @Test
  public void testSupplierFixedLengthZero() throws Exception {
    // optimization: If a supplier has a fixed length of zero, we can supply an empty string
    assertSame(StringHolder.withContent(""), StringHolder.withSupplierFixedLength(0, () -> {
      fail("Should not be reachable");
      return "";
    }));
  }

  @Test
  public void testSupplierFixedLength() throws Exception {
    StringHolder shStr = StringHolder.withContent("1");
    StringHolder shFl = StringHolder.withSupplierFixedLength(1, () -> "1");
    assertNotSame(shStr, shFl);
    assertTrue(shFl.isLengthKnown());
    assertEquals(shStr.toString(), shFl.toString());
  }

  @Test
  public void testBrokenSupplierFixedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierFixedLength(1, () -> "123");
    assertThrows(IllegalStateException.class, () -> sh.toString());
    assertTrue(sh.checkError()); // checkError is set
    assertEquals("123", sh.toString()); // second time works since we got a string after all
    assertEquals(3, sh.getMinimumLength());
  }

  @Test
  public void testSetExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withContent("test");
    assertEquals(4, sh.getExpectedLength());
    sh.setExpectedLength(10); // ignored because we know it's a string
    assertEquals(4, sh.getExpectedLength());
  }

  @Test
  public void testAsContent() throws Exception {
    StringHolder sh;
    Object content;

    sh = StringHolder.withContent("test");
    content = sh.asContent();
    assertInstanceOf(String.class, content);
    assertEquals("test", content);

    sh = new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "test";
      }
    };
    content = sh.asContent();
    assertInstanceOf(StringHolder.class, content);
    assertEquals("test", content.toString());
  }

  @Test
  public void testComputeLength() throws Exception {
    StringHolder sh = new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "test";
      }
    };
    assertFalse(sh.isString());
    assertEquals(4, sh.length());
    assertTrue(sh.isString());
  }

  @Test
  public void testWithContent() throws Exception {
    assertEquals(StringHolder.withContent("Hello", ' ', "World"), "Hello World");
    assertEquals(StringHolder.withContent("Hello", StringHolder.withContent(" "), "World"),
        "Hello World");
    assertEquals(StringHolder.withContent("Hello", StringHolder.withSupplier(() -> " "), "World"),
        "Hello World");
    assertEquals(StringHolder.withContent("Hello", StringHolder.withSupplierFixedLength(1,
        () -> " "), "World"), "Hello World");
    assertEquals(StringHolder.withContent("Hello", StringHolder.withSupplierFixedLength(1,
        () -> " "), new StringHolderSequence(), "World"), "Hello World");
    assertEquals(StringHolder.withContent("Hello", StringHolder.withSupplierFixedLength(1,
        () -> " "), new StringHolderSequence().append(new AbstractStringHolder() {

          @Override
          protected String getString() {
            return "";
          }
        }), "World"), "Hello World");
  }

  @Test
  public void testWithContentArrayEmpty() throws Exception {
    assertSame(StringHolder.withContent(""), StringHolder.withContent());
    assertSame(StringHolder.withContent(""), StringHolder.withContent(new Object[] {}));
    assertSame(StringHolder.withContent(""), StringHolder.withContent(new Object[] {""}));
    assertSame(StringHolder.withContent(""), StringHolder.withContent("", ""));
    assertSame(StringHolder.withContent(""), StringHolder.withContent("", StringHolder.withContent(
        ""), ""));
  }

  @Test
  public void testWithContentArray() throws Exception {
    assertEquals(StringHolder.withContent("Hello", ' ', new StringBuilder("World")), "Hello World");
  }

  @Test
  public void testEqualsStringUnknownLength() throws Exception {
    assertEquals(StringHolder.withSupplier(() -> "abc"), "abc");
    assertNotEquals(StringHolder.withSupplier(() -> "abcd"), "abc");
    assertNotEquals(StringHolder.withSupplier(() -> "abc"), "abcd");

    assertNotEquals(StringHolder.withContent("abc"), StringHolder.withSupplierMinimumLength(4,
        () -> "abcd"));
    assertEquals(StringHolder.withContent("abc"), StringHolder.withSupplierMinimumLength(2,
        () -> "abc"));
  }

  @Test
  public void testEqualsStringKnownLength() throws Exception {
    assertEquals(StringHolder.withSupplierFixedLength(3, () -> "abc"), "abc");
    assertNotEquals(StringHolder.withSupplierFixedLength(4, () -> "abcd"), "abc");
    assertNotEquals(StringHolder.withSupplierFixedLength(3, () -> "abc"), "abcd");

    assertNotEquals(StringHolder.withSupplierFixedLength(3, () -> "abc"), StringHolder
        .withSupplierFixedLength(4, () -> "abcd"));
    assertNotEquals(StringHolder.withSupplierFixedLength(3, () -> "abc"), StringHolder.withContent(
        "abcd"));
    assertNotEquals(StringHolder.withContent("abc"), StringHolder.withSupplierMinimumLength(4,
        () -> "abcd"));
    assertNotEquals(StringHolder.withSupplierFixedLength(3, () -> "abc"), StringHolder
        .withSupplierMinimumLength(4, () -> "abcd"));
  }

  private static StringHolder troubleHolder(int minLen) {
    StringHolder sh = StringHolder.withSupplierMinimumLength(minLen, () -> "");
    assertThrows(IllegalStateException.class, sh::toString);
    assertTrue(sh.checkError());
    assertTrue(sh.isString());
    return sh;
  }

  @Test
  public void testEqualsStringCheckError() throws Exception {
    StringHolder sh = troubleHolder(1);
    assertEquals(sh, StringHolder.withSupplier(() -> ""));
    assertNotEquals(sh, StringHolder.withSupplier(() -> "abc"));
    assertNotEquals(sh, StringHolder.withSupplierMinimumLength(3, () -> "abc"));
    assertNotEquals(StringHolder.withContent("abc"), sh);
    assertNotEquals(StringHolder.withSupplierMinimumLength(3, () -> "abc"), sh);
    assertNotEquals(StringHolder.withSupplier(() -> "abc"), sh);
    assertEquals(sh, troubleHolder(1));
    assertEquals(sh, troubleHolder(2));
  }

  @Test
  public void testUpdateHashCode() throws Exception {
    assertEquals("Hello World".hashCode(), new AbstractStringHolder(0) {

      @Override
      protected String getString() {
        return " World";
      }

    }.updateHashCode("Hello".hashCode()));

    assertEquals("Hello World".hashCode(), new AbstractStringHolder(0) {

      @Override
      protected String getString() {
        return "Hello World";
      }
    }.updateHashCode(0));

    assertEquals("Hello World".hashCode(), ((AbstractStringHolder) StringHolder
        .withSupplierFixedLength("Hello World".length(), () -> "Hello World")).updateHashCode(0));
  }

  @Test
  public void testMarkImmutable() throws Exception {
    StringHolder sh = new AbstractStringHolder() {

      @Override
      protected String getString() {
        return "Hello World";
      }
    };
    assertFalse(sh.isEffectivelyImmutable());
    sh.markEffectivelyImmutable();
    assertTrue(sh.isEffectivelyImmutable());
  }

  private static StringHolder emptyStringHolderButNotAString(boolean lengthKnown) {
    return new AbstractStringHolder() {

      @Override
      public boolean isLengthKnown() {
        return lengthKnown;
      }

      @Override
      protected int computeLength() {
        return 0;
      }

      @Override
      protected String getString() {
        return "";
      }
    };
  }

  @Test
  public void testIndexOfChar() throws Exception {
    assertEquals(0, StringHolder.withContent("Foo bar").indexOf('F'));
    assertEquals(3, StringHolder.withContent("Foo bar").indexOf(' '));
    assertEquals(6, StringHolder.withContent("Foo bar").indexOf('r'));
    assertEquals(-1, StringHolder.withContent("Foo bar").indexOf('f'));
    assertEquals(0, StringHolder.withContent("F").indexOf('F'));
    assertEquals(0, StringHolder.withSupplier(() -> "F").indexOf('F'));

    assertEquals(-1, StringHolder.withContent("").indexOf(' '));
    assertEquals(-1, StringHolder.withContent(new StringBuilder()).indexOf(' '));
    assertEquals(-1, StringHolder.withSupplierFixedLength(0, () -> "").indexOf(' '));
    assertEquals(-1, emptyStringHolderButNotAString(false).indexOf(' '));
    assertEquals(-1, emptyStringHolderButNotAString(true).indexOf(' '));

    assertEquals(3, StringHolder.withSupplier(() -> "Foo bar").indexOf(' '));
    assertEquals(3, StringHolder.newSequence().append(StringHolder.withContent("Foo")).append(' ')
        .append(StringHolder.withSupplier(() -> "bar")).indexOf(' '));
  }

  @Test
  public void testIndexOfCharSequenceSingleChar() throws Exception {
    assertEquals(0, StringHolder.withContent("Foo bar").indexOf("F"));
    assertEquals(3, StringHolder.withContent("Foo bar").indexOf(" "));
    assertEquals(6, StringHolder.withContent("Foo bar").indexOf("r"));
    assertEquals(-1, StringHolder.withContent("Foo bar").indexOf("f"));
    assertEquals(0, StringHolder.withContent("F").indexOf("F"));
    assertEquals(0, StringHolder.withSupplier(() -> "F").indexOf("F"));

    assertEquals(-1, StringHolder.withContent("").indexOf(" "));
    assertEquals(-1, StringHolder.withContent(new StringBuilder()).indexOf(" "));
    assertEquals(-1, StringHolder.withSupplierFixedLength(0, () -> "").indexOf(" "));
    assertEquals(-1, emptyStringHolderButNotAString(false).indexOf(" "));
    assertEquals(-1, emptyStringHolderButNotAString(true).indexOf(" "));

    assertEquals(3, StringHolder.withSupplier(() -> "Foo bar").indexOf(" "));
    assertEquals(3, StringHolder.newSequence().append(StringHolder.withContent("Foo")).append(" ")
        .append(StringHolder.withSupplier(() -> "bar")).indexOf(" "));
  }

  private static CharSequence newCustomEmptyCharSequence() {
    return new CharSequence() {

      @Override
      public CharSequence subSequence(int start, int end) {
        return this;
      }

      @Override
      public int length() {
        return 0;
      }

      @Override
      public char charAt(int index) {
        return (char) -1;
      }
    };
  }

  @Test
  public void testIndexOfEmptyCharSequence() throws Exception {
    assertEquals(0, "".indexOf(""));
    assertEquals(0, "".indexOf("", 1));
    assertEquals(0, "".indexOf("", 2));
    assertEquals(0, "Foo bar".indexOf(""));
    assertEquals(1, "Foo bar".indexOf("", 1));

    assertEquals(0, StringHolder.withContent("Foo bar").indexOf(""));
    assertEquals(1, StringHolder.withContent("Foo bar").indexOf("", 1));
    assertEquals(0, StringHolder.withContent("Foo bar").indexOf(newCustomEmptyCharSequence()));
    assertEquals(1, StringHolder.withContent("Foo bar").indexOf(newCustomEmptyCharSequence(), 1));

    assertEquals(0, StringHolder.withContent("").indexOf(""));
    assertEquals(0, StringHolder.withContent("").indexOf("", 1));
    assertEquals(0, StringHolder.withContent(new StringBuilder()).indexOf(""));
    assertEquals(0, StringHolder.withSupplierFixedLength(0, () -> "").indexOf(""));
    assertEquals(0, emptyStringHolderButNotAString(false).indexOf(""));
    assertEquals(0, emptyStringHolderButNotAString(true).indexOf(""));
    assertEquals(0, emptyStringHolderButNotAString(false).indexOf("", 1));
    assertEquals(0, emptyStringHolderButNotAString(true).indexOf("", 1));

    assertEquals(0, StringHolder.newSequence().indexOf(""));
    assertEquals(0, StringHolder.newSequence().append(StringHolder.withContent("")).indexOf(""));
  }

  @Test
  public void testIndexOfMissing() throws Exception {
    assertEquals(-1, "".indexOf("bar Foo"));
    assertEquals(-1, "Foo bar".indexOf("bar Foo"));

    assertEquals(-1, StringHolder.withContent("Foo bar").indexOf("bar Foo"));
    assertEquals(-1, StringHolder.withContent("Foo bar").indexOf(new StringBuilder("bar Foo")));

    assertEquals(-1, StringHolder.withContent("Foo bar").indexOf("bar Foo"));
    assertEquals(-1, StringHolder.withContent(new StringBuilder("Foo bar")).indexOf("bar Foo"));
    assertEquals(-1, StringHolder.withSupplierFixedLength(0, () -> "Foo bar").indexOf("bar Foo"));
    assertEquals(-1, StringHolder.withSupplierFixedLength(3, () -> "Foo").indexOf("bar Foo"));
    assertEquals(-1, StringHolder.withSupplierFixedLength(3, () -> "Foo").indexOf(StringHolder
        .withSupplierFixedLength(7, () -> "bar Foo")));

    assertEquals(-1, StringHolder.newSequence().append("Foo bar").indexOf("bar Foo"));
    assertEquals(-1, StringHolder.newSequence().append(StringHolder.withContent("Foo")).append(" ")
        .append(StringHolder.withSupplier(() -> "bar")).indexOf("bar Foo"));
  }

  @Test
  public void testIndexOfFound() throws Exception {
    assertEquals(4, "Foo bar".indexOf("bar"));

    assertEquals(4, StringHolder.withContent("Foo bar").indexOf("bar"));
    assertEquals(4, StringHolder.withContent("Foo bar").indexOf(new StringBuilder("bar")));
    assertEquals(4, StringHolder.withContent("Foo bar").indexOf(StringHolder
        .withSupplierFixedLength(3, () -> "bar")));

    StringHolder sh = StringHolder.withContent("Foo bar");
    sh.toString();
    assertEquals(4, sh.indexOf("bar"));
    assertEquals(4, sh.indexOf(StringHolder.withContent("bar")));

    assertEquals(4, StringHolder.withContent("Foo bar").indexOf("bar"));
    assertEquals(4, StringHolder.withContent(new StringBuilder("Foo bar")).indexOf("bar"));

    assertEquals(4, StringHolder.withSupplierFixedLength(7, () -> "Foo bar").indexOf("bar"));
    assertEquals(-1, StringHolder.withSupplierFixedLength(0, () -> "Foo bar").indexOf("bar"));

    assertEquals(4, StringHolder.newSequence().append("Foo bar").indexOf("bar"));
    assertEquals(4, StringHolder.newSequence().append(StringHolder.withContent("Foo")).append(" ")
        .append(StringHolder.withSupplier(() -> "bar")).indexOf("bar"));
  }

  @Test
  public void testIndexOfSelf() throws Exception {
    StringHolder sh;

    sh = StringHolder.withContent("Foo bar");
    assertEquals(0, sh.indexOf(sh));
    assertEquals(-1, sh.indexOf(sh, 1));

    sh = StringHolder.withContent("");
    assertEquals(0, sh.indexOf(sh));
    assertEquals(0, sh.indexOf(sh, 1)); // note that behavior is different for empty strings

    sh = StringHolder.withSupplier(() -> "");
    assertEquals(0, sh.indexOf(sh));
  }

  @Test
  public void testContains() throws Exception {
    assertTrue(StringHolder.withContent("Foo bar").contains("bar"));
    assertFalse(StringHolder.withContent("Foo bar").contains("baz"));
  }

  @Test
  public void testIndexOfPartialMatch() {
    assertEquals(4, StringHolder.withContent("Foo Fobar").indexOf(new StringBuilder("Fobar")));
  }

  @Test
  public void testIndexOfSurrogatePairNative() throws Exception {
    int cat = 0x1f408;
    int catHigh = Character.highSurrogate(cat);
    int catLow = Character.lowSurrogate(cat);
    String catStr = "\ud83d\udc08";
    String catStrInvalidReversed = "\udc08\ud83d";

    String testStr;

    testStr = "\ud83d\udc08";
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(0, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(0, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(1, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));

    testStr = "c\ud83d\udc08at";
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(1, testStr.indexOf(cat));
    assertEquals(1, testStr.indexOf(catHigh));
    assertEquals(2, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(1, testStr.indexOf(cat, 1));
    assertEquals(2, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(2, testStr.indexOf(catLow, 2));

    testStr = "\ud83d\ud83d\udc08at";
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(1, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(1, testStr.indexOf(catHigh, 1));
    assertEquals(2, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(1, testStr.indexOf(cat, 1));
    assertEquals(2, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(2, testStr.indexOf(catLow, 2));

    testStr = "\ud83dX\ud83d";
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(-1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(-1, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(2, testStr.indexOf(catHigh, 1));
    assertEquals(-1, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(-1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(2, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));

    testStr = "\udc08\ud83d"; // wrong order of surrogate pair
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(-1, testStr.indexOf(catStr));
    assertEquals(0, testStr.indexOf(catStrInvalidReversed)); // 2 individual (albeit invalid) chars
    assertEquals(-1, testStr.indexOf(cat));
    assertEquals(1, testStr.indexOf(catHigh));
    assertEquals(0, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(-1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));
  }

  @Test
  public void testIndexOfSurrogatePair() throws Exception {
    int cat = 0x1f408;
    int catHigh = Character.highSurrogate(cat);
    int catLow = Character.lowSurrogate(cat);
    String catStr = "\ud83d\udc08";
    String catStrInvalidReversed = "\udc08\ud83d";

    StringHolder testStr;

    testStr = StringHolder.withSupplier(() -> "\ud83d\udc08");
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(0, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(0, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(1, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));

    testStr = StringHolder.withSupplier(() -> "c\ud83d\udc08at");
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(1, testStr.indexOf(cat));
    assertEquals(1, testStr.indexOf(catHigh));
    assertEquals(2, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(1, testStr.indexOf(cat, 1));
    assertEquals(2, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(2, testStr.indexOf(catLow, 2));

    testStr = StringHolder.withSupplier(() -> "\ud83d\ud83d\udc08at");
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(1, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(1, testStr.indexOf(catHigh, 1));
    assertEquals(2, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(1, testStr.indexOf(cat, 1));
    assertEquals(2, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(2, testStr.indexOf(catLow, 2));

    testStr = StringHolder.withSupplier(() -> "\ud83dX\ud83d");
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(-1, testStr.indexOf(catStr));
    assertEquals(-1, testStr.indexOf(catStrInvalidReversed));
    assertEquals(-1, testStr.indexOf(cat));
    assertEquals(0, testStr.indexOf(catHigh));
    assertEquals(2, testStr.indexOf(catHigh, 1));
    assertEquals(-1, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(-1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(2, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));

    testStr = StringHolder.withSupplier(() -> "\udc08\ud83d"); // wrong order of surrogate pair
    assertEquals(0, testStr.indexOf(testStr));
    assertEquals(-1, testStr.indexOf(catStr));
    assertEquals(0, testStr.indexOf(catStrInvalidReversed)); // 2 individual (albeit invalid) chars
    testStr = StringHolder.withSupplier(() -> "\udc08\ud83d"); // fresh instance
    assertEquals(0, testStr.indexOf(catStrInvalidReversed)); // 2 individual (albeit invalid) chars
    assertEquals(-1, testStr.indexOf(cat));
    assertEquals(1, testStr.indexOf(catHigh));
    assertEquals(0, testStr.indexOf(catLow));
    assertEquals(-1, testStr.indexOf(testStr, 1));
    assertEquals(-1, testStr.indexOf(cat, 1));
    assertEquals(-1, testStr.indexOf(catLow, 1));
    assertEquals(-1, testStr.indexOf(cat, 2));
    assertEquals(-1, testStr.indexOf(catHigh, 2));
    assertEquals(-1, testStr.indexOf(catLow, 2));
  }
}
