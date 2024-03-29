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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.PrimitiveIterator;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
@SuppressWarnings("PMD.ExcessivePublicCount")
public interface StringHolder extends CharSequence, HasLength, Comparable<Object>, Cloneable {
  /**
   * Constructs a new {@link StringHolder} with content from the given supplier, assuming a minimum
   * length of 0.
   *
   * @param supplier The supplier.
   * @return The {@link StringHolder} instance.
   * @throws IllegalStateException if minLength is negative.
   * @throws NullPointerException if supplier was {@code null}.
   */
  static StringHolder withSupplier(Supplier<?> supplier) {
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
  static StringHolder withSupplier(IOSupplier<?> supplier, IOExceptionHandler onError) {
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
  static StringHolder withSupplierMinimumLength(int minLength, Supplier<?> supplier) {
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
  static StringHolder withSupplierMinimumLength(int minLength, IOSupplier<?> supplier,
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
  static StringHolder withSupplierExpectedLength(int expectedLength, Supplier<?> supplier) {
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
  static StringHolder withSupplierExpectedLength(int expectedLength, IOSupplier<?> supplier,
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
  static StringHolder withSupplierMinimumAndExpectedLength(int minLength, int expectedLength,
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
  static StringHolder withSupplierMinimumAndExpectedLength(int minLength, int expectedLength,
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
  static StringHolder withSupplierFixedLength(int fixedLength, Supplier<?> supplier) {
    if (fixedLength == 0) {
      return CommonStrings.EMPTY_STRINGHOLDER;
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
  static StringHolder withSupplierFixedLength(int fixedLength, IOSupplier<?> supplier,
      IOExceptionHandler onError) {
    if (fixedLength == 0) {
      return CommonStrings.EMPTY_STRINGHOLDER;
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
  static StringHolder withReaderSupplier(IOSupplier<Reader> readerSupply,
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
  static StringHolder withReaderSupplierMinimumLength(int minLen, IOSupplier<Reader> readerSupply,
      IOExceptionHandler onError) {
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
  static StringHolder withReaderSupplierExpectedLength(int expectedLen,
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
  static StringHolder withReaderSupplierMinimumAndExpectedLength(int minLen, int expectedLen,
      IOSupplier<Reader> readerSupply, IOExceptionHandler onError) {
    return new ReaderStringHolder(minLen, expectedLen, readerSupply, onError);
  }

  /**
   * Constructs a new, empty {@link StringHolderSequence}.
   *
   * @return The new, empty sequence.
   */
  static StringHolderSequence newSequence() {
    return new StringHolderSequence();
  }

  /**
   * Constructs a new, empty {@link StringHolderSequence}.
   *
   * @param estimatedNumberOfAppends Estimated number of calls to
   *          {@link StringHolderSequence#append(Object)}, etc.
   *
   * @return The new, empty sequence.
   */
  static StringHolderSequence newSequence(int estimatedNumberOfAppends) {
    return new StringHolderSequence(estimatedNumberOfAppends);
  }

  /**
   * Constructs a new, empty async {@link StringHolderSequence}.
   *
   * {@link StringHolder}s are automatically converted upon append, with appends being run
   * asynchronously, using temporary intermediate storage, if possible/necessary.
   *
   * @return The new, empty async sequence.
   */
  static StringHolderSequence newAsyncSequence() {
    return new AsyncStringHolderSequence();
  }

  /**
   * Constructs a new, empty async {@link StringHolderSequence}.
   *
   * {@link StringHolder}s are automatically converted upon append, with appends being run
   * asynchronously, using temporary intermediate storage, if possible/necessary.
   *
   * @param executor The executor to use.
   *
   * @return The new, empty async sequence.
   */
  static StringHolderSequence newAsyncSequence(Executor executor) {
    return new AsyncStringHolderSequence(executor);
  }

  /**
   * Constructs a new, empty async {@link StringHolderSequence}.
   *
   * {@link StringHolder}s are automatically converted upon append, with appends being run
   * asynchronously, using temporary intermediate storage, if possible/necessary.
   *
   * @param estimatedNumberOfAppends Estimated number of calls to
   *          {@link StringHolderSequence#append(Object)}, etc.
   *
   * @return The new, empty async sequence.
   */
  static StringHolderSequence newAsyncSequence(int estimatedNumberOfAppends) {
    return new AsyncStringHolderSequence(estimatedNumberOfAppends);
  }

  /**
   * Constructs a new, empty async {@link StringHolderSequence}.
   *
   * {@link StringHolder}s are automatically converted upon append, with appends being run
   * asynchronously, using temporary intermediate storage, if possible/necessary.
   *
   * @param estimatedNumberOfAppends Estimated number of calls to
   *          {@link StringHolderSequence#append(Object)}, etc.
   * @param executor The executor to use.
   *
   * @return The new, empty sequence.
   */
  static StringHolderSequence newAsyncSequence(int estimatedNumberOfAppends, Executor executor) {
    return new AsyncStringHolderSequence(estimatedNumberOfAppends, executor);
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
  static StringHolder withContent(Object obj) {
    if (obj == null) {
      return CommonStrings.NULL_STRINGHOLDER;
    } else if (obj instanceof String) {
      String s = (String) obj;
      StringHolder sh = CommonStrings.lookup(s);
      if (sh != null) {
        return sh;
      }
      return new SimpleStringHolder(s);
    } else if (obj instanceof StringHolder) {
      return (StringHolder) obj;
    } else {
      if (obj instanceof CharSequence) {
        if (CharSequenceReleaseShim.isEmpty((CharSequence) obj)) {
          return CommonStrings.EMPTY_STRINGHOLDER;
        }
      } else {
        StringHolder sh = CommonStrings.lookup(obj);
        if (sh != null) {
          return sh;
        }
      }
      String s = String.valueOf(obj);
      StringHolder sh = CommonStrings.lookup(s);
      if (sh != null) {
        return sh;
      }
      return new SimpleStringHolder(s);
    }
  }

  /**
   * Constructs a {@link StringHolder} with the given content sequences, as if they had been
   * appended to a {@link StringHolderSequence}. Empty sequences are optimized.
   *
   * @param objects The objects to represent as a sequence; if {@code null}, a StringHolder of the
   *          string {@code "null"} is returned.
   * @return The {@link StringHolder} instance.
   * @see #withContent(Object)
   */
  static StringHolder withContent(Object... objects) {
    if (objects == null) {
      return CommonStrings.NULL_STRINGHOLDER;
    }
    switch (objects.length) {
      case 0:
        return CommonStrings.EMPTY_STRINGHOLDER;
      case 1:
        return withContent(objects[0]);
      default:
        // see below
    }
    StringHolderSequence seq = new StringHolderSequence(objects.length);
    for (Object obj : objects) {
      if (obj instanceof StringHolder) {
        if (((StringHolder) obj).isKnownEmpty()) {
          continue;
        }
      } else if (obj instanceof CharSequence && CharSequenceReleaseShim.isEmpty(
          (CharSequence) obj)) {
        continue;
      }
      seq.append(obj);
    }
    if (seq.numberOfAppends() == 0) {
      return CommonStrings.EMPTY_STRINGHOLDER;
    } else {
      return seq;
    }
  }

  /**
   * Constructs a conditional wrapper around the given {@link StringHolder}; the given
   * {@code include} supplier controls whether the {@link StringHolder} is included, or effectively
   * empty.
   *
   * The conditional supplier is called at most once; the check is delayed as much as possible.
   *
   * @param wrapped The wrapped {@link StringHolder}.
   * @param includePredicate Controls the inclusion of that {@link StringHolder}; {@code false}
   *          means "excluded".
   * @return the conditional {@link StringHolder}.
   */
  static StringHolder withConditionalStringHolder(StringHolder wrapped,
      Predicate<StringHolder> includePredicate) {
    return new ConditionalStringHolder(wrapped, includePredicate);
  }

  /**
   * Constructs a wrapper around the given {@link StringHolder}, marking it as "uncacheable" (i.e.,
   * {@link #isCacheable()} returning {@code false}, unless {@link #isString()} is {@code true}).
   *
   * @param wrapped The wrapped {@link StringHolder}.
   * @return the uncacheable {@link StringHolder}.
   */
  static StringHolder withUncacheableStringHolder(StringHolder wrapped) {
    return wrapped.isCacheable() ? withConditionalStringHolder(wrapped, (k) -> true) : wrapped;
  }

  @Override
  default boolean isEmpty() {
    return length() == 0;
  }

  /**
   * Checks if this holder is currently backed by a plain {@link String}.
   *
   * @return {@code true} if currently backed by a plain {@link String}.
   */
  boolean isString();

  /**
   * Provides the contents of this {@link StringHolder} as a {@link Reader}.
   *
   * @return The reader.
   * @throws IOException on error.
   */
  Reader toReader() throws IOException;

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Appendable}; this may or
   * may not turn the contents of this instance into a String.
   *
   * @param out The target.
   * @throws IOException on error.
   */
  void appendTo(Appendable out) throws IOException;

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuilder}; this may
   * or may not turn the contents of this instance into a String.
   *
   * @param out The target.
   */
  void appendTo(StringBuilder out);

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuffer}; this may or
   * may not turn the contents of this instance into a String.
   *
   * @param out The target.
   */
  void appendTo(StringBuffer out);

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Writer}; this may or may
   * not turn the contents of this instance into a String.
   *
   * @param out The target.
   * @throws IOException on error.
   */
  void appendTo(Writer out) throws IOException;

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Appendable}, and returns
   * the number of characters appended. This call may or may not turn the contents of this instance
   * into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   * @throws IOException on error.
   */
  int appendToAndReturnLength(Appendable out) throws IOException;

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuilder}, and
   * returns the number of characters appended. This call may or may not turn the contents of this
   * instance into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   */
  int appendToAndReturnLength(StringBuilder out);

  /**
   * Append the contents of this {@link StringHolder} to the given {@link StringBuffer}, and returns
   * the number of characters appended. This call may or may not turn the contents of this instance
   * into a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   */
  int appendToAndReturnLength(StringBuffer out);

  /**
   * Append the contents of this {@link StringHolder} to the given {@link Writer}, and returns the
   * number of characters appended. This call may or may not turn the contents of this instance into
   * a String.
   *
   * @param out The target.
   * @return The number of characters appended.
   * @throws IOException on error.
   */
  int appendToAndReturnLength(Writer out) throws IOException;

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
  int getMinimumLength();

  /**
   * Returns the current estimate of the length of the string in this {@link StringHolder}, which is
   * at least the {@link #getMinimumLength()} but could be substantially larger.
   *
   * This is equivalent to {@link #length()} if {@link #isString()} is {@code true}.
   *
   * @return The currently expected length.
   */
  @Override
  int getExpectedLength();

  /**
   * Updates the current estimate of the length of the string in this {@link StringHolder}.
   *
   * The value will be rounded to {@link #getMinimumLength()} if necessary.
   *
   * @param len The new expected length
   */
  void setExpectedLength(int len);

  /**
   * Returns the actual length of this instance's contents. This may trigger a conversion to
   * {@link String}.
   *
   * @return The actual length.
   */
  @Override
  int length();

  /**
   * Checks if this {@link StringHolder} is known to yield an empty {@link String}.
   *
   * @return {@code true} if known non-empty.
   */
  @Override
  boolean isKnownEmpty();

  /**
   * Returns {@code true} if the actual length is known, i.e. {@link #getMinimumLength()} {@code ==}
   * {@link #getExpectedLength()} {@code == } {@link #length()}.
   *
   * By default, this is only true if {@link #isString()} {@code == true}, but subclasses may
   * override this check. When they do, they must include a check for
   * {@code || super.isLengthKnown()}.
   *
   * Note that once a length is <em>known</em>, it cannot change (i.e., don't override this for
   * mutable objects like {@link StringHolderSequence}).
   *
   * @return {@code true} if the length in this holder is known.
   * @see #isKnownEmpty()
   */
  @Override
  boolean isLengthKnown();

  /**
   * Checks if this {@link StringHolder} had some kind of unexpected condition.
   *
   * If so, {@link #getMinimumLength()} may be adjusted to a value smaller than its previous state.
   *
   * @return {@code true} if trouble was detected.
   */
  boolean checkError();

  /**
   * Returns the {@link StringHolderScope} associated with this {@link StringHolder}, or
   * {@code null} if no scope was associated.
   *
   * @return The scope, or {@code null}.
   */
  StringHolderScope getScope();

  /**
   * Sets the {@link StringHolderScope} associated with this {@link StringHolder}. Any previously
   * associated scope is removed from this instance and returned.
   *
   * @param newScope The new scope, or {@code null}/{@link StringHolderScope#NONE} to set "no
   *          scope".
   * @return The old scope, or {@code null} if none was set before.
   */
  StringHolderScope updateScope(StringHolderScope newScope);

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
  Object asContent();

  @Override
  default int compareTo(Object o) {
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
  int compareTo(CharSequence o);

  /**
   * Narrower implementation of {@link #compareTo(Object)} for {@link StringHolder}s.
   *
   * @param o The other object.
   * @return The comparison result, as defined by {@link #compareTo(Object)}.
   */
  int compareTo(StringHolder o);

  /**
   * Checks if this {@link StringHolder} is <em>effectively immutable</em>.
   *
   * @return {@code true} if the contents aren't going to change.
   */
  boolean isEffectivelyImmutable();

  /**
   * Marks this instance as <em>effectively immutable</em>.
   */
  void markEffectivelyImmutable();

  /**
   * Checks if this {@link StringHolder} is cacheable. A cacheable {@link StringHolder} may be
   * probed early for its contents (length, hashCode, etc.). This is usually the case, except when a
   * {@link StringHolder} is involved that was supplied via
   * {@link StringHolder#withConditionalStringHolder(StringHolder, Predicate)} or via
   * {@link #withUncacheableStringHolder(StringHolder)}, and that StringHolder is not already a
   * string or otherwise supplied, for example. By convention, it should be cacheable whenever
   * {@link #isString()} is {@code true}.
   *
   * @return {@code true} if cacheable.
   */
  default boolean isCacheable() {
    return true;
  }

  /**
   * Deep-clones this {@link StringHolder}.
   *
   * @return The cloned instance.
   */
  StringHolder clone();

  /**
   * Returns the index within this string of the first occurrence of the specified
   * character/codepoint, or {@code -1} if not found.
   *
   * @param c The character/codepoint to look for.
   * @return The position, or {@code -1} if not found.
   */
  default int indexOf(int c) {
    return indexOf(c, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the specified
   * character/codepoint, starting with the given character offset, or {@code -1} if not found.
   *
   * @param c The character/codepoint to look for.
   * @param start The character offset.
   * @return The position, or {@code -1} if not found.
   */
  default int indexOf(int c, int start) {
    if (isString()) {
      return toString().indexOf(c, start);
    } else if (isKnownEmpty()) {
      return -1;
    }

    boolean isSurrogatePair = c > 0xFFFF;

    int i = 0;
    int next = -1;
    for (PrimitiveIterator.OfInt it = chars().skip(start).iterator(); next != -1 || it
        .hasNext(); i++) {
      int ch;
      if (next != -1) {
        ch = next;
        next = -1;
      } else {
        ch = it.nextInt();
      }
      if (isSurrogatePair && Character.isHighSurrogate((char) ch) && it.hasNext()) {
        char ch2 = (char) it.nextInt();
        if (Character.isLowSurrogate(ch2)) {
          ch = Character.toCodePoint((char) ch, ch2);
        } else {
          // We detected an invalid UTF-8 surrogate, restart search from here
          next = ch2;
        }
      }
      if (ch == c) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the index within this StringHolder of the first occurrence of the specified
   * CharSequence, or {@code -1} if not found.
   *
   * @param str The char sequence to look for.
   * @return The position, or {@code -1} if not found.
   */
  @SuppressWarnings("PMD.CognitiveComplexity")
  default int indexOf(CharSequence str) {
    return indexOf(str, 0);
  }

  /**
   * Returns the index within this StringHolder of the first occurrence of the specified
   * CharSequence, starting with the given character offset, or {@code -1} if not found.
   *
   * @param str The char sequence to look for.
   * @param start The character offset.
   * @return The position, or {@code -1} if not found.
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
  default int indexOf(CharSequence str, int start) {
    if (str == this) { // NOPMD.CompareObjectsWithEquals
      return start == 0 || isEmpty() ? 0 : -1;
    } else if (CharSequenceReleaseShim.isEmpty(str)) {
      return Math.min(start, length());
    } else if (isKnownEmpty()) {
      return -1;
    }

    if (isString()) {
      if (str instanceof String || //
          (str instanceof StringHolder && ((StringHolder) str).isString())) {
        return toString().indexOf(str.toString());
      }
    }

    int strLen = str.length();
    if (isLengthKnown() && length() < (strLen + start)) {
      return -1;
    }

    char firstChar = str.charAt(start);
    switch (strLen) {
      case 1:
        return indexOf(firstChar);
      case 2:
        char secondChar = str.charAt(start + 1);
        if (Character.isSurrogatePair(firstChar, secondChar)) {
          return indexOf(Character.toCodePoint(firstChar, secondChar), start);
        }
        break;
      default:
        // continue below
        break;
    }

    int max = length() - strLen - start;

    boolean found = false;
    loop : for (int i = start; i <= max; i++) {
      char myChar = charAt(i);
      if (myChar != firstChar) {
        // seek ahead
        while (++i <= max) { // NOPMD.AvoidReassigningLoopVariables
          if (charAt(i) == firstChar) {
            found = true;
            break;
          }
        }
      } else {
        found = true;
      }
      if (found) {
        int myPos = i + 1;
        int end = myPos + strLen - 1;
        for (int strPos = 1; myPos < end; myPos++, strPos++) {
          if (charAt(myPos) != str.charAt(strPos)) {
            continue loop;
          }
        }
        return i;
      }
    }
    return -1;
  }

  /**
   * Checks if this {@link StringHolder} contains the given {@link CharSequence}.
   *
   * @param s The char sequence to look for.
   * @return {@code true} if found (also if the sequence is empty).
   */
  default boolean contains(CharSequence s) {
    return indexOf(s) >= 0;
  }
}
