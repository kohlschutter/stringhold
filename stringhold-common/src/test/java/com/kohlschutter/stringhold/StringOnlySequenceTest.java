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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class StringOnlySequenceTest {

  @Test
  public void testAppend() throws Exception {
    StringOnlySequence sos = new StringOnlySequence();
    sos.append("Hello");
    sos.append(StringHolder.withContent(" "));
    sos.append(StringHolder.withContent(""));
    sos.append(StringHolder.withSupplier(() -> "Wor"));
    sos.append(StringHolder.withSupplier(() -> ""));
    sos.append(StringHolder.withSupplier(() -> "ld"));

    assertEquals(sos, "Hello World");
  }

  @Test
  public void testClone() throws Exception {
    AtomicInteger supplies = new AtomicInteger(0);
    StringOnlySequence sos = new StringOnlySequence();
    sos.append(StringHolder.withSupplier(() -> {
      supplies.incrementAndGet();
      return "Hello";
    }));

    assertEquals("Hello", sos.clone().toString());
    assertEquals("Hello", sos.toString());

    // StringHolders are converted to string upon append to StringOnlySequence
    assertEquals(1, supplies.get());
  }
}
