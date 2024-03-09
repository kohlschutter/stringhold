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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class CompareTest {

  private static StringHolder ensureString(StringHolder sh) {
    sh.toString();
    return sh;
  }

  @Test
  public void testCompareEqualStrings() throws Exception {
    assertEquals(0, StringHolder.withContent("hello").compareTo("hello"));
    assertEquals(0, StringHolder.withContent("hello").compareTo(new StringBuilder("hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo(new StringBuffer("hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo(new WrappedCharSequence("hello")));

    assertEquals(0, StringHolder.withContent("hello").compareTo(StringHolder.withContent("hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo(StringHolder.withSupplier(
        () -> "hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo(ensureString(StringHolder
        .withSupplier(() -> "hello"))));
    assertEquals(0, StringHolder.withContent("hello").compareTo(new StringHolderSequence().append(
        "hel").append("lo")));
    assertEquals(0, StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("hel").append("lo"))));
    assertEquals(0, StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("hel").append(StringHolder.withSupplier(() -> "lo")))));

    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo("hello"));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(new StringBuilder("hello")));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(new StringBuffer("hello")));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(new WrappedCharSequence(
        "hello")));

    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(StringHolder.withContent(
        "hello")));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(StringHolder.withSupplier(
        () -> "hello")));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(ensureString(StringHolder
        .withSupplier(() -> "hello"))));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(new StringHolderSequence()
        .append("hel").append("lo")));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("hel").append("lo"))));
    assertEquals(0, StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("hel").append(StringHolder.withSupplier(() -> "lo")))));

    StringHolder sh = StringHolder.withSupplier(() -> "hello");
    sh.toString();
    assertEquals(0, sh.compareTo("hello"));
    assertEquals(0, sh.compareTo(new StringBuilder("hello")));
    assertEquals(0, sh.compareTo(new StringBuffer("hello")));
    assertEquals(0, sh.compareTo(new WrappedCharSequence("hello")));
  }

  @Test
  public void testCompareLessStrings() throws Exception {
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo("world!")));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(new StringBuilder(
        "world!"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(new StringBuffer(
        "world!"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(
        new WrappedCharSequence("world!"))));

    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(StringHolder
        .withContent("world!"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(StringHolder
        .withSupplier(() -> "world!"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        StringHolder.withSupplier(() -> "world!")))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(
        new StringHolderSequence().append("wor").append("ld"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("wor").append("ld")))));
    assertEquals(-1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("wor").append(StringHolder.withSupplier(() -> "ld"))))));

    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo("world!")));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new StringBuilder("world!"))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new StringBuffer("world!"))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new WrappedCharSequence("world!"))));

    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(StringHolder
        .withContent("world!"))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(StringHolder
        .withSupplier(() -> "world!"))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        StringHolder.withSupplier(() -> "world!")))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new StringHolderSequence().append("wor").append("ld"))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("wor").append("ld")))));
    assertEquals(-1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("wor").append(StringHolder.withSupplier(() -> "ld"))))));

    StringHolder sh = ensureString(StringHolder.withSupplier(() -> "hello"));
    assertEquals(-1, Math.signum(sh.compareTo("world!")));
    assertEquals(-1, Math.signum(sh.compareTo(new StringBuilder("world!"))));
    assertEquals(-1, Math.signum(sh.compareTo(new StringBuffer("world!"))));
    assertEquals(-1, Math.signum(sh.compareTo(new WrappedCharSequence("world!"))));
  }

  @Test
  public void testCompareMoreStrings() throws Exception {
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo("ha")));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(new StringBuilder(
        "ha"))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(new StringBuffer(
        "ha"))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(new WrappedCharSequence(
        "ha"))));

    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(StringHolder
        .withContent("ha"))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(StringHolder
        .withSupplier(() -> "ha"))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        StringHolder.withSupplier(() -> "ha")))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(
        new StringHolderSequence().append("h").append("a"))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("h").append("a")))));
    assertEquals(1, Math.signum(StringHolder.withContent("hello").compareTo(ensureString(
        new StringHolderSequence().append("h").append(StringHolder.withSupplier(() -> "a"))))));

    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo("ha")));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new StringBuilder("ha"))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(new StringBuffer(
        "ha"))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new WrappedCharSequence("ha"))));

    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(StringHolder
        .withContent("ha"))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(StringHolder
        .withSupplier(() -> "ha"))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        StringHolder.withSupplier(() -> "ha")))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(
        new StringHolderSequence().append("h").append("a"))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("h").append("a")))));
    assertEquals(1, Math.signum(StringHolder.withSupplier(() -> "hello").compareTo(ensureString(
        new StringHolderSequence().append("h").append(StringHolder.withSupplier(() -> "a"))))));

    StringHolder sh = ensureString(StringHolder.withSupplier(() -> "hello"));
    assertEquals(1, Math.signum(sh.compareTo("ha")));
    assertEquals(1, Math.signum(sh.compareTo(new StringBuilder("ha"))));
    assertEquals(1, Math.signum(sh.compareTo(new StringBuffer("ha"))));
    assertEquals(1, Math.signum(sh.compareTo(new WrappedCharSequence("ha"))));
  }

  public void testNotComparable() throws Exception {
    assertThrows(ClassCastException.class, () -> StringHolder.withContent("hello").compareTo(
        new Object()));
    assertThrows(ClassCastException.class, () -> StringHolder.withContent("hello").compareTo(
        new StringReader("hello")));
  }

  @Test
  public void testCompareEmpty() throws Exception {
    assertEquals(0, StringHolder.withContent("").compareTo(""));
    assertEquals(0, StringHolder.withContent("").compareTo(new StringBuilder("")));
    assertEquals(0, StringHolder.withContent("").compareTo(new StringBuffer("")));
    assertEquals(0, StringHolder.withContent("").compareTo(new WrappedCharSequence("")));
    assertEquals(0, StringHolder.withContent("").compareTo(StringHolder.withContent("")));
    assertEquals(0, StringHolder.withContent("").compareTo(StringHolder.withSupplier(() -> "")));
    assertEquals(0, StringHolder.withContent("").compareTo(ensureString(StringHolder.withSupplier(
        () -> ""))));
    assertEquals(0, StringHolder.withContent("").compareTo(new StringHolderSequence()));
    assertEquals(0, StringHolder.withContent("").compareTo(new StringHolderSequence().append("")));
    assertEquals(0, StringHolder.withContent("").compareTo(new StringHolderSequence().append(
        StringHolder.withSupplier(() -> ""))));

    assertEquals(0, StringHolder.withContent("").compareTo(new CustomEmptyStringHolder()));
    assertEquals(0, new CustomEmptyStringHolder().compareTo(StringHolder.withContent("")));
    assertEquals(0, new CustomEmptyStringHolder().compareTo(new CustomEmptyStringHolder()));
  }

  private static final class CustomEmptyStringHolder extends AbstractStringHolder {
    @Override
    protected String getString() {
      return "";
    }
  }

  @Test
  public void testCompareEmptyCharSequence() throws Exception {
    StringHolder.withSupplierMinimumLength(1, () -> fail("should not be reached")).compareTo("");
  }

  @Test
  public void testCompareEmptyLess() throws Exception {
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo("a")));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(new StringBuilder("a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(new StringBuffer("a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(new WrappedCharSequence(
        "a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(StringHolder.withContent(
        "a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(StringHolder.withSupplier(
        () -> "a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(ensureString(StringHolder
        .withSupplier(() -> "a")))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(new StringHolderSequence()
        .append("a"))));
    assertEquals(-1, Math.signum(StringHolder.withContent("").compareTo(new StringHolderSequence()
        .append(StringHolder.withSupplier(() -> "a")))));
  }

  @Test
  public void testCompareObject() throws Exception {
    assertEquals(0, StringHolder.withContent("hello").compareTo((Object) "hello"));
    assertEquals(0, StringHolder.withContent("hello").compareTo((Object) StringHolder.withContent(
        "hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo((CharSequence) StringHolder
        .withContent("hello")));
    assertEquals(0, StringHolder.withContent("hello").compareTo((Object) new WrappedCharSequence(
        "hello")));
    assertThrows(ClassCastException.class, () -> StringHolder.withContent("hello").compareTo(
        new Object()));
    assertThrows(NullPointerException.class, () -> StringHolder.withContent("hello").compareTo(
        (Object) null));
    assertThrows(NullPointerException.class, () -> StringHolder.withContent("hello").compareTo(
        (CharSequence) null));
    assertThrows(NullPointerException.class, () -> StringHolder.withContent("hello").compareTo(
        (StringHolder) null));
  }

  @Test
  public void testCustomCharAt() throws Exception {
    assertEquals(0, new CustomCharAtStringHolder("").compareTo(new CustomCharAtStringHolder("")));
    assertEquals(-1, new CustomCharAtStringHolder("").compareTo(new CustomCharAtStringHolder("a")));
    assertEquals(1, new CustomCharAtStringHolder("a").compareTo(new CustomCharAtStringHolder("")));
    assertEquals(0, new CustomCharAtStringHolder("a").compareTo(new CustomCharAtStringHolder("a")));
    assertEquals(-1, new CustomCharAtStringHolder("a").compareTo(new CustomCharAtStringHolder(
        "aa")));
    assertEquals(1, new CustomCharAtStringHolder("aa").compareTo(new CustomCharAtStringHolder(
        "a")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(new CustomCharAtStringHolder(
        "ab")));
    assertEquals(0, new CustomCharAtStringHolder("aa").compareTo(new CustomCharAtStringHolder(
        "aa")));
    assertEquals(1, new CustomCharAtStringHolder("ab").compareTo(new CustomCharAtStringHolder(
        "aa")));
    assertEquals(0, new CustomCharAtStringHolder("aaa").compareTo(new CustomCharAtStringHolder(
        "aaa")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(new CustomCharAtStringHolder(
        "aaa")));
    assertEquals(1, new CustomCharAtStringHolder("aaa").compareTo(new CustomCharAtStringHolder(
        "aa")));
    assertEquals(-1, new CustomCharAtStringHolder("aaa").compareTo(new CustomCharAtStringHolder(
        "aab")));
    assertEquals(1, new CustomCharAtStringHolder("aab").compareTo(new CustomCharAtStringHolder(
        "aaa")));

    assertEquals(0, new CustomCharAtStringHolder("").compareTo(""));
    assertEquals(-1, new CustomCharAtStringHolder("").compareTo("a"));
    assertEquals(1, new CustomCharAtStringHolder("a").compareTo(""));
    assertEquals(0, new CustomCharAtStringHolder("a").compareTo("a"));
    assertEquals(-1, new CustomCharAtStringHolder("a").compareTo("aa"));
    assertEquals(1, new CustomCharAtStringHolder("aa").compareTo("a"));
    assertEquals(0, new CustomCharAtStringHolder("aa").compareTo("aa"));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo("ab"));
    assertEquals(1, new CustomCharAtStringHolder("ab").compareTo("aa"));
    assertEquals(0, new CustomCharAtStringHolder("aaa").compareTo("aaa"));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo("aaa"));
    assertEquals(1, new CustomCharAtStringHolder("aaa").compareTo("aa"));
    assertEquals(-1, new CustomCharAtStringHolder("aaa").compareTo("aab"));
    assertEquals(1, new CustomCharAtStringHolder("aab").compareTo("aaa"));

    assertEquals(0, new CustomCharAtStringHolder("").compareTo(new WrappedCharSequence("")));
    assertEquals(-1, new CustomCharAtStringHolder("").compareTo(new WrappedCharSequence("a")));
    assertEquals(1, new CustomCharAtStringHolder("a").compareTo(new WrappedCharSequence("")));
    assertEquals(0, new CustomCharAtStringHolder("a").compareTo(new WrappedCharSequence("a")));
    assertEquals(-1, new CustomCharAtStringHolder("a").compareTo(new WrappedCharSequence("aa")));
    assertEquals(1, new CustomCharAtStringHolder("aa").compareTo(new WrappedCharSequence("a")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(new WrappedCharSequence("ab")));
    assertEquals(1, new CustomCharAtStringHolder("ab").compareTo(new WrappedCharSequence("aa")));
    assertEquals(0, new CustomCharAtStringHolder("aaa").compareTo(new WrappedCharSequence("aaa")));
    assertEquals(-1, new CustomCharAtStringHolder("aaa").compareTo(new WrappedCharSequence("aab")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(new WrappedCharSequence("aaa")));
    assertEquals(1, new CustomCharAtStringHolder("aaa").compareTo(new WrappedCharSequence("aa")));
    assertEquals(1, new CustomCharAtStringHolder("aab").compareTo(new WrappedCharSequence("aaa")));

    assertEquals(0, new KnownLengthCustomCharAtStringHolder("").compareTo(new WrappedCharSequence(
        "")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("").compareTo(new WrappedCharSequence(
        "a")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("a").compareTo(new WrappedCharSequence(
        "")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("a").compareTo(new WrappedCharSequence(
        "a")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("a").compareTo(new WrappedCharSequence(
        "aa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(new WrappedCharSequence(
        "a")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new WrappedCharSequence("ab")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("ab").compareTo(new WrappedCharSequence(
        "aa")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new WrappedCharSequence("aaa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new WrappedCharSequence("aab")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new WrappedCharSequence("aaa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new WrappedCharSequence("aa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aab").compareTo(
        new WrappedCharSequence("aaa")));

    assertEquals(0, new KnownLengthCustomCharAtStringHolder("").compareTo(
        new CustomCharAtStringHolder("")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("").compareTo(
        new CustomCharAtStringHolder("a")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new CustomCharAtStringHolder("")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new CustomCharAtStringHolder("a")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new CustomCharAtStringHolder("aa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("a")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("aa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("ab")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("ab").compareTo(
        new CustomCharAtStringHolder("aa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("aaa")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new CustomCharAtStringHolder("aaa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new CustomCharAtStringHolder("aa")));

    assertEquals(0, new CustomCharAtStringHolder("").compareTo(
        new KnownLengthCustomCharAtStringHolder("")));
    assertEquals(-1, new CustomCharAtStringHolder("").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(1, new CustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("")));
    assertEquals(0, new CustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(-1, new CustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(1, new CustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("ab")));
    assertEquals(0, new CustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(1, new CustomCharAtStringHolder("ab").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aaa")));
    assertEquals(0, new CustomCharAtStringHolder("aaa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aaa")));
    assertEquals(1, new CustomCharAtStringHolder("aaa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));

    assertEquals(0, new KnownLengthCustomCharAtStringHolder("").compareTo(
        new KnownLengthCustomCharAtStringHolder("")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("a").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("a")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("ab")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("ab").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aaa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aaa")));
    assertEquals(1, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aa")));

    assertEquals(0, new CustomCharAtStringHolder("").compareTo(new StringTurningCharAtStringHolder(
        "")));
    assertEquals(-1, new CustomCharAtStringHolder("").compareTo(new StringTurningCharAtStringHolder(
        "a")));
    assertEquals(1, new CustomCharAtStringHolder("a").compareTo(new StringTurningCharAtStringHolder(
        "")));
    assertEquals(0, new CustomCharAtStringHolder("a").compareTo(new StringTurningCharAtStringHolder(
        "a")));
    assertEquals(-1, new CustomCharAtStringHolder("a").compareTo(
        new StringTurningCharAtStringHolder("aa")));
    assertEquals(1, new CustomCharAtStringHolder("aa").compareTo(
        new StringTurningCharAtStringHolder("a")));
    assertEquals(-1, new CustomCharAtStringHolder("aa").compareTo(
        new StringTurningCharAtStringHolder("ab")));
    assertEquals(1, new CustomCharAtStringHolder("ab").compareTo(
        new StringTurningCharAtStringHolder("aa")));
    assertEquals(0, new CustomCharAtStringHolder("aaa").compareTo(
        new StringTurningCharAtStringHolder("aaa")));
    assertEquals(-1, new CustomCharAtStringHolder("aaa").compareTo(
        new StringTurningCharAtStringHolder("aab")));
    assertEquals(1, new CustomCharAtStringHolder("aab").compareTo(
        new StringTurningCharAtStringHolder("aaa")));

    assertEquals(0, new StringTurningCharAtStringHolder("").compareTo(new CustomCharAtStringHolder(
        "")));
    assertEquals(-1, new StringTurningCharAtStringHolder("").compareTo(new CustomCharAtStringHolder(
        "a")));
    assertEquals(1, new StringTurningCharAtStringHolder("a").compareTo(new CustomCharAtStringHolder(
        "")));
    assertEquals(0, new StringTurningCharAtStringHolder("a").compareTo(new CustomCharAtStringHolder(
        "a")));
    assertEquals(-1, new StringTurningCharAtStringHolder("a").compareTo(
        new CustomCharAtStringHolder("aa")));
    assertEquals(1, new StringTurningCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("a")));
    assertEquals(-1, new StringTurningCharAtStringHolder("aa").compareTo(
        new CustomCharAtStringHolder("ab")));
    assertEquals(1, new StringTurningCharAtStringHolder("ab").compareTo(
        new CustomCharAtStringHolder("aa")));
    assertEquals(0, new StringTurningCharAtStringHolder("aab").compareTo(
        new CustomCharAtStringHolder("aab")));
    assertEquals(-1, new StringTurningCharAtStringHolder("aaa").compareTo(
        new CustomCharAtStringHolder("aab")));
    assertEquals(1, new StringTurningCharAtStringHolder("aab").compareTo(
        new CustomCharAtStringHolder("aaa")));

    assertEquals(1, new CustomCharAtStringHolder(4, "aaaa").compareTo(
        new KnownLengthCustomCharAtStringHolder("aaa")));
    assertEquals(0, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new CustomCharAtStringHolder(3, "aaa")));
    assertEquals(-1, new KnownLengthCustomCharAtStringHolder("aaa").compareTo(
        new CustomCharAtStringHolder(4, "aaaa")));
  }

  private static class CustomCharAtStringHolder extends AbstractStringHolder {
    protected final String str;

    CustomCharAtStringHolder(String s) {
      this(0, s);
    }

    CustomCharAtStringHolder(int minLen, String s) {
      super(minLen);
      this.str = s;
    }

    @Override
    protected int computeLength() {
      fail("Should not be reached");
      return str.length();
    }

    @Override
    public char charAt(int index) {
      return str.charAt(index);
    }

    @Override
    protected String getString() {
      fail("Should not be reached");
      return str;
    }
  }

  private static class KnownLengthCustomCharAtStringHolder extends CustomCharAtStringHolder {

    KnownLengthCustomCharAtStringHolder(String s) {
      this(s.length(), s);
    }

    KnownLengthCustomCharAtStringHolder(int minLen, String s) {
      super(minLen, s);
    }

    @Override
    public boolean isLengthKnown() {
      return true;
    }

    @Override
    protected int computeLength() {
      return str.length();
    }

  }

  /**
   * A Custom {@link StringHolder} that turns into a string after calling {@link #charAt(int)}.
   *
   * @author Christian Kohlschütter
   */
  private static class StringTurningCharAtStringHolder extends CustomCharAtStringHolder {
    StringTurningCharAtStringHolder(String s) {
      super(s);
    }

    @Override
    public char charAt(int index) {
      char c = super.charAt(index);
      toString();
      return c;
    }

    @Override
    protected String getString() {
      return str;
    }
  }
}
