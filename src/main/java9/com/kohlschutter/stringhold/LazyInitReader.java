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
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A lazy-initialized {@link Reader} that is supplied with some Reader upon the first method call.
 *
 * If the first call is made to {@link #close()}, the underlying Reader is not initialized, and
 * subsequent calls to other methods will fail with an {@link IOException}.
 *
 * @author Christian Kohlschütter
 */
public final class LazyInitReader extends Reader {
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final IOSupplier<Reader> readerSupplier;

  private Reader in = new KickstartPlaceholder();

  private LazyInitReader(IOSupplier<Reader> readerSupplier) {
    super();
    this.readerSupplier = readerSupplier;
  }

  /**
   * Creates a lazily-initialized reader from the {@link Reader} supplied through given
   * {@link Supplier}.
   *
   * @param readerSupplier The supplier supplying the Reader.
   * @return The {@link LazyInitReader}.
   */
  public static LazyInitReader withSupplier(Supplier<Reader> readerSupplier) {
    return new LazyInitReader((IOSupplier<Reader>) () -> readerSupplier.get());
  }

  /**
   * Creates a lazily-initialized reader from the {@link Reader} supplied through given
   * {@link IOSupplier}.
   *
   * @param readerSupplier The supplier supplying the Reader.
   * @return The {@link LazyInitReader}.
   */
  public static LazyInitReader withIOSupplier(IOSupplier<Reader> readerSupplier) {
    return new LazyInitReader(readerSupplier);
  }

  /**
   * Checks if this instance has been initialized with the actual reader.
   *
   * This is typically {@code true} after the first call.
   *
   * @return {@code true} if initialized.
   */
  public boolean isInitialized() {
    return !(in instanceof KickstartPlaceholder);
  }

  private Reader init() throws IOException {
    if (closed.get()) {
      throw new IOException("Stream closed");
    }
    return (in = readerSupplier.get());
  }

  @Override
  public int read(CharBuffer target) throws IOException {
    return in.read(target);
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public int read(char[] cbuf) throws IOException {
    return in.read(cbuf);
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    return in.read(cbuf, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return in.skip(n);
  }

  @Override
  public boolean ready() throws IOException {
    return in.ready();
  }

  @Override
  public boolean markSupported() {
    return in.markSupported();
  }

  @Override
  public void mark(int readAheadLimit) throws IOException {
    in.mark(readAheadLimit);
  }

  @Override
  public void reset() throws IOException {
    in.reset();
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  // @Override
  // public long transferTo(Writer out) throws IOException {
  // return in.transferTo(out);
  // }

  private final class KickstartPlaceholder extends Reader {
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      return init().read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
      closed.set(true);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
      return init().read(target);
    }

    @Override
    public int read() throws IOException {
      return init().read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
      return init().read(cbuf);
    }

    @Override
    public long skip(long n) throws IOException {
      return init().skip(n);
    }

    @Override
    public boolean ready() throws IOException {
      return init().ready();
    }

    @Override
    public boolean markSupported() {
      try {
        return init().markSupported();
      } catch (IOException e) {
        return false;
      }
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
      init().mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
      init().reset();
    }

    // @Override
    // public long transferTo(Writer out) throws IOException {
    // return init().transferTo(out);
    // }
  }
}
