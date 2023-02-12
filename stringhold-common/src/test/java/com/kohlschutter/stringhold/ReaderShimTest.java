package com.kohlschutter.stringhold;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;

import org.junit.jupiter.api.Test;

class ReaderShimTest {

  @SuppressWarnings("static-access")
  @Test
  public void testShim() throws Exception {
    try (Reader r = new ReaderShim() {

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
      }

      @Override
      public void close() throws IOException {
      }
    }) {
      assertEquals(-1, r.read());
      assertEquals(-1, r.nullReader().read());
    }
  }

}
