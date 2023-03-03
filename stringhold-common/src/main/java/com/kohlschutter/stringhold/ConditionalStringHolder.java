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
import java.io.Writer;
import java.util.function.Predicate;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;
import com.kohlschutter.annotations.compiletime.SuppressFBWarnings;

/**
 * A {@link StringHolder} that may conditionally be included/excluded.
 *
 * @author Christian Kohlschütter
 */
final class ConditionalStringHolder implements StringHolder {
  private StringHolder wrapped;
  private final Predicate<StringHolder> include;

  @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
  private Boolean excluded;

  /**
   * Constructs a conditional wrapper around the given {@link StringHolder}; the given
   * {@code include} supplier controls whether the {@link StringHolder} is included, or effectively
   * empty.
   *
   * @param wrapped The wrapped {@link StringHolder}.
   * @param include Controls the inclusion of that {@link StringHolder}; {@code false} means
   *          "excluded".
   */
  ConditionalStringHolder(StringHolder wrapped, Predicate<StringHolder> include) {
    this.wrapped = wrapped;
    this.include = include;
  }

  private boolean isKnownExcluded() {
    return excluded != null && excluded;
  }

  private synchronized boolean isExcluded() {
    if (excluded == null) {
      excluded = !include.test(this);
    }
    return excluded;
  }

  @Override
  public char charAt(int index) {
    if (isExcluded()) {
      throw new IndexOutOfBoundsException();
    }
    return wrapped.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (start == end) {
      return "";
    }
    if (isExcluded()) {
      throw new IndexOutOfBoundsException();
    }
    return wrapped.subSequence(start, end);
  }

  @Override
  public boolean isString() {
    if (excluded == null) {
      return false;
    }
    if (isKnownExcluded()) {
      return true;
    } else {
      return wrapped.isString();
    }
  }

  @Override
  public Reader toReader() throws IOException {
    if (isExcluded()) {
      return ReaderShim.nullReader();
    }
    return wrapped.toReader();
  }

  @Override
  public void appendTo(Appendable out) throws IOException {
    if (isExcluded()) {
      return;
    }
    wrapped.appendTo(out);
  }

  @Override
  public void appendTo(StringBuilder out) {
    if (isExcluded()) {
      return;
    }
    wrapped.appendTo(out);
  }

  @Override
  public void appendTo(StringBuffer out) {
    if (isExcluded()) {
      return;
    }
    wrapped.appendTo(out);
  }

  @Override
  public void appendTo(Writer out) throws IOException {
    if (isExcluded()) {
      return;
    }
    wrapped.appendTo(out);
  }

  @Override
  public int appendToAndReturnLength(Appendable out) throws IOException {
    if (isExcluded()) {
      return 0;
    }
    return wrapped.appendToAndReturnLength(out);
  }

  @Override
  public int appendToAndReturnLength(StringBuilder out) {
    if (isExcluded()) {
      return 0;
    }
    return wrapped.appendToAndReturnLength(out);
  }

  @Override
  public int appendToAndReturnLength(StringBuffer out) {
    if (isExcluded()) {
      return 0;
    }
    return wrapped.appendToAndReturnLength(out);
  }

  @Override
  public int appendToAndReturnLength(Writer out) throws IOException {
    if (isExcluded()) {
      return 0;
    }
    return wrapped.appendToAndReturnLength(out);
  }

  @Override
  public int getMinimumLength() {
    return 0;
  }

  @Override
  public int getExpectedLength() {
    if (isKnownExcluded()) {
      return 0;
    }
    return wrapped.getExpectedLength();
  }

  @Override
  public void setExpectedLength(int len) {
    if (isKnownExcluded()) {
      return;
    }
    wrapped.setExpectedLength(len);
  }

  @Override
  public int length() {
    if (isExcluded()) {
      return 0;
    }

    return wrapped.length();
  }

  @Override
  public boolean isKnownEmpty() {
    if (isKnownExcluded()) {
      return true;
    }

    return wrapped.isKnownEmpty();
  }

  @Override
  public boolean isLengthKnown() {
    if (isKnownExcluded()) {
      return true;
    }

    return wrapped.isLengthKnown();
  }

  @Override
  public boolean checkError() {
    return wrapped.checkError();
  }

  @Override
  public StringHolderScope getScope() {
    return wrapped.getScope();
  }

  @Override
  public StringHolderScope updateScope(StringHolderScope newScope) {
    return wrapped.updateScope(newScope);
  }

  @Override
  public Object asContent() {
    if (isExcluded()) {
      return "";
    }
    return wrapped.asContent();
  }

  @Override
  public int compareTo(CharSequence o) {
    if (isExcluded()) {
      if (CharSequenceReleaseShim.isEmpty(o)) {
        return 0;
      } else {
        return -1;
      }
    }

    return wrapped.compareTo(o);
  }

  @Override
  public int compareTo(StringHolder o) {
    if (isExcluded()) {
      if (o.isEmpty()) {
        return 0;
      } else {
        return -1;
      }
    }

    return wrapped.compareTo(o);
  }

  @Override
  public boolean isEffectivelyImmutable() {
    if (isKnownExcluded()) {
      return true;
    }
    return wrapped.isEffectivelyImmutable();
  }

  @Override
  public void markEffectivelyImmutable() {
    isExcluded(); // trigger init
    wrapped.markEffectivelyImmutable();
  }

  @Override
  public String toString() {
    if (isExcluded()) {
      return "";
    }
    return wrapped.toString();
  }

  @Override
  public boolean isCacheable() {
    return isString();
  }

  @ExcludeFromCodeCoverageGeneratedReport(reason = "exception unreachable")
  private ConditionalStringHolder cloneSuper() {
    try {
      return (ConditionalStringHolder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public StringHolder clone() {
    ConditionalStringHolder clone = cloneSuper();
    clone.wrapped = clone.wrapped.clone();
    return clone;
  }
}
