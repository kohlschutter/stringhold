package com.kohlschutter.stringhold;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
