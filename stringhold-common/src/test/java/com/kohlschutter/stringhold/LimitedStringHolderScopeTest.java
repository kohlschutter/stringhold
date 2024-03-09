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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class LimitedStringHolderScopeTest {
  private static final class QuotaExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public QuotaExceededException(String message) {
      super(message);
    }
  }

  @Test
  public void testNoLimits_strings() {
    LimitedStringHolderScope ls = LimitedStringHolderScope.withNoLimits();
    assertEquals(0, ls.getMinimumLength());
    assertEquals(0, ls.getExpectedLength());

    StringHolder sh = StringHolder.withContent("abc");
    assertNull(sh.getScope());
    sh.updateScope(ls);

    assertEquals(3, ls.getMinimumLength());
    assertEquals(3, ls.getExpectedLength());

    StringHolder.withContent("").updateScope(ls);

    assertEquals(3, ls.getMinimumLength());
    assertEquals(3, ls.getExpectedLength());

    StringHolder.withContent("defg").updateScope(ls);

    assertEquals(7, ls.getMinimumLength());
    assertEquals(7, ls.getExpectedLength());

    sh.updateScope(null);

    assertEquals(4, ls.getMinimumLength());
    assertEquals(4, ls.getExpectedLength());
  }

  @Test
  public void testNoLimits_supplier() {
    LimitedStringHolderScope ls = LimitedStringHolderScope.withNoLimits();

    StringHolder sh = StringHolder.withSupplierMinimumAndExpectedLength(3, 5, () -> "four");
    sh.updateScope(ls);

    assertEquals(3, ls.getMinimumLength());
    assertEquals(5, ls.getExpectedLength());

    sh.toString();

    assertEquals(4, ls.getMinimumLength());
    assertEquals(4, ls.getExpectedLength());
  }

  @Test
  public void testLimit_minLength() {
    LimitedStringHolderScope ls = LimitedStringHolderScope.withUpperLimitForMinimumLength(3, (
        sh) -> {
      throw new QuotaExceededException("Limit exceeded");
    });

    StringHolder.withContent("ab").updateScope(ls);

    assertEquals(2, ls.getMinimumLength());
    assertEquals(2, ls.getExpectedLength());

    StringHolder sh1 = StringHolder.withSupplierMinimumAndExpectedLength(0, 5, () -> "c");
    sh1.updateScope(ls);

    assertEquals(2, ls.getMinimumLength());
    assertEquals(7, ls.getExpectedLength());

    sh1.toString();

    assertEquals(3, ls.getMinimumLength());
    assertEquals(3, ls.getExpectedLength());

    StringHolder sh2 = StringHolder.withSupplierMinimumAndExpectedLength(0, 5, () -> "defg");
    sh2.updateScope(ls);

    assertEquals(3, ls.getMinimumLength());
    assertEquals(8, ls.getExpectedLength());

    assertThrows(QuotaExceededException.class, () -> sh2.toString());
  }

  @Test
  public void testLimit_minExpectedLength() {
    LimitedStringHolderScope ls = LimitedStringHolderScope.withUpperLimitForExpectedLength(3, (
        sh) -> {
      throw new QuotaExceededException("Limit exceeded");
    });

    StringHolder.withContent("ab").updateScope(ls);

    assertEquals(2, ls.getMinimumLength());
    assertEquals(2, ls.getExpectedLength());

    StringHolder sh1 = StringHolder.withSupplierMinimumAndExpectedLength(0, 5, () -> "c");
    assertThrows(QuotaExceededException.class, () -> sh1.updateScope(ls));
  }

  @Test
  public void testLimit_minAndExpectedLength() {
    LimitedStringHolderScope ls = LimitedStringHolderScope
        .withUpperLimitForMinimumAndExpectedLength(3, 30, (sh) -> {
          throw new QuotaExceededException("Limit exceeded");
        });

    StringHolder.withContent("ab").updateScope(ls);

    assertEquals(2, ls.getMinimumLength());
    assertEquals(2, ls.getExpectedLength());

    StringHolder sh1 = StringHolder.withSupplierMinimumAndExpectedLength(0, 300, () -> "");
    assertThrows(QuotaExceededException.class, () -> sh1.updateScope(ls));
  }

  @Test
  public void testLimit_badExpectedLengthAdd() {
    AtomicBoolean exceeded = new AtomicBoolean(false);
    LimitedStringHolderScope ls = LimitedStringHolderScope
        .withUpperLimitForMinimumAndExpectedLength(0, 10, (sh) -> exceeded.set(true));
    assertFalse(exceeded.get());
    StringHolder.withContent("One Mississippi, two Mississippi").updateScope(ls);
    assertTrue(exceeded.get());
  }

  @Test
  public void testLimit_badExpectedLengthResize() {
    AtomicBoolean exceeded = new AtomicBoolean(false);

    LimitedStringHolderScope ls = LimitedStringHolderScope
        .withUpperLimitForMinimumAndExpectedLength(0, 10, (sh) -> exceeded.set(true));
    StringHolder sh = StringHolder.withSupplier(() -> "One Mississippi, two Mississippi");
    sh.updateScope(ls);
    assertFalse(exceeded.get());
    sh.toString();
    assertTrue(exceeded.get());
  }

  private static class CustomResizeStringHolder extends AbstractStringHolder {

    CustomResizeStringHolder() {
      super(0);
    }

    void triggerResize() {
      resizeBy(10, 30);
    }

    @Override
    protected String getString() {
      fail("Should not be reached");
      return null;
    }
  }

  @Test
  public void testLimit_badExpectedLength_customResize_exceedsExpectedLengthLimit() {
    AtomicBoolean exceeded = new AtomicBoolean(false);

    LimitedStringHolderScope ls = LimitedStringHolderScope
        .withUpperLimitForMinimumAndExpectedLength(10, 20, (sh) -> exceeded.set(true));
    CustomResizeStringHolder sh = new CustomResizeStringHolder();
    sh.updateScope(ls);
    assertFalse(exceeded.get());
    sh.triggerResize();
    assertTrue(exceeded.get());
  }

  @Test
  public void testLimit_badExpectedLength_customResize_exceedsMinimumLengthLimit() {
    AtomicBoolean exceeded = new AtomicBoolean(false);

    LimitedStringHolderScope ls = LimitedStringHolderScope
        .withUpperLimitForMinimumAndExpectedLength(0, 10, (sh) -> exceeded.set(true));
    CustomResizeStringHolder sh = new CustomResizeStringHolder();
    sh.updateScope(ls);
    assertFalse(exceeded.get());
    sh.triggerResize();
    assertTrue(exceeded.get());
  }
}
