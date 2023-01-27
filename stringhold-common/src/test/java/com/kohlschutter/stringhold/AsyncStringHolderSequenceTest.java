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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.Test;

public class AsyncStringHolderSequenceTest {

  @Test
  public void testStringsOnly() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();
    sgs.append("Hello").append(" ").append("World");
    assertEquals(3, sgs.numberOfAppends());

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testStringAndTwoHolders() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();
    sgs.append("Hello").append(StringHolder.withSupplier(() -> " ")).append(StringHolder
        .withSupplier(() -> "World"));
    assertEquals(3, sgs.numberOfAppends());

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testStringHoldersAndString() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder shKnownEmpty = StringHolder.withSupplier(() -> "");

    sgs.append(StringHolder.withSupplier(() -> "Hello")).append(shKnownEmpty).append(StringHolder
        .withSupplier(() -> " ")).append(StringHolder.withSupplier(() -> "")).append("World");
    assertEquals(5, sgs.numberOfAppends());

    shKnownEmpty.toString(); // trigger isKnownEmpty() after append

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testStringedStringHoldersAndString() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "Hello");
    StringHolder sh2 = StringHolder.withSupplier(() -> " ");

    sgs.append(sh1).append(sh2).append("World");
    assertEquals(3, sgs.numberOfAppends());

    sh1.toString();
    sh2.toString();

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testMixedStringHolders() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "l");
    StringHolder sh2 = StringHolder.withSupplier(() -> " ");
    StringHolder sh3 = StringHolder.withSupplier(() -> "World");

    sgs.append("H").append("e").append(sh1).append("lo").append(sh2).append(sh3);
    assertEquals(6, sgs.numberOfAppends());

    sh2.toString(); // only the second StringHolder has isString()==true

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testAppendToStringBuilder() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "l");
    StringHolder sh2 = StringHolder.withSupplier(() -> " ");
    StringHolder sh3 = StringHolder.withSupplier(() -> "World");

    sgs.append("H").append("e").append(sh1).append("lo").append(sh2).append(sh3);

    assertFalse(sgs.isString());
    StringBuilder sb = new StringBuilder();
    sgs.appendTo(sb);
    assertEquals("Hello World", sb.toString());
    assertFalse(sgs.isString());
  }

  @Test
  public void testAppendToStringBuffer() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "l");
    StringHolder sh2 = StringHolder.withSupplier(() -> " ");
    StringHolder sh3 = StringHolder.withSupplier(() -> "World");

    sgs.append("H").append("e").append(sh1).append("lo").append(sh2).append(sh3);

    assertFalse(sgs.isString());
    StringBuffer sb = new StringBuffer();
    sgs.appendTo(sb);
    assertEquals("Hello World", sb.toString());
    assertFalse(sgs.isString());
  }

  @Test
  public void testAppendToStringWriter() throws IOException {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    StringHolder sh1 = StringHolder.withSupplier(() -> "l");
    StringHolder sh2 = StringHolder.withSupplier(() -> " ");
    StringHolder sh3 = StringHolder.withSupplier(() -> "World");

    sgs.append("H").append("e").append(sh1).append("lo").append(sh2).append(sh3);

    assertFalse(sgs.isString());
    StringWriter sw = new StringWriter();
    sgs.appendTo(sw);
    assertEquals("Hello World", sw.toString());
    assertFalse(sgs.isString());
  }

  @Test
  public void testDependentHolders() throws Exception {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    Semaphore sema = new Semaphore(0);

    // sh1's content is only available after sh2's.
    StringHolder sh1 = StringHolder.withSupplier(() -> {
      try {
        sema.acquire();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
      return "Hello";
    }, IOExceptionHandler.ILLEGAL_STATE);

    StringHolder sh2 = StringHolder.withSupplier(() -> {
      CompletableFuture.runAsync(() -> sema.release());
      return "World";
    });

    sgs.append(sh1).append(" ").append(sh2);

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }

  @Test
  public void testDependentHoldersStringWriter() throws Exception {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence();

    Semaphore sema = new Semaphore(0);

    // sh1's content is only available after sh2's.
    StringHolder sh1 = StringHolder.withSupplier(() -> {
      try {
        sema.acquire();
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
      return "Hello";
    }, IOExceptionHandler.ILLEGAL_STATE);

    StringHolder sh2 = StringHolder.withSupplier(() -> {
      CompletableFuture.runAsync(() -> sema.release());
      return "World";
    });

    sgs.append(sh1).append(" ").append(sh2);

    StringWriter sw = new StringWriter();
    assertFalse(sgs.isString());
    sgs.appendTo(sw);
    assertEquals("Hello World", sw.toString());
    assertFalse(sgs.isString());
  }

  @Test
  public void testCustomExecutor() throws Exception {
    ExecutorService es = Executors.newSingleThreadExecutor();
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence(es);
    sgs.append(StringHolder.withSupplier(() -> "Hello")).append(" ").append(StringHolder
        .withSupplier(() -> "World"));

    assertEquals("Hello World", sgs.toString());
  }
  
  @Test
  public void testEstimatedNumberOfAppendsTooLow() {
    AsyncStringHolderSequence sgs = new AsyncStringHolderSequence(0);
    sgs.append("Hello").append(" ").append("World");
    assertEquals(3, sgs.numberOfAppends());

    assertFalse(sgs.isString());
    assertEquals("Hello World", sgs.toString());
    assertTrue(sgs.isString());
  }
}
