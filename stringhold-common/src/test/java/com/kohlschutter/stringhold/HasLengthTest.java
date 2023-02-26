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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HasLengthTest {

  @Test
  public void testKnownLength() throws Exception {
    String str = "Hello";

    HasKnownLength hl = str::length;
    assertEquals(5, hl.length());
    assertEquals(5, hl.getMinimumLength());
    assertEquals(5, hl.getExpectedLength());
    assertTrue(hl.isLengthKnown());
    assertTrue(!hl.isKnownEmpty());
    assertTrue(!hl.isEmpty());
  }

  @Test
  public void testLength() throws Exception {
    String str = "Hello";

    HasLength hl = str::length;
    assertEquals(5, hl.length());
    assertEquals(5, hl.getMinimumLength());
    assertEquals(5, hl.getExpectedLength());
    assertFalse(hl.isLengthKnown());
    assertTrue(!hl.isKnownEmpty());
    assertTrue(!hl.isEmpty());
  }

  @Test
  public void testKnownLengthEmpty() throws Exception {
    String str = "";

    HasKnownLength hl = str::length;
    assertEquals(0, hl.length());
    assertEquals(0, hl.getMinimumLength());
    assertEquals(0, hl.getExpectedLength());
    assertTrue(hl.isLengthKnown());
    assertTrue(hl.isKnownEmpty());
    assertTrue(hl.isEmpty());
  }

  @Test
  public void testLengthEmpty() throws Exception {
    String str = "";

    HasLength hl = str::length;
    assertEquals(0, hl.length());
    assertEquals(0, hl.getMinimumLength());
    assertEquals(0, hl.getExpectedLength());
    assertFalse(hl.isLengthKnown());
    assertFalse(hl.isKnownEmpty());
    assertTrue(hl.isEmpty());
  }

  @Test
  public void testMinimumLength() throws Exception {
    String str = "Hello";

    HasMinimumLength hl = str::length;
    assertEquals(5, hl.getMinimumLength());
    assertEquals(5, hl.getExpectedLength());
    assertFalse(hl.isLengthKnown());
  }

  @Test
  public void testExpectedLength() throws Exception {
    String str = "Hello";

    HasExpectedLength hl = str::length;
    assertEquals(5, hl.getExpectedLength());
    assertFalse(hl.isLengthKnown());
  }

  @Test
  public void testKnownNotEmpty() throws Exception {
    HasLength hl = new HasLength() {

      @Override
      public int length() {
        return 5;
      }

      @Override
      public boolean isLengthKnown() {
        return true;
      }

      @Override
      public int getMinimumLength() {
        return 0;
      }
    };
    assertFalse(hl.isKnownEmpty());
  }
}
