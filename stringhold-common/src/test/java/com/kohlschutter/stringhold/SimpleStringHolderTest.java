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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SimpleStringHolderTest {

  @Test
  public void testMarkImmutable() throws Exception {
    StringHolder sh = StringHolder.withContent("test");
    assertTrue(sh.isEffectivelyImmutable());
    sh.markEffectivelyImmutable();
    assertTrue(sh.isEffectivelyImmutable());
  }

  @Test
  public void testCacheable() throws Exception {
    StringHolder sh = StringHolder.withContent("test");
    assertTrue(sh.isCacheable());
  }

  @Test
  public void testClone() throws Exception {
    assertEquals("test", StringHolder.withContent("test").clone().toString());
  }
}
