package com.kohlschutter.stringhold.liqp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import liqp.ProtectionSettings;
import liqp.RenderSettings;
import liqp.Template;
import liqp.TemplateParser;

public class StringsOnlyRenderTransformerTest {
  @Test
  public void testPrerenderStringHolder() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringsOnlyRenderTransformer
            .getInstance()).build()).build();

    String json = "{\"array\" : [1,2,3] }";

    Template template = parser.parse("{% for item in array %}{{ item }}{% endfor %}");

    assertTrue(template.prerender(json) instanceof String,
        "Prerendered result of a for-loop should be a String");
  }

  @Test
  public void testPrerenderString() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringsOnlyRenderTransformer
            .getInstance()).build()).build();

    Template template = parser.parse("Hello World");

    assertTrue(template.prerender() instanceof String,
        "Prerendered result of a simple string should be a String");
  }

  @Test
  public void testPrerenderLengthExceeded() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringsOnlyRenderTransformer
            .getInstance()).build()).withProtectionSettings(new ProtectionSettings.Builder()
                .withMaxSizeRenderedString(2).build()).build();

    String json = "{\"array\" : [1,2,3] }";
    Template template = parser.parse("{% for item in array %}{{ item }}{% endfor %}");

    assertThrows(Exception.class, () -> template.prerender(json),
        "Exception should be thrown because string exceeds limit");
  }
}
