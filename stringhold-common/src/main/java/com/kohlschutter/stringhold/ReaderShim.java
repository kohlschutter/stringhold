package com.kohlschutter.stringhold;

import java.io.Reader;

abstract class ReaderShim extends Reader {
  public static Reader nullReader() { // since Java 11
    return Reader.nullReader();
  }
}
