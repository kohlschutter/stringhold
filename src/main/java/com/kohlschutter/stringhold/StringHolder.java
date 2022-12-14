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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Supplier;

import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;
import com.kohlschutter.util.ComparisonUtil;

/**
 * A {@link StringHolder} holds something that can <em>eventually</em> turn into a string.
 *
 * {@link StringHolder}s may reduce string allocation in cases where the final string sequence is,
 * for example sent to a Writer (or other Appendable), discarded after a certain length, ignored
 * upon an exception thrown along the way, etc.
 *
 * Apart from reducing string concatenation-related allocations, {@link StringHolder}s may reduce
 * the end-to-end string life-cycle by allowing concurrency between construction and transmission:
 * The string can be transmitted while it's being constructed.
 *
 * Unlike regular stream-based approaches, a pre-rendered structure is available before transmission
 * starts. This means a transmission that is known to exceed certain limits can be stopped before a
 * single character is transmitted.
 *
 * @author Christian Kohlschütter
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ExcessiveClassLength"})
public abstract class StringHolder extends CharSequenceReleaseShim implements CharSequence,
    HasLength, Comparable<Object> {

  String theString;

  private int minLength;
  private int expectedLength;

  private boolean trouble = false;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  private StringHolderScope scope = null;

  /**
   * Constructs a {@link StringHolder} with a zero minimum length.
   */
  protected StringHolder() {
    this(0);
  }

  /**
   * Constructs a {@link StringHolder} with the given minimum length, use {@code 0} if no minimum
   * length is known.
   *
   * @param minLength The minimum length, which must not be larger than the eventual actual length.
   */
  protected StringHolder(int minLength) {
    this(minLength, minLength);
  }

  /**
   * Constructs a {@link StringHolder} with the given minimum length (use {@code 0} if no minimum
   * length is known), and expected length.
   *
   * @param minLength The minimum length, which must not be larger than the eventual actual length.
   * @param expectedLength The expected length, which may be larger than the eventual actual length
   */
  protected StringHolder(int minLength, int expectedLength) {
    super();
    if (minLength < 0) {
      throw new IllegalArgumentException("Invalid minLength");
    }
    this.minLength = minLength;
    this.expectedLength = Math.max(minLength, expectedLength);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, assuming a minimum
   * length of 0.
   *
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplier(Supplier<?> supplier) {
    return withSupplierMinimumLength(0, supplier);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, assuming a minimum
   * length of 0.
   *
   * @param supplier The supplier (may throw an {@link IOException} upon {@link Supplier#get()},
   *          which is handled by the given exception handler).
   * @param onError The exception handler.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplier(IOSupplier<?> supplier, IOExceptionHandler onError) {
    return withSupplierMinimumLength(0, supplier, onError);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param minLength The minimum length, must not be larger than the actual length.
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierMinimumLength(int minLength, Supplier<?> supplier) {
    return new SuppliedStringHolder(minLength, minLength, supplier);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param minLength The minimum length, must not be larger than the actual length.
   * @param supplier The supplier (may throw an {@link IOException} upon {@link Supplier#get()},
   *          which is handled by the given exception handler).
   * @param onError The exception handler.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierMinimumLength(int minLength, IOSupplier<?> supplier,
      IOExceptionHandler onError) {
    return new SuppliedStringHolder(minLength, minLength, supplier, onError);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param expectedLength The expected length, may be larger than the actual length.
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierExpectedLength(int expectedLength, Supplier<?> supplier) {
    return new SuppliedStringHolder(0, expectedLength, supplier);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param expectedLength The expected length, may be larger than the actual length.
   * @param supplier The supplier (may throw an {@link IOException} upon {@link Supplier#get()},
   *          which is handled by the given exception handler).
   * @param onError The exception handler.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierExpectedLength(int expectedLength, IOSupplier<?> supplier,
      IOExceptionHandler onError) {
    return new SuppliedStringHolder(0, expectedLength, supplier, onError);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param minLength The minimum length, must not be larger than the actual length.
   * @param expectedLength The expected length, may be larger than the actual length.
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierMinimumAndExpectedLength(int minLength, int expectedLength,
      Supplier<?> supplier) {
    return new SuppliedStringHolder(minLength, expectedLength, supplier);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying a
   * minimum of the estimated length.
   *
   * @param minLength The minimum length, must not be larger than the actual length.
   * @param expectedLength The expected length, may be larger than the actual length.
   * @param supplier The supplier (may throw an {@link IOException} upon {@link Supplier#get()},
   *          which is handled by the given exception handler).
   * @param onError The exception handler.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierMinimumAndExpectedLength(int minLength, int expectedLength,
      IOSupplier<?> supplier, IOExceptionHandler onError) {
    return new SuppliedStringHolder(minLength, expectedLength, supplier, onError);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying the
   * length the supplied string is going to have. An {@link IllegalStateException} will be thrown
   * once a string is supplied that does not match this length.
   *
   * @param fixedLength The exact length of the string.
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if fixedLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierFixedLength(int fixedLength, Supplier<?> supplier) {
    if (fixedLength == 0) {
      return SimpleStringHolder.EMPTY_STRING;
    }
    return new FixedLengthSuppliedStringHolder(fixedLength, supplier);
  }

  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, specifying the
   * length the supplied string is going to have. An {@link IllegalStateException} will be thrown
   * once a string is supplied that does not match this length.
   *
   * @param fixedLength The exact length of the string.
   * @param supplier The supplier (may throw an {@link IOException} upon {@link Supplier#get()},
   *          which is handled by the given exception handler).
   * @param onError The exception handler.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if fixedLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withSupplierFixedLength(int fixedLength, IOSupplier<?> supplier,
      IOExceptionHandler onError) {
    if (fixedLength == 0) {
      return SimpleStringHolder.EMPTY_STRING;
    }
    return new FixedLengthSuppliedStringHolder(fixedLength, supplier, onError);
  }

  /**
   * Constructs a {@link ReaderStringHolder} with the given Reader source.
   *
   * @param readerSupply The supply of {@link Reader} instances for the content.
   * @param onError The exception handler.
   * @return The {@link ReaderStringHolder}.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withReaderSupplier(IOSupplier<Reader> readerSupply,
      IOExceptionHandler onError) {
    return new ReaderStringHolder(0, 0, readerSupply, onError);
  }

  /**
   * Constructs a {@link ReaderStringHolder} with the given Reader source.
   *
   * @param minLen The minimum length of the content, must not be larger than the actual length.
   * @param readerSupply The supply of {@link Reader} instances for the content.
   * @param onError The exception handler.
   * @return The {@link ReaderStringHolder}.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withReaderSupplierMinimumLength(int minLen,
      IOSupplier<Reader> readerSupply, IOExceptionHandler onError) {
    return new ReaderStringHolder(minLen, minLen, readerSupply, onError);
  }

  /**
   * Constructs a {@link ReaderStringHolder} with the given Reader source.
   *
   * @param expectedLen The expected length of the content, which is only an estimate.
   * @param readerSupply The supply of {@link Reader} instances for the content.
   * @param onError The exception handler.
   * @return The {@link ReaderStringHolder}.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withReaderSupplierExpectedLength(int expectedLen,
      IOSupplier<Reader> readerSupply, IOExceptionHandler onError) {
    return new ReaderStringHolder(0, expectedLen, readerSupply, onError);
  }

  /**
   * Constructs a {@link ReaderStringHolder} with the given Reader source.
   *
   * @param minLen The minimum length of the content, must not be larger than the actual length.
   * @param expectedLen The expected length of the content, which is only an estimate.
   * @param readerSupply The supply of {@link Reader} instances for the content.
   * @param onError The exception handler.
   * @return The {@link ReaderStringHolder}.
   * @throws NullPointerException if supplier was {@code null}.
   */
  public static StringHolder withReaderSupplierMinimumAndExpectedLength(int minLen, int expectedLen,
      IOSupplier<Reader> readerSupply, IOExceptionHandler onError) {
    return new ReaderStringHolder(minLen, expectedLen, readerSupply, onError);
  }

  /**
   * Constructs a {@link StringHolder} with the given content.
   *
   * Unless the object already is a {@link StringHolder}, or is known to be empty, its contents are
   * converted to String. {@code null} objects are converted to {@code "null"}, in accordance with
   * {@link String#valueOf(Object)}.
   *
   * @param obj The object.
   * @return The {@link StringHolder} instance.
   */
  @SuppressWarnings("PMD.CognitiveComplexity")
  public static StringHolder withContent(Object obj) {
    if (obj == null) {
      return SimpleStringHolder.NULL_STRING;
    }
    if (obj instanceof String) {
      String s = (String) obj;
      StringHolder sh = SimpleStringHolder.COMMON_STRINGS.get(s);
      if (sh != null) {
        return sh;
      }
      return new SimpleStringHolder(s);
    } else if (obj instanceof StringHolder) {
      return (StringHolder) obj;
    } else {
      if (obj instanceof CharSequence) {
        if (CharSequenceReleaseShim.isEmpty((CharSequence) obj)) {
          return SimpleStringHolder.EMPTY_STRING;
        }
      } else {
        StringHolder sh = SimpleStringHolder.COMMON_STRINGS.get(obj);
        if (sh != null) {
          return sh;
        }
      }
      String s = String.valueOf(obj);
      StringHolder sh = SimpleStringHolder.COMMON_STRINGS.get(s);
      if (sh != null) {
        return sh;
      }
      return new SimpleStringHolder(s);
    }
  }

  /**
   * Returns the current minimum length of the expected string length in this {@link StringHolder}.
   *
   * This is equivalent to {@link #length()} if {@link #isString()} is {@code true}.
   *
   * NOTE: When using this parameter for optimizations (e.g., to speed-up equality checks), make
   * sure to also check {@link #checkError()}. When that method returns {@code true}, the minimum
   * length can actually not be guaranteed.
   *
   * @return The minimum length (but be sure to see {@link #checkError()}).
   */
  @Override
  public final int getMinimumLength() {
    return minLength;
  }

  /**
   * Returns the current estimate of the length of the string in this {@link StringHolder}, which is
   * at least the {@link #getMinimumLength()} but could be substantially larger.
   *
   * This is equivalent to {@link #length()} if {@link #isString()} is {@code true}.
   *
   * @return The currently expected length.
   */
  @Override
  public final int getExpectedLength() {
    return expectedLength;
  }

  /**
   * Updates the current estimate of the length of the string in this {@link StringHolder}.
   *
   * The value will be rounded to {@link #getMinimumLength()} if necessary.
   *
   * @param len The new expected length
   */
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
        sc.resizeBy(this, this.minLength - oldMin, this.expectedLength - oldExpected);
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
        sc.resizeBy(this, this.minLength - oldMin, this.expectedLength - oldExpected);
      } catch (RuntimeException | Error e) {
        setError();
        throw e;
      }
    }
  }

  /**
   * Returns the actual length of this instance's contents. This may trigger a conversion to
   * {@link String}.
   *
   * @return The actual length.
   */
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

  /**
   * Checks if this holder is currently backed by a plain {@link String}.
   *
   * @return {@code true} if currently backed by a plain {@link String}.
   */
  public final boolean isString() {
    return theString != null;
  }

  /**
   * Checks if this {@link StringHolder} is known to yield an empty {@link String}.
   *
   * @return {@code true} if known non-empty.
   */
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

  /**
   * Returns {@code true} if the actual length is known, i.e. {@link #getMinimumLength()} {@code ==}
   * {@link #getExpectedLength()} {@code == } {@link #length()}.
   *
   * By default, this is only true if {@link #isString()} {@code == true}, but subclasses may
   * override this check. When they do, they must include a check for {@code ||}
   * {@link #isString()}.
   *
   * @return {@code true} if the length in this holder is known.
   * @see #isKnownEmpty()
   */
  @Override
  public boolean isLengthKnown() {
    return isString();
  }

  @Override
  public final int hashCode() {
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
    } else {
      return toString().equals(s);
    }
  }

  private boolean equalsStringHolder(StringHolder obj) {
    if (isString()) {
      if (!obj.checkError() && length() < obj.getMinimumLength()) {
        return false;
      }
    } else if (obj.isString()) {
      if (!checkError() && obj.length() < getMinimumLength()) {
        return false;
      }
    }

    return obj.equalsString(toString());
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Appendable}; this may or
   * may not turn the contents of this instance into a String.
   *
   * @param out The target.
   * @throws IOException on error.
   */
  public final void appendTo(Appendable out) throws IOException {
    appendToAndReturnLength(out);
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuilder}; this may
   * or may not turn the contents of this instance into a String.
   *
   * @param out The target.
   */
  public final void appendTo(StringBuilder out) {
    appendToAndReturnLength(out);
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuffer}; this may or
   * may not turn the contents of this instance into a String.
   *
   * @param out The target.
   */
  public final void appendTo(StringBuffer out) {
    appendToAndReturnLength(out);
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Writer}; this may or may
   * not turn the contents of this instance into a String.
   *
   * @param out The target.
   * @throws IOException on error.
   */
  public final void appendTo(Writer out) throws IOException {
    appendToAndReturnLength(out);
  }

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Appendable}, and returns
   * the number of characters appended. This call may or may not turn the contents of this instance
   * into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   * @throws IOException on error.
   */
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

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuilder}, and
   * returns the number of characters appended. This call may or may not turn the contents of this
   * instance into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   */
  public final int appendToAndReturnLength(StringBuilder out) {
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
   * Append the contents of this {@link StringHolder} to the given {@link StringBuffer}, and returns
   * the number of characters appended. This call may or may not turn the contents of this instance
   * into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   */
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

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Writer}, and returns the
   * number of characters appended. This call may or may not turn the contents of this instance into
   * a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   * @throws IOException on error.
   */
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
    out.append(s);
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
    out.append(s);
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
    out.append(s);
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
          theString = s = Objects.requireNonNull(getString());
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
   * returning it. This may be a good opportunity to see if we got what we wanted, setError, etc.
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
   */
  protected void uncache() {
    theString = null;
  }

  /**
   * Provides the contents of this {@link StringHolder} as a {@link Reader}.
   *
   * @return The reader.
   * @throws IOException on error.
   */
  public final Reader toReader() throws IOException {
    String s = theString;
    if (s != null) {
      return new StringReader(s);
    } else {
      return newReader();
    }
  }

  /**
   * Checks if this {@link StringHolder} had some kind of unexpected condition.
   *
   * If so, {@link #getMinimumLength()} may be adjusted to a value smaller than its previous state.
   *
   * @return {@code true} if trouble was detected.
   */
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
    return LazyInitReader.withSupplier(() -> new StringReader(StringHolder.this.toString()));
  }

  /**
   * Returns the {@link StringHolderScope} associated with this {@link StringHolder}, or
   * {@code null} if no scope was associated.
   *
   * @return The scope, or {@code null}.
   */
  public final StringHolderScope getScope() {
    return scope;
  }

  /**
   * Sets the {@link StringHolderScope} associated with this {@link StringHolder}. Any previously
   * associated scope is removed from this instance and returned.
   *
   * @param newScope The new scope, or {@code null}/{@link StringHolderScope#NONE} to set "no
   *          scope".
   * @return The old scope, or {@code null} if none was set before.
   */
  public final StringHolderScope updateScope(StringHolderScope newScope) {
    if (newScope == StringHolderScope.NONE) { // NOPMD
      newScope = null;
    }
    StringHolderScope oldScope = this.scope;
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

  /**
   * Returns something that can be used in {@link StringHolder#withContent(Object)} which then
   * yields the same output when calling {@link #toString()} on either instance.
   *
   * The returned object usually is this instance itself. However, this method may return a
   * simplified version of the content stored in this instance. For example, if the content already
   * is a string, the string is returned.
   *
   * @return The "content" of this instance, which could be the instance itself, or something else.
   * @see #withContent(Object)
   */
  public Object asContent() {
    if (isString()) {
      return toString();
    }
    return this;
  }

  @Override
  public int compareTo(Object o) {
    if (o instanceof StringHolder) {
      return compareTo((StringHolder) o);
    } else if (o instanceof CharSequence) {
      return compareTo((CharSequence) o);
    } else {
      throw new ClassCastException("Cannot compare " + o.getClass());
    }
  }

  /**
   * Narrower implementation of {@link #compareTo(Object)} for {@link String}s.
   *
   * @param o The other object.
   * @return The comparison result, as defined by {@link #compareTo(Object)}.
   */
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

  /**
   * Narrower implementation of {@link #compareTo(Object)} for {@link StringHolder}s.
   *
   * @param o The other object.
   * @return The comparison result, as defined by {@link #compareTo(Object)}.
   */
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
}
