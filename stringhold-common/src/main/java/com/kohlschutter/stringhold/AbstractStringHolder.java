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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;
import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.util.ComparisonUtil;

/**
 * Base implementation of a {@link StringHolder}.
 *
 * @author Christian Kohlschütter
 */
@SuppressWarnings({
    "PMD.CyclomaticComplexity", "PMD.ExcessiveClassLength", "PMD.ExcessivePublicCount"})
public abstract class AbstractStringHolder extends CharSequenceReleaseShim implements StringHolder {
  String theString;

  private int minLength;
  private int expectedLength;

  private boolean trouble = false;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  private StringHolderScope scope = null;

  /**
   * Constructs a {@link AbstractStringHolder} with a zero minimum length.
   */
  protected AbstractStringHolder() {
    this(0);
  }

  /**
   * Constructs a {@link AbstractStringHolder} with the given minimum length, use {@code 0} if no
   * minimum length is known.
   *
   * @param minLength The minimum length, which must not be larger than the eventual actual length.
   */
  protected AbstractStringHolder(int minLength) {
    this(minLength, minLength);
  }

  /**
   * Constructs a {@link AbstractStringHolder} with the given minimum length (use {@code 0} if no
   * minimum length is known), and expected length.
   *
   * @param minLength The minimum length, which must not be larger than the eventual actual length.
   * @param expectedLength The expected length, which may be larger than the eventual actual length
   */
  protected AbstractStringHolder(int minLength, int expectedLength) {
    super();
    if (minLength < 0) {
      throw new IllegalArgumentException("Invalid minLength");
    }
    this.minLength = minLength;
    this.expectedLength = Math.max(minLength, expectedLength);
  }

  @Override
  public final int getMinimumLength() {
    return minLength;
  }

  @Override
  public final int getExpectedLength() {
    return expectedLength;
  }

  @Override
  public void setExpectedLength(int len) {
    resizeTo(getMinimumLength(), len);
  }

  /**
   * Sets the expected lengths (minimum and estimated) to the given values.
   *
   * @param min The new minimum length, must not be smaller than the current minimum (unless
   *          {@link #checkError()} is {@code true})
   * @param expected The new expected length (will be rounded up if less than {@code min}).
   * @return The new expected length (which may be adjusted to the new minimum).
   * @throws IllegalStateException if the value is negative, and {@link #checkError()} is
   *           {@code false}.
   */
  protected final int resizeTo(int min, int expected) {
    return resizeTo(min, expected, false);
  }

  private int resizeTo(int min, int expected, boolean fromToString) {
    int oldMin = this.minLength;
    int oldExpected = this.expectedLength;

    if (!fromToString && isString()) {
      // unchanged
      return oldExpected;
    }

    if (min < oldMin) {
      if (checkError()) {
        // throw away our previous expectations upon error
        expected = (min = Math.max(0, min));
      } else {
        if (fromToString) {
          setError();
          resizeTo(min, expected, false);
          throw new IllegalStateException("Detected mispredicted minLength");
        } else {
          throw new IllegalStateException("New minimum is smaller than current minimum");
        }
      }
    }

    int el = (this.expectedLength = Math.max((this.minLength = min), expected));

    StringHolderScope sc = this.scope;
    if (sc != null) {
      try {
        sc.resizeBy(this.minLength - oldMin, this.expectedLength - oldExpected);
      } catch (RuntimeException | Error e) {
        setError();
        throw e;
      }
    }

    return el;
  }

  /**
   * Increases the expected lengths (minimum and estimated) by the given values.
   *
   * Any value that overflows {@link Integer#MAX_VALUE} will be capped at that limit.
   *
   * @param minBy The minimum length increment, must not be negative (unless {@link #checkError()}
   *          is {@code true})
   * @param expectedBy The expected length increment, may be negative; final length will be
   *          {@code >= 0}.
   * @throws IllegalArgumentException if minBy is negative and {@link #checkError()} is
   *           {@code false}
   */
  protected final void resizeBy(int minBy, int expectedBy) {
    int oldMin = this.minLength;
    int oldExpected = this.expectedLength;

    if (minBy < 0) {
      if (checkError()) {
        this.minLength = Math.max(0, this.minLength + minBy);
      } else {
        throw new IllegalArgumentException("Minimum length increment is negative");
      }
    } else if ((this.minLength += minBy) < 0) {
      // cannot express minimum length that large
      this.minLength = Integer.MAX_VALUE;
    }
    this.expectedLength = Math.max(minLength, this.expectedLength + expectedBy);

    StringHolderScope sc = this.scope;
    if (sc != null) {
      try {
        sc.resizeBy(this.minLength - oldMin, this.expectedLength - oldExpected);
      } catch (RuntimeException | Error e) {
        setError();
        throw e;
      }
    }
  }

  @Override
  public final int length() {
    if (theString != null) {
      return minLength;
    } else {
      return resizeTo(computeLength(), 0);
    }
  }

  /**
   * Computes the actual length of this instance's contents.
   *
   * By default, this is implemented as {@code toString().length()}.
   *
   * When overriding this method, make sure to also override {@link #isLengthKnown()}.
   *
   * @return The actual length.
   */
  protected int computeLength() {
    return toString().length();
  }

  @Override
  public final boolean isString() {
    return theString != null;
  }

  @Override
  public final boolean isKnownEmpty() {
    if (minLength > 0) {
      return false;
    } else if (isLengthKnown() && length() == 0) {
      return true;
    } else {
      String s;

      return (s = theString) != null && s.isEmpty();
    }
  }

  @Override
  public final boolean isEmpty() {
    return isKnownEmpty() || super.isEmpty();
  }

  @Override
  public boolean isLengthKnown() {
    return isString();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @SuppressFBWarnings("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
  @Override
  public final boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj == this) {
      return true;
    } else if (obj instanceof String) {
      return equalsString((String) obj);
    } else if (obj instanceof StringHolder) {
      return equalsStringHolder((StringHolder) obj);
    } else {
      return false;
    }
  }

  private boolean equalsString(String s) {
    if (!checkError() && s.length() < getMinimumLength()) {
      return false;
    } else if (isString()) {
      return toString().equals(s);
    } else if (isLengthKnown() && length() != s.length()) {
      return false;
    } else {
      return checkEquals(s);
    }
  }

  /**
   * Checks if this {@link StringHolder} instance is equal to the given String (assume that trivial
   * requirements, such as minimum length, were already checked).
   *
   * Subclasses may override this check for a faster operation.
   *
   * @param s The other string.
   * @return {@code true} if this {@link StringHolder} is equal to the given string.
   */
  protected boolean checkEquals(String s) {
    return toString().equals(s);
  }

  /**
   * Checks if this {@link StringHolder} instance is equal to the given {@link StringHolder} (assume
   * that trivial requirements, such as minimum length, were already checked).
   *
   * Subclasses may override this check for a faster operation.
   *
   * @param sh The other {@link StringHolder}.
   * @return {@code true} if this {@link StringHolder} is equal to the given string.
   */
  protected boolean checkEquals(StringHolder sh) {
    return toString().equals(sh.toString());
  }

  @SuppressWarnings("unlikely-arg-type")
  @SuppressFBWarnings("EC_UNRELATED_CLASS_AND_INTERFACE")
  private boolean equalsStringHolder(StringHolder obj) {
    if (isLengthKnown() && obj.isLengthKnown() && length() != obj.length()) {
      return false;
    }

    if (isString()) {
      if (!obj.checkError() && length() < obj.getMinimumLength()) {
        return false;
      }
      if (obj.isString()) {
        return toString().equals(obj.toString());
      } else {
        return obj.equals(toString());
      }
    } else if (obj.isString()) {
      if (!checkError() && obj.length() < getMinimumLength()) {
        return false;
      }
    }

    return checkEquals(obj);
  }

  @Override
  public final void appendTo(Appendable out) throws IOException {
    appendToAndReturnLength(out);
  }

  @Override
  public final void appendTo(StringBuilder out) {
    appendToAndReturnLength(out);
  }

  @Override
  public final void appendTo(StringBuffer out) {
    appendToAndReturnLength(out);
  }

  @Override
  public final void appendTo(Writer out) throws IOException {
    appendToAndReturnLength(out);
  }

  @Override
  public final int appendToAndReturnLength(Appendable out) throws IOException {
    if (out instanceof Writer) {
      return appendToAndReturnLength((Writer) out);
    } else if (out instanceof StringBuilder) {
      return appendToAndReturnLength((StringBuilder) out);
    } else if (out instanceof StringBuffer) {
      return appendToAndReturnLength((StringBuffer) out);
    } else {
      return appendToAndReturnLengthDefault(out);
    }
  }

  @Override
  public final int appendToAndReturnLength(StringBuilder out) {
    int len;
    if (isString()) {
      len = length();
      if (len > 0) {
        out.append(toString());
      }
    } else {
      len = appendToAndReturnLengthImpl(out);
      if (minLength < len) {
        resizeTo(len, 0);
      }
    }
    return len;
  }

  @Override
  public final int appendToAndReturnLength(StringBuffer out) {
    int len;
    if (isString()) {
      len = length();
      out.append(toString());
    } else {
      len = appendToAndReturnLengthImpl(out);
      if (minLength < len) {
        resizeTo(len, 0);
      }
    }
    return len;
  }

  @Override
  public final int appendToAndReturnLength(Writer out) throws IOException {
    int len;
    if (isString()) {
      len = length();
      out.append(toString());
    } else {
      len = appendToAndReturnLengthImpl(out);
      if (minLength < len) {
        resizeTo(len, 0);
      }
    }
    return len;
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Appendable} (which is
   * neither a {@link StringBuilder}, {@link StringBuffer}, nor a {@link Writer}), and returns the
   * number of characters appended. This call may or may not turn the contents of this instance into
   * a String. It won't be called if it's already one.
   *
   * @param out The target.
   * @return The number of characters appended (which is assumed to be the new minimum length).
   * @see #appendToAndReturnLength(StringBuilder)
   * @see #appendToAndReturnLength(StringBuffer)
   * @see #appendToAndReturnLength(StringWriter)
   * @throws IOException on error.
   */
  protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
    String s = toString();
    if (!s.isEmpty()) {
      out.append(s);
    }
    return s.length();
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuilder}, and
   * returns the number of characters appended. This call may or may not turn the contents of this
   * instance into a String. It won't be called if it's already one.
   *
   * @param out The target.
   * @return The number of characters appended (which is assumed to be the new minimum length).
   */
  protected int appendToAndReturnLengthImpl(StringBuilder out) {
    String s = toString();
    if (!s.isEmpty()) {
      out.append(s);
    }
    return s.length();
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuffer}, and returns
   * the number of characters appended. This call may or may not turn the contents of this instance
   * into a String. It won't be called if it's already one.
   *
   * @param out The target.
   * @return The number of characters appended (which is assumed to be the new minimum length).
   */
  protected int appendToAndReturnLengthImpl(StringBuffer out) {
    String s = toString();
    if (!s.isEmpty()) {
      out.append(s);
    }
    return s.length();
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Writer}, and returns the
   * number of characters appended. This call may or may not turn the contents of this instance into
   * a String. It won't be called if it's already one.
   *
   * @param out The target.
   * @return The number of characters appended (which is assumed to be the new minimum length).
   * @throws IOException on error.
   */
  protected int appendToAndReturnLengthImpl(Writer out) throws IOException {
    // subclasses may implement a better way for Writers, but we don't
    return appendToAndReturnLengthDefault(out);
  }

  private int appendToAndReturnLengthDefault(Appendable out) throws IOException {
    int len;
    if (isString()) {
      len = length();
      out.append(toString());
    } else {
      len = appendToAndReturnLengthDefaultImpl(out);
      if (minLength < len) {
        resizeTo(len, 0);
      }
    }
    return len;
  }

  @Override
  public final String toString() {
    String s = theString;
    if (s != null) {
      return s;
    }
    synchronized (this) {
      try {
        if (isKnownEmpty()) {
          theString = s = "";
        } else {
          theString = s = CommonStrings.lookupIfPossible(Objects.requireNonNull(getString()));
        }
        resizeTo(s.length(), 0, true);
      } catch (RuntimeException e) {
        s = theString;
        if (s != null) {
          resizeTo(s.length(), 0, true);
        }
        setError();
        throw e;
      }

      stringSanityCheck(s);
      return s;
    }
  }

  /**
   * Called from within {@link #toString()} after updating/assigning the cached string but before
   * returning it. This may be a good opportunity to see if we got what we wanted, call setError,
   * etc.
   *
   * @param s The string.
   */
  protected void stringSanityCheck(String s) {
  }

  /**
   * Retrieves the string.
   *
   * @return The string; must not be {@code null}.
   */
  protected abstract String getString();

  /**
   * Un-caches the already-determined String. This can be used to implement mutable data structures.
   *
   * Important: Subclasses must carefully check {@link #isEffectivelyImmutable()} status.
   */
  protected void uncache() {
    theString = null;
  }

  @Override
  public final Reader toReader() throws IOException {
    String s = theString;
    if (s != null) {
      return new StringReader(s);
    } else {
      return newReader();
    }
  }

  @Override
  public final boolean checkError() {
    return trouble;
  }

  /**
   * Signals that this instance had some kind of unexpected condition.
   *
   * @see #checkError()
   * @see #clearError()
   */
  protected final void setError() {
    trouble = true;
    StringHolderScope sc = scope;
    if (sc != null) {
      sc.setError(this);
    }
  }

  /**
   * Clears the trouble state of this instance.
   *
   * @see #checkError()
   * @see #setError()
   */
  protected final void clearError() {
    trouble = false;
    StringHolderScope sc = scope;
    if (sc != null) {
      sc.clearError(this);
    }
  }

  /**
   * Constructs a new {@link Reader} providing the contents of this {@link StringHolder}.
   *
   * @return The reader.
   * @throws IOException on error.
   */
  protected Reader newReader() throws IOException {
    return LazyInitReader.withSupplier(() -> new StringReader(AbstractStringHolder.this
        .toString()));
  }

  @Override
  public final StringHolderScope getScope() {
    return scope;
  }

  @Override
  public final StringHolderScope updateScope(StringHolderScope newScope) {
    if (newScope == StringHolderScope.NONE) { // NOPMD.CompareObjectsWithEquals
      newScope = null;
    }
    StringHolderScope oldScope = this.scope;
    if (oldScope == newScope) { // NOPMD.CompareObjectsWithEquals
      return oldScope;
    }

    if (oldScope != null) {
      try {
        oldScope.remove(this);
      } catch (RuntimeException | Error e) {
        setError();
        throw e;
      }
    }

    if (newScope != null) {
      try {
        newScope.add(this);
      } catch (RuntimeException | Error e) {
        setError();
        throw e;
      }
    }
    this.scope = newScope;
    return oldScope;
  }

  @Override
  public char charAt(int index) {
    return toString().charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (start == 0 && end == length()) {
      return this;
    }
    return toString().subSequence(start, end);
  }

  @Override
  public Object asContent() {
    if (isString()) {
      return toString();
    }
    return this;
  }

  @Override
  public int compareTo(CharSequence o) {
    if (o instanceof StringHolder) {
      return compareTo((StringHolder) o);
    }

    if (isKnownEmpty()) {
      if (CharSequenceReleaseShim.isEmpty(o)) {
        return 0;
      } else {
        return -1;
      }
    }

    return compareToDefault(o);
  }

  @Override
  public int compareTo(StringHolder o) {
    if (o.isKnownEmpty()) {
      if (isKnownEmpty()) {
        return 0;
      }
    } else if (o.isString()) {
      if (isString()) {
        return toString().compareTo(o.toString());
      } else {
        return compareTo(o.toString());
      }
    } else if (isString()) {
      return ComparisonUtil.reverseComparisonResult(o.compareTo(toString()));
    }

    return compareToDefault(o);
  }

  /**
   * Default implementation for comparing this instance with another {@link CharSequence} that is
   * not a {@link StringHolder}. Certain trivial checks were already performed, such as one or both
   * being known empty.
   *
   * @param o The other object.
   * @return The comparison result, as defined by {@link #compareTo(Object)}.
   */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  protected final int compareToDefault(CharSequence o) {
    int k = 0;

    if (getMinimumLength() > 0 && CharSequenceReleaseShim.isEmpty(o)) {
      // NOTE: we trust the StringHolder claim of minimum length
      return 1;
    }
    int len2 = o.length();

    int lim;
    char c1;
    try {
      c1 = charAt(k);
    } catch (IndexOutOfBoundsException e) {
      if (len2 == 0) {
        return 0;
      } else {
        return -1;
      }
    }

    int len1;
    if (isString()) {
      if (o instanceof String) {
        return toString().compareTo((String) o);
      }
      len1 = length();
      lim = Math.min(len1, len2);
    } else {
      if (isLengthKnown()) {
        len1 = length();
        lim = Math.min(len1, len2);
      } else {
        len1 = Integer.MAX_VALUE;
        lim = len2;
      }
    }

    while (k < lim) {
      char c2 = o.charAt(k);
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
      if (k < lim) {
        try {
          c1 = charAt(k);
        } catch (IndexOutOfBoundsException e) {
          return -1;
        }
      }
    }

    if (getMinimumLength() > k) {
      return 1;
    } else if (len1 == k) {
      return len1 - len2;
    }
    try {
      charAt(k);
      return 1;
    } catch (IndexOutOfBoundsException e) {
      return 0;
    }
  }

  /**
   * Default implementation for comparing this instance with another {@link StringHolder}. Certain
   * trivial checks were already performed, such as one or both being known empty or known being
   * string.
   *
   * @param o The other object.
   * @return The comparison result, as defined by {@link #compareTo(Object)}.
   */
  @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity"})
  protected int compareToDefault(StringHolder o) {
    int k = 0;

    char c1;
    char c2;
    try {
      c1 = charAt(k);
    } catch (IndexOutOfBoundsException e) {
      try {
        o.charAt(k);
        return -1;
      } catch (IndexOutOfBoundsException e2) {
        return 0;
      }
    }
    try {
      c2 = o.charAt(k);
    } catch (IndexOutOfBoundsException e) {
      return 1;
    }

    if (c1 != c2) {
      return c1 - c2;
    }

    if (isString()) {
      return ComparisonUtil.reverseComparisonResult(o.compareTo(toString()));
    } else if (o.isString()) {
      return compareTo(o.toString());
    }

    boolean len1Known = isLengthKnown();
    boolean len2Known = o.isLengthKnown();

    if (len1Known && len2Known) {
      return compareBothLengthsKnown(o, k, length(), o.length());
    } else if (len1Known) {
      return compareOurLengthKnown(o, k, length());
    } else if (len2Known) {
      return compareOtherLengthKnown(o, k, o.length());
    } else {
      return compareBothLengthsUnknown(o, k);
    }
  }

  private int compareBothLengthsKnown(StringHolder o, int k, int len1, int len2) {
    int lim = Math.min(len1, len2);

    char c1;
    char c2;

    while (k < lim) {
      c1 = charAt(k);
      c2 = o.charAt(k);
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }

    return len1 - len2;
  }

  private int compareOurLengthKnown(StringHolder o, int k, int len1) {
    char c1;
    char c2;

    while (k < len1) {
      c1 = charAt(k);
      try {
        c2 = o.charAt(k);
      } catch (IndexOutOfBoundsException e) {
        return 1;
      }
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }

    try {
      if (o.getMinimumLength() > k) {
        return -1;
      }
      o.charAt(k);
      return -1;
    } catch (IndexOutOfBoundsException e) {
      return 0;
    }
  }

  private int compareOtherLengthKnown(StringHolder o, int k, int len2) {
    char c1;
    char c2;

    while (k < len2) {
      try {
        c1 = charAt(k);
      } catch (IndexOutOfBoundsException e) {
        return -1;
      }
      c2 = o.charAt(k);
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }

    try {
      if (getMinimumLength() > k) {
        return 1;
      }
      charAt(k);
      return 1;
    } catch (IndexOutOfBoundsException e) {
      return 0;
    }
  }

  private int compareBothLengthsUnknown(StringHolder o, int k) {
    char c1;
    char c2;
    while (true) {
      k++;

      try {
        c1 = charAt(k);
      } catch (IndexOutOfBoundsException e) {
        try {
          o.charAt(k);
          return -1;
        } catch (IndexOutOfBoundsException e2) {
          return 0;
        }
      }
      try {
        c2 = o.charAt(k);
      } catch (IndexOutOfBoundsException e) {
        return 1;
      }

      if (c1 != c2) {
        return c1 - c2;
      }
    }
  }

  /**
   * Computes a partial hash code, using the given value as the seed.
   *
   * @param h The initial value (seed).
   * @return The updated hash code.
   */
  protected int updateHashCode(int h) {
    int length = length();

    if (h == 0 && isString()) {
      return toString().hashCode();
    }

    for (int i = 0; i < length; i++) {
      h = 31 * h + charAt(i);
    }
    return h;
  }

  @Override
  public boolean isEffectivelyImmutable() {
    return isString();
  }

  @Override
  public void markEffectivelyImmutable() {
    if (!isEffectivelyImmutable()) {
      toString();
    }
  }

  @ExcludeFromCodeCoverageGeneratedReport(reason = "exception unreachable")
  private StringHolder cloneSuper() {
    try {
      return (StringHolder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public StringHolder clone() {
    return cloneSuper();
  }
}
