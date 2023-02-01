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
package com.kohlschutter.stringhold.liqp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kohlschutter.stringhold.StringHolder;

import liqp.ProtectionSettings;
import liqp.RenderSettings;
import liqp.Template;
import liqp.TemplateParser;

public class StringHolderRenderTransformerTest {
  @Test
  public void testPrerenderStringHolder() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringHolderRenderTransformer
            .getInstance()).build()).build();

    String json = "{\"array\" : [1,2,3] }";

    Template template = parser.parse("{% for item in array %}{{ item }}{% endfor %}");

    assertTrue(template.prerender(json) instanceof StringHolder,
        "Prerendered result of a for-loop should be a StringHolder");
  }

  @Test
  public void testPrerenderString() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringHolderRenderTransformer
            .getInstance()).build()).build();

    Template template = parser.parse("Hello World");

    assertTrue(template.prerender() instanceof String,
        "Prerendered result of a simple string should be a String");
  }

  @Test
  public void testPrerenderLengthExceeded() throws Exception {
    TemplateParser parser = new TemplateParser.Builder().withRenderSettings(
        new RenderSettings.Builder().withRenderTransformer(StringHolderRenderTransformer
            .getInstance()).build()).withProtectionSettings(new ProtectionSettings.Builder()
                .withMaxSizeRenderedString(2).build()).build();

    String json = "{\"array\" : [1,2,3] }";
    Template template = parser.parse("{% for item in array %}{{ item }}{% endfor %}");

    assertThrows(Exception.class, () -> template.prerender(json),
        "Exception should be thrown because string exceeds limit");
  }

}