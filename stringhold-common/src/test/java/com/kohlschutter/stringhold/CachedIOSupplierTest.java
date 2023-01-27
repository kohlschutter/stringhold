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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;

public class CachedIOSupplierTest {

  @Test
  public void testCachedSupplier() throws Exception {
    StringReader sr = new StringReader("first");
    CachedIOSupplier<@NonNull StringReader> cs = new CachedIOSupplier<>(sr, () -> new StringReader(
        "next"));

    StringWriter sw;
    assertTrue(cs.get().transferTo(sw = new StringWriter()) == 5 && sw.toString().equals("first"));
    assertTrue(cs.get().transferTo(sw = new StringWriter()) == 4 && sw.toString().equals("next"));
    assertTrue(cs.get().transferTo(sw = new StringWriter()) == 4 && sw.toString().equals("next"));
  }

  private static StringReader nullReader() {
    return null;
  }

  @Test
  public void testNoCachedSupplier() throws Exception {
    @SuppressWarnings("null")
    CachedIOSupplier<@NonNull StringReader> cs = new CachedIOSupplier<>(nullReader(),
        () -> new StringReader("next"));

    StringWriter sw;
    assertTrue(cs.get().transferTo(sw = new StringWriter()) == 4 && sw.toString().equals("next"));
    assertTrue(cs.get().transferTo(sw = new StringWriter()) == 4 && sw.toString().equals("next"));
  }

}
