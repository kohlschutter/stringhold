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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;
import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;

class SuppliedStringHolder extends AbstractStringHolder {

  private static final IOExceptionHandler UNREACHABLE_EXCEPTION_HANDLER = new IOExceptionHandler() {

    @Override
    @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
    public ExceptionResponse onException(IOException exception) {
      return ExceptionResponse.ILLEGAL_STATE;
    }
  };

  private final IOSupplier<?> supplier;
  private final IOExceptionHandler onError;
  private AtomicBoolean supplied = new AtomicBoolean(false);
  private Object object = null;

  SuppliedStringHolder(int minLen, int expLen, Supplier<?> supplier) {
    this(minLen, expLen, Objects.requireNonNull(supplier, "supplier")::get,
        UNREACHABLE_EXCEPTION_HANDLER);
  }

  SuppliedStringHolder(int minLen, int expLen, IOSupplier<?> supplier,
      IOExceptionHandler exceptionHandler) {
    super(minLen, expLen);
    this.supplier = supplier;
    this.onError = exceptionHandler;
  }

  private Object getSuppliedObject() {
    if (!supplied.compareAndSet(false, true)) {
      return object;
    }

    try {
      object = supplier.get();
    } catch (IOException e) {

      ExceptionResponse resp = onError.onException(e);
      if (resp == ExceptionResponse.ILLEGAL_STATE) {
        throw new IllegalStateException("Unexpected IOException", e);
      }

      setError();

      switch (onError.onException(e)) {
        case FLUSH:
          object = "";
          break;
        case EXCEPTION_MESSAGE:
          object = e.toString();
          break;
        case STACKTRACE:
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          object = sw.toString();
          break;
        default:
          object = "";
          break;
      }
    }
    if (object instanceof StringHolder) {
      StringHolder sh = (StringHolder) object;
      resizeTo(sh.getMinimumLength(), sh.getExpectedLength());
      object = sh.asContent();
    }

    if (object instanceof String) {
      toString();
    }

    return object;
  }

  @Override
  public Object asContent() {
    if (isString()) {
      return toString();
    }
    Object o = object;
    if (o instanceof StringHolder) {
      return ((StringHolder) o).asContent();
    } else {
      return this;
    }
  }

  @Override
  public char charAt(int index) {
    if (!isString()) {
      Object sup = getSuppliedObject();
      if (sup instanceof CharSequence) {
        return ((CharSequence) sup).charAt(index);
      }
    }
    return super.charAt(index);
  }

  @Override
  protected Reader newReader() throws IOException {
    Object obj = getSuppliedObject();
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      return sh.toReader();
    } else {
      return super.newReader();
    }
  }

  @Override
  protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
    Object obj = getSuppliedObject();
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      return sh.appendToAndReturnLength(out);
    } else if (obj instanceof CharSequence) {
      CharSequence cs = (CharSequence) obj;
      if (CharSequenceReleaseShim.isEmpty(cs)) {
        return 0;
      } else {
        out.append(cs);
        return cs.length();
      }
    } else {
      return super.appendToAndReturnLengthDefaultImpl(out);
    }
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuilder out) {
    Object obj = getSuppliedObject();
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      return sh.appendToAndReturnLength(out);
    } else if (obj instanceof CharSequence) {
      CharSequence cs = (CharSequence) obj;
      if (CharSequenceReleaseShim.isEmpty(cs)) {
        return 0;
      } else {
        out.append(cs);
        return cs.length();
      }
    } else {
      return super.appendToAndReturnLengthImpl(out);
    }
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuffer out) {
    Object obj = getSuppliedObject();
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      return sh.appendToAndReturnLength(out);
    } else if (obj instanceof CharSequence) {
      CharSequence cs = (CharSequence) obj;
      if (CharSequenceReleaseShim.isEmpty(cs)) {
        return 0;
      } else {
        out.append(cs);
        return cs.length();
      }
    } else {
      return super.appendToAndReturnLengthImpl(out);
    }
  }

  @Override
  protected int appendToAndReturnLengthImpl(Writer out) throws IOException {
    Object obj = getSuppliedObject();
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;
      return sh.appendToAndReturnLength(out);
    } else if (obj instanceof CharSequence) {
      CharSequence cs = (CharSequence) obj;
      if (CharSequenceReleaseShim.isEmpty(cs)) {
        return 0;
      } else {
        out.append(cs);
        return cs.length();
      }
    } else {
      return super.appendToAndReturnLengthImpl(out);
    }
  }

  @Override
  protected int computeLength() {
    Object obj = getSuppliedObject();
    if (obj instanceof CharSequence) {
      return ((CharSequence) obj).length();
    }
    return super.computeLength();
  }

  @Override
  public boolean isLengthKnown() {
    if (super.isLengthKnown()) {
      return true;
    }
    Object obj = object;
    if (obj instanceof StringHolder) {
      return ((StringHolder) obj).isLengthKnown();
    } else {
      return (obj instanceof CharSequence);
    }
  }

  @Override
  protected String getString() {
    Object obj = object == null ? getSuppliedObject() : object;
    if (obj instanceof CharSequence) {
      return ((CharSequence) obj).toString();
    } else {
      return String.valueOf(obj);
    }
  }

  @Override
  public SuppliedStringHolder clone() {
    SuppliedStringHolder clone = (SuppliedStringHolder) super.clone();
    clone.supplied = new AtomicBoolean(supplied.get());
    return clone;
  }
}
