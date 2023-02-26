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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;

public class LazyInitReaderTest {
  private static final class CountingSupplier<T> implements Supplier<T> {
    private final Supplier<T> supp;
    private int count = 0;

    CountingSupplier(Supplier<@NonNull T> supp) {
      this.supp = Objects.requireNonNull(supp);
    }

    @Override
    public T get() {
      ++count;
      return supp.get();
    }

    int getCount() {
      return count;
    }
  }

  @Test
  public void testCloseBeforeInit() throws Exception {
    IOSupplier<Reader> badIOSupplier = () -> {
      throw new IOException();
    };

    LazyInitReader.withIOSupplier(badIOSupplier).close();
  }

  @Test
  public void testBadIOSupplier() throws Exception {
    IOSupplier<Reader> badIOSupplier = () -> {
      throw new IOException();
    };

    try (Reader r = LazyInitReader.withIOSupplier(badIOSupplier)) {
      assertThrows(IOException.class, () -> r.read());
    }
  }

  @Test
  public void testBufferedReaderFromStringReader() throws Exception {
    Supplier<Reader> supplier = () -> new StringReader("hello");

    try (BufferedReader r = new BufferedReader(LazyInitReader.withSupplier(supplier))) {
      assertEquals("hello", r.readLine());
    }
  }

  @Test
  public void testStringReader() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader("hello"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      assertEquals(0, supplier.getCount());
      assertFalse(r.isInitialized());
      assertEquals('h', (char) r.read());
      assertTrue(r.isInitialized());
      char[] cbuf = new char[4];
      assertEquals(2, r.read(cbuf, 1, 2));
      assertArrayEquals(new char[] {0, 'e', 'l', 0}, cbuf);
      CharBuffer cb = CharBuffer.allocate(4);
      r.read(cb);
      cb.flip();
      assertEquals(2, cb.remaining());
      assertEquals("lo", cb.toString());
      assertTrue(r.isInitialized());
    }
    assertEquals(1, supplier.getCount());
  }

  @Test
  public void testMarkReset() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (Reader r = LazyInitReader.withSupplier(supplier)) {
      assertTrue(r.markSupported()); // because StringReader
      r.read();
      r.read();
      r.mark(32);
      int expected = r.read();
      assertEquals('l', expected);
      r.read();
      r.read();
      r.read();
      assertEquals('t', r.read());
      r.reset();
      assertEquals('l', r.read());
      assertTrue(r.markSupported()); // because StringReader
    }
    assertEquals(1, supplier.getCount());
  }

  @Test
  public void testClose() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      assertFalse(r.isInitialized());
      assertTrue(r.ready());
      assertTrue(r.isInitialized());
      r.close();
      assertThrows(IOException.class, () -> r.ready());
      assertTrue(r.isInitialized());
    }
  }

  @Test
  public void testSkip() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      assertFalse(r.isInitialized());
      assertEquals(6, r.skip(6));
      assertTrue(r.isInitialized());
      assertEquals("there", new BufferedReader(r).readLine());
    }
  }

  @Test
  public void testCloseThenRead() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      assertFalse(r.isInitialized());
      r.close();
      assertFalse(r.isInitialized());
      assertThrows(IOException.class, () -> r.mark(1));
    }
  }

  @Test
  public void testMarkFirst() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      r.mark(3);
      assertEquals('h', r.read());
      r.reset();
      assertEquals('h', r.read());
    }
  }

  @Test
  public void testResetFirst() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      r.reset(); // StringReader doesn't complain about missing mark
    }
  }

  @Test
  public void testReadCharBufferFirst() throws Exception {
    CountingSupplier<Reader> supplier = new CountingSupplier<>(() -> new StringReader(
        "hello there"));
    CharBuffer cb = CharBuffer.allocate(32);
    try (LazyInitReader r = LazyInitReader.withSupplier(supplier)) {
      cb.limit(5);
      r.read(cb);
      cb.flip();
    }
    assertEquals("hello", cb.toString());
  }
}
