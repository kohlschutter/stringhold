package com.kohlschutter.stringhold;

import java.io.Reader;
import java.io.StringReader;

abstract class ReaderShim extends Reader {
  public static Reader nullReader() {
    return new StringReader("");
  }
}
