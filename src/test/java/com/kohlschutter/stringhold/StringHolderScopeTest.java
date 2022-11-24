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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.kohlschutter.stringhold.IOExceptionHandler.ExceptionResponse;

public class StringHolderScopeTest {
  private static class CountingScope implements StringHolderScope {
    int count;
    int error;

    @Override
    public void add(StringHolder sh) {
      ++count;
    }

    @Override
    public void remove(StringHolder sh) {
      --count;
    }

    @Override
    public void setError(StringHolder sh) {
      ++error;
    }

    @Override
    public void clearError(StringHolder sh) {
      --error;
    }

  }

  @Test
  public void testAddRemoveScope() throws Exception {
    CountingScope cs = new CountingScope();
    assertEquals(0, cs.count);
    StringHolder sh = StringHolder.withContent("test");
    assertNull(sh.getScope());
    assertEquals(0, cs.count);
    sh.updateScope(cs);
    assertEquals(1, cs.count);
    assertEquals(cs, sh.getScope());
    sh.updateScope(cs);
    assertEquals(1, cs.count);
    assertEquals(cs, sh.getScope());

    sh.updateScope(null);
    assertEquals(0, cs.count);
    assertNull(sh.getScope());
    sh.updateScope(StringHolderScope.NONE);
    assertNull(sh.getScope());
    assertEquals(0, cs.count);
  }

  @Test
  public void testAddRemoveScope_withSupplier() throws Exception {
    CountingScope cs = new CountingScope();
    assertEquals(0, cs.count);
    StringHolder sh = StringHolder.withSupplier(() -> "test");
    assertNull(sh.getScope());
    assertEquals(0, cs.count);
    sh.updateScope(cs);
    assertEquals(1, cs.count);
    assertEquals(cs, sh.getScope());
    sh.updateScope(cs);
    assertEquals(1, cs.count);
    assertEquals(cs, sh.getScope());

    // trigger StringHolderScope#resizeBy, which is a no-op here
    sh.toString();

    sh.updateScope(null);
    assertEquals(0, cs.count);
    assertNull(sh.getScope());
    sh.updateScope(null);
    assertNull(sh.getScope());
    assertEquals(0, cs.count);
  }

  @Test
  public void testScopeThatRefusesCertainAdds() throws Exception {
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
        if (!sh.isLengthKnown()) {
          throw new UnsupportedOperationException("I don't like uncertainty");
        }
      }

      @Override
      public void remove(StringHolder sh) {
      }
    };

    StringHolder sh = StringHolder.withContent("test");
    sh.updateScope(sc);

    StringHolder shLengthNotKnown = StringHolder.withSupplier(() -> "Hello");
    assertThrows(UnsupportedOperationException.class, () -> shLengthNotKnown.updateScope(sc));
  }

  @Test
  public void testScopeThatCannotBeRemoved() throws Exception {
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
        throw new UnsupportedOperationException("I don't like to be removed");
      }
    };

    StringHolder sh = StringHolder.withContent("test");
    sh.updateScope(sc);
    assertThrows(UnsupportedOperationException.class, () -> sh.updateScope(null));
  }

  @Test
  public void testScopeThatDoesntLikeResizeBy() throws Exception {
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
      }

      @Override
      public void resizeBy(StringHolder sh, int minLengthBy, int expectedLengthBy) {
        if (expectedLengthBy > 10) {
          throw new UnsupportedOperationException("I don't like large resizes");
        }
      }
    };

    StringHolder.withContent("test").updateScope(sc);

    StringHolder.withContent("One, Two, Mississippi").updateScope(sc);
    StringHolder.withSupplier(() -> "One, Two, Mississippi").updateScope(sc);

    StringHolder sh = StringHolder.withSupplier(() -> "One, Two, Mississippi");
    sh.updateScope(sc);

    // toString triggers recalculation
    assertThrows(UnsupportedOperationException.class, () -> sh.toString());
  }

  private static final class ResizingStringHolder extends StringHolder {

    protected ResizingStringHolder() {
      super(10);
    }

    void triggerResize() {
      resizeBy(20, 30);
    }

    @Override
    protected String getString() {
      fail("Should not be reachable");
      return "";
    }

  }

  @Test
  public void testScope_customStringHolder() throws Exception {
    ResizingStringHolder rsh = new ResizingStringHolder();

    AtomicBoolean triggered = new AtomicBoolean(false);
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
      }

      @Override
      public void resizeBy(StringHolder sh, int minLengthBy, int expectedLengthBy) {
        triggered.set(true);
      }
    };

    rsh.updateScope(sc);
    rsh.triggerResize();
    assertTrue(triggered.get());
  }

  @Test
  public void testScopeThatDoesntLikeResizeBy_customStringHolder() throws Exception {
    ResizingStringHolder rsh = new ResizingStringHolder();

    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
      }

      @Override
      public void resizeBy(StringHolder sh, int minLengthBy, int expectedLengthBy) {
        throw new UnsupportedOperationException("I don't like resizes");
      }
    };

    rsh.updateScope(sc);
    assertThrows(UnsupportedOperationException.class, () -> rsh.triggerResize());
  }

  @Test
  public void testScopeThatDoesntLikeResizeBy_Reader() throws Exception {
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
      }

      @Override
      public void resizeBy(StringHolder sh, int minLengthBy, int expectedLengthBy) {
        if (expectedLengthBy > 10) {
          throw new UnsupportedOperationException("I don't like large resizes");
        }
      }
    };

    StringWriter sw = new StringWriter();

    StringHolder sh = ReaderStringHolder.withReaderSupplier(() -> new StringReader(
        "One, Two, Mississippi"), (e) -> ExceptionResponse.EMPTY);
    sh.updateScope(sc);

    assertThrows(UnsupportedOperationException.class, () -> sh.appendTo(sw));

    // The append happens before resizeBy gets called
    assertEquals("One, Two, Mississippi", sw.toString());
  }

  private static final class BrokenStringHolder extends StringHolder {
    @Override
    protected String getString() {
      // TODO Auto-generated method stub
      return null;
    }

    void setErrorState(boolean b) {
      if (b) {
        setError();
      } else {
        clearError();
      }
    }
  }

  @Test
  public void testError() throws Exception {
    CountingScope sc = new CountingScope();

    BrokenStringHolder bsh = new BrokenStringHolder();
    bsh.updateScope(sc);

    assertEquals(0, sc.error);
    bsh.setErrorState(true);
    assertEquals(1, sc.error);
    bsh.setErrorState(false);
    assertEquals(0, sc.error);
  }

  @Test
  public void testErrorDefaultImpl() throws Exception {
    StringHolderScope sc = new StringHolderScope() {

      @Override
      public void add(StringHolder sh) {
      }

      @Override
      public void remove(StringHolder sh) {
      }
    };

    BrokenStringHolder bsh = new BrokenStringHolder();
    bsh.updateScope(sc);

    bsh.setErrorState(true);
    bsh.setErrorState(false);
  }
}
