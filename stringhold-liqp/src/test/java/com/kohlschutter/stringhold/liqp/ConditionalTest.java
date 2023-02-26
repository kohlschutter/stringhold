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
package com.kohlschutter.stringhold.liqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import liqp.ParseSettings;
import liqp.RenderSettings;
import liqp.Template;
import liqp.TemplateParser;

public class ConditionalTest {

  private static final TemplateParser newParser() {
    TemplateParser parser = new TemplateParser.Builder() //
        .withRenderSettings(new RenderSettings.Builder() //
            .withRenderTransformer(StringholdLiqpSettings.RENDER_SETTINGS.getRenderTransformer())
            .build()) //
        .withParseSettings(new ParseSettings.Builder() //
            .with(StringholdLiqpSettings.PARSE_SETTINGS) //
            .build()) //
        .build();

    return parser;
  }

  @Test
  public void testConditionallyDefault() throws Exception {
    Template template = newParser().parse(
        "Hello{% conditionally test %} World{% endconditionally %}");
    assertEquals("Hello", template.render());
  }

  @Test
  public void testConditionallySetAtEnd() throws Exception {
    Template template = newParser().parse(
        "Hello{% conditionally test %} World{% endconditionally %}{% conditional set: test %}");
    assertEquals("Hello World", template.render());
  }

  @Test
  public void testConditionallySetAtFront() throws Exception {
    Template template = newParser().parse(
        "{% conditional set: test %}Hello{% conditionally test %} World{% endconditionally %}");
    assertEquals("Hello World", template.render());
  }

  @Test
  public void testConditionallySetWithin() throws Exception {
    // the behavior of a conditional tag within a conditionally block is currently undefined.
    Template template = newParser().parse(
        "Hello{% conditionally test %}{% conditional set: test %} World{% endconditionally %}");
    template.render();
  }

  @Test
  public void testConditionallySetThenCleared() throws Exception {
    Template template = newParser().parse(
        "{% conditional set: test %}Hello{% conditionally test %} World{% endconditionally %}{% conditional clear: test %}");;
    assertEquals("Hello", template.render());
  }

  @Test
  public void testGet() throws Exception {
    Template template = newParser().parse(
        "{% conditional get: test %}{% conditional set: test %}{% conditional get: test %}{% conditional clear: test %}{% conditional get: test %}");
    assertEquals("falsetruefalse", template.render());
  }

  @Test
  public void testBadArg() throws Exception {
    TemplateParser parser = newParser();

    assertThrows(RuntimeException.class, () -> parser.parse("{% conditional %}").render());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("{% conditional get %}")
        .render());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("{% conditional yo %}")
        .render());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("{% conditional get yo %}")
        .render());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("{% conditional get: %}")
        .render());
    assertThrows(IllegalArgumentException.class, () -> parser.parse("{% conditional bork: bork %}")
        .render());

    assertThrows(RuntimeException.class, () -> parser.parse(
        "{% conditionally %}what{% endconditionally %}").render());
  }

  @Test
  public void testAlreadySupplied() throws Exception {
    TemplateParser parser = newParser();

    // "yo" was not set, so everything is OK
    assertEquals("", parser.parse(
        "{% capture test %}{% conditionally yo %}true{% endconditionally %}{% endcapture %}{{ test }}")
        .render());

    // "yo" was already cleared when we captured the string to a value, so everything is OK
    assertEquals("", parser.parse(
        "{% capture test %}{% conditionally yo %}true{% endconditionally %}{% endcapture %}{{ test }}{% conditional clear: yo %}")
        .render());

    // we already captured the value to a variable; setting the conditional will (currently) fail
    assertThrows(IllegalStateException.class, () -> parser.parse(
        "{% capture test %}{% conditionally yo %}true{% endconditionally %}{% endcapture %}{{ test }}{% conditional set: yo %}")
        .render());
  }

  @Test
  public void testSetConditional() throws Exception {
    TemplateParser parser = new TemplateParser.Builder() //
        .withRenderSettings(new RenderSettings.Builder() //
            .withRenderTransformer(StringholdLiqpSettings.RENDER_SETTINGS.getRenderTransformer())
            .withEnvironmentMapConfigurator((envMap) -> {
              Conditional.setConditional(envMap, "test", true);
            }) //
            .build()) //
        .withParseSettings(new ParseSettings.Builder() //
            .with(StringholdLiqpSettings.PARSE_SETTINGS) //
            .build()) //
        .build();

    Template template = parser.parse("Hello{% conditionally test %} World{% endconditionally %}");
    assertEquals("Hello World", template.render());
  }

  @Test
  public void testGetConditional() throws Exception {
    CompletableFuture<Map<String, Object>> envMapFuture = new CompletableFuture<>();

    TemplateParser parser = new TemplateParser.Builder() //
        .withRenderSettings(new RenderSettings.Builder() //
            .withRenderTransformer(StringholdLiqpSettings.RENDER_SETTINGS.getRenderTransformer())
            .withEnvironmentMapConfigurator(envMapFuture::complete) //
            .build()) //
        .withParseSettings(new ParseSettings.Builder() //
            .with(StringholdLiqpSettings.PARSE_SETTINGS) //
            .build()) //
        .build();

    Template template = parser.parse("{% conditional set: test %}");
    assertEquals("", template.renderToObject());
    Map<String, Object> envMap = envMapFuture.get(); // NOTE: only accessible upon renderToObject
    assertTrue(Conditional.isConditionalSet(envMap, "test"));
    assertFalse(Conditional.isConditionalSet(envMap, "somethingelse"));
  }
}
