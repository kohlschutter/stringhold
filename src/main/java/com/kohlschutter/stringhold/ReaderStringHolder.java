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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Objects;

import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;

/**
 * A {@link StringHolder} that is supplied from {@link Reader} instances of the same source.
 *
 * @author Christian Kohlschütter
 */
final class ReaderStringHolder extends StringHolder {
  private static final int BUFFER_SIZE = 8192;
  private final IOExceptionHandler onError;
  private final IOSupplier<Reader> readerSupply;

  /**
   * Constructs a {@link ReaderStringHolder} with the given Reader source.
   *
   * @param minLen The minimum length of the content, must not be larger than the actual length.
   * @param readerSupply The supply of {@link Reader} instances for the content.
   * @param onError The exception handler.
   */
  ReaderStringHolder(int minLen, int expectedLen, IOSupplier<Reader> readerSupply,
      IOExceptionHandler onError) {
    super(minLen, expectedLen);
    this.readerSupply = Objects.requireNonNull(readerSupply);
    this.onError = Objects.requireNonNull(onError);
  }

  @Override
  protected Reader newReader() {
    return LazyInitReader.withIOSupplier(readerSupply);
  }

  @Override
  protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
    try (Reader reader = newReader()) {
      int len = 0;

      char[] buf = new char[BUFFER_SIZE];
      CharBuffer cbuf = CharBuffer.wrap(buf);
      int count;
      while ((count = reader.read(buf)) > 0) {
        out.append(cbuf, 0, count);
        len += count;
        cbuf.clear();
      }

      return len;
    } catch (IOException e) {
      handleIOExceptionOnAppendable(out, e);
      throw e;
    }
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
  @Override
  protected int appendToAndReturnLengthImpl(Writer out) throws IOException {
    try (Reader reader = newReader()) {
      int len = 0;

      char[] buf = new char[BUFFER_SIZE];
      int count;
      while ((count = reader.read(buf)) > 0) {
        out.write(buf, 0, count);
        len += count;
      }
      return len;
    } catch (IOException e) {
      handleIOExceptionOnAppendable(out, e);
      throw e;
    }
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuilder out) {
    int len = 0;
    try (Reader reader = newReader()) {
      char[] buf = new char[BUFFER_SIZE];
      int count;
      while ((count = reader.read(buf)) > 0) {
        out.append(buf, 0, count);
        len += count;
      }
    } catch (IOException e) {
      len += handleIOExceptionOnAppendable(out, e);
    }
    return len;
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuffer out) {
    int len = 0;
    try (Reader reader = newReader()) {

      char[] buf = new char[BUFFER_SIZE];
      int count;
      while ((count = reader.read(buf)) > 0) {
        out.append(buf, 0, count);
        len += count;
      }
    } catch (IOException e) {
      len += handleIOExceptionOnAppendable(out, e);
    }

    return len;
  }

  private int handleIOExceptionOnAppendable(Appendable out, IOException e) {
    String s;

    ExceptionResponse resp = onError.onException(e);
    if (resp == ExceptionResponse.ILLEGAL_STATE) {
      throw new IllegalStateException("Unexpected IOException", e);
    }

    setError();

    switch (resp) {
      case EXCEPTION_MESSAGE:
        s = e.toString();
        try {
          out.append(s);
        } catch (IOException e1) {
          // ignore
        }
        return s.length();
      case STACKTRACE:
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        s = sw.toString();
        try {
          out.append(s);
        } catch (IOException e1) {
          // ignore
        }
        return s.length();
      default:
        // nothing
        return 0;
    }
  }

  @Override
  protected synchronized String getString() {
    StringBuilder sb = new StringBuilder();
    try (Reader r = newReader()) {
      readFully(r, sb);
    } catch (IOException e) {
      ExceptionResponse resp = onError.onException(e);
      if (resp == ExceptionResponse.ILLEGAL_STATE) {
        throw new IllegalStateException("Unexpected IOException", e);
      }

      setError();
      switch (resp) {
        case FLUSH:
          break;
        case EXCEPTION_MESSAGE:
          return e.toString();
        case STACKTRACE:
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          sb.append(sw.toString());
          break;
        default:
          return "";
      }
    }
    return sb.toString();
  }

  private static void readFully(Reader r, StringBuilder sb) throws IOException {
    char[] buf = new char[BUFFER_SIZE];
    int count;
    while ((count = r.read(buf)) > 0) {
      sb.append(buf, 0, count);
    }
  }
}
