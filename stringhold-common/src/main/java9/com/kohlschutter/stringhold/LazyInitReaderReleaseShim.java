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
import java.util.Objects;

abstract class LazyInitReaderReleaseShim extends Reader {
  abstract class KickstartPlaceholderReleaseShim extends Reader {
    // since Java 10
    long transferTo(Writer out) throws IOException {
      return transfer(init(), out);
    }
  }

  // since Java 10
  long transferTo(Writer out) throws IOException {
    return transfer(currentReader(), out);
  }

  abstract Reader currentReader();

  abstract Reader init() throws IOException;

  private static long transfer(Reader in, Writer out) throws IOException {
    Objects.requireNonNull(out, "out");

    char[] buffer = new char[8192];

    long transferred = 0;
    int numRead;
    while ((numRead = in.read(buffer, 0, buffer.length)) >= 0) {
      out.write(buffer, 0, numRead);
      transferred += numRead;
    }
    return transferred;
  }
}
