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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SuppliedStringHolderTest {
  @Test
  public void testEmpty() throws Exception {
    assertEquals(StringHolder.withContent(""), StringHolder.withSupplier(() -> ""));
  }

  @Test
  public void testSimple() throws Exception {
    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplier(() -> "x"));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(0,
        () -> "x"));

    assertEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(1,
        () -> "x"));

    // we claim that the minimum length for the second value is 2, so this check fails
    assertNotEquals(StringHolder.withContent("x"), StringHolder.withSupplierMinimumLength(2,
        () -> "x"));
  }

  @Test
  public void testNull() throws Exception {
    assertEquals("", StringHolder.withSupplier(null).toString());
  }

  @Test
  public void testBadMinimumLength() throws Exception {
    // it is not permissible to specify a minimum length that is larger than the actual size
    assertThrows(IllegalStateException.class, () -> StringHolder.withSupplierMinimumLength(234,
        () -> "abc").toString());
  }

  @Test
  public void testExpectedLength() throws Exception {
    StringHolder sh = StringHolder.withSupplierMinimumAndExpectedLength(2, 7, () -> "hello");
    assertEquals(2, sh.getMinimumLength());
    assertEquals(7, sh.getExpectedLength());
    sh.toString();
    assertEquals(5, sh.getMinimumLength());
    assertEquals(5, sh.getExpectedLength());
  }
}
