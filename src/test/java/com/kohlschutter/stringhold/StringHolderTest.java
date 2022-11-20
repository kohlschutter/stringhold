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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    assertTrue(StringHolder.withContent(null).isString());
    assertTrue(StringHolder.withContent(new StringBuilder("foo")).isString());

    // StringSequences are not supplied as String immediately
    assertFalse(StringHolder.withContent(new StringSequence()).isString());

    StringHolder sh = StringHolder.withContent(new StringSequence("Foo", "bar"));
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
    assertNotEquals(StringHolder.withContent(null), null);

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
      StringHolder sh1 = ReaderStringHolder.withIOSupplierMinimumLength(5, () -> {
        throw new IOException();
      }, (e) -> ExceptionResponse.EMPTY);
      StringHolder sh2 = StringHolder.withSupplierMinimumLength(5, () -> "12345");
      assertNotEquals(sh1, sh2);
      assertEquals(0, sh1.getMinimumLength());
      assertTrue(sh1.checkError());
    }

    // holder 1 claims minimum length of 5, but has checkError() == true
    {
      StringHolder sh1 = ReaderStringHolder.withIOSupplierMinimumLength(5, () -> {
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
      StringHolder brokenSh = new StringHolder(10) {
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
    assertSame(StringHolder.withContent(null), StringHolder.withContent(""));

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
    assertEquals("", StringHolder.withContent(null).toString());
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
    StringHolder def = StringHolder.withContent("Def");

    StringWriter out = new StringWriter();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    def.appendTo(out);
    assertEquals(abc + def.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + def + abc, out.toString());
  }

  @Test
  public void testNegativeLength() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> StringHolder.withSupplierMinimumLength(-1,
        () -> ""));
  }

  @Test
  public void testAppendToStringBuilder() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder def = StringHolder.withContent("Def");

    StringBuilder out = new StringBuilder();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    def.appendTo(out);
    assertEquals(abc + def.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + def + abc, out.toString());
  }

  @Test
  public void testAppendToStringBuffer() {
    StringHolder abc = StringHolder.withContent("Abc");
    StringHolder def = StringHolder.withContent("Def");

    StringBuffer out = new StringBuffer();
    abc.appendTo(out);
    assertEquals(abc.toString(), out.toString());

    def.appendTo(out);
    assertEquals(abc + def.toString(), out.toString());

    abc.appendTo(out);
    assertEquals("" + abc + def + abc, out.toString());
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
    StringHolder.withContent(new StringSequence("foo")).toReader().transferTo(out);
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
    StringHolder customHolder = new StringHolder(0, 8) {
      {
        increaseLengths(1, -1);
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
    assertThrows(IllegalArgumentException.class, () -> new StringHolder(6, 0) {
      {
        // using a negative increment for the minimum length is not allowed, unless checkError() is
        // true.
        increaseLengths(-1, 0);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    });
  }

  @Test
  public void testIncreaseLengthNegativeValueTrouble() throws Exception {
    new StringHolder(6, 0) {
      {
        // using a negative increment for the minimum length is not allowed
        // unless checkError() is true.
        setError();
        increaseLengths(-1, 0);
        assertEquals(5, getMinimumLength());
        increaseLengths(-1000, 0);
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
    assertThrows(IllegalStateException.class, () -> new StringHolder(6, 0) {
      {
        // using a value smaller than the current minimum is not allowed, unless checkError() is
        // true
        updateLengths(3, 0);
      }

      @Override
      protected String getString() {
        return "hello";
      }
    });
  }

  @Test
  public void testUpdateLengthLesserValueTrouble() throws Exception {
    new StringHolder(6, 0) {
      {
        //
        assertEquals(6, getMinimumLength());
        assertEquals(getMinimumLength(), getExpectedLength());

        // using a value smaller than the current minimum is not allowed
        // unless checkError() is true
        setError();
        updateLengths(3, 0);
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
    assertThrows(IllegalStateException.class, () -> new StringHolder(6, 0) {
      {
        // using a value smaller than the current minimum is not allowed
        // unless checkError() is true
        setError();
        updateLengths(3, 0);

        clearError();
        // not allowed, since checkError is false again
        updateLengths(2, 0);
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

    sh.increaseLengths(1, 0);
    assertEquals(Integer.MAX_VALUE, sh.getMinimumLength());

    // we increase beyond Integer.MAX_VALUE, and don't overflow
    sh.increaseLengths(1, 0);
    assertEquals(Integer.MAX_VALUE, sh.getMinimumLength());
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
    StringHolder shBroken = new StringHolder(0) {
      {
        setError();
        theString = "error";
      }

      @Override
      protected String getString() {
        fail("Unexpected");
        return "";
      }

    };
    assertFalse(shBroken.isKnownEmpty());
    shBroken.length();
    assertFalse(shBroken.isKnownEmpty());
  }
}
