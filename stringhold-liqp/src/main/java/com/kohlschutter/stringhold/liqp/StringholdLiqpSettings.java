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

import java.util.function.Function;

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

import liqp.TemplateParser;

public final class StringholdLiqpSettings {

  @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
  private StringholdLiqpSettings() {
    throw new IllegalStateException("No instances");
  }

  public static TemplateParser.Builder configure(TemplateParser.Builder builder) {
    return builder //
        .withInsertion(new Conditional()) //
        .withInsertion(new Conditionally()) //
        .withRenderTransformer(StringHolderRenderTransformer.getSharedCacheInstance());
  }

  public static TemplateParser newConfiguredTemplateParser() {
    return configure(new TemplateParser.Builder()).build();
  }

  public static TemplateParser newConfiguredTemplateParser(
      Function<TemplateParser.Builder, TemplateParser.Builder> additionalConfiguration) {
    TemplateParser.Builder builder = new TemplateParser.Builder();
    configure(builder);
    if (additionalConfiguration != null) {
      builder = additionalConfiguration.apply(builder);
    }
    return builder.build();
  }
}
