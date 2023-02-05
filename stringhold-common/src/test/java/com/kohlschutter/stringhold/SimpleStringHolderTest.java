package com.kohlschutter.stringhold;

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
}
