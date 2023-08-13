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
import liqp.TemplateParser.Builder;

/**
 * Some helper methods that can be useful when working with Liqp.
 * 
 * @author Christian Kohlschütter
 */
public final class StringholdLiqpHelper {

  @ExcludeFromCodeCoverageGeneratedReport(reason = "unreachable")
  private StringholdLiqpHelper() {
    throw new IllegalStateException("No instances");
  }

  /**
   * Configures the given {@link Builder}, adding {@link Conditionally} and {@link Conditional}, as
   * well as using the {@link StringHolderRenderTransformer} available through
   * {@link StringHolderRenderTransformer#getSharedCacheInstance()}..
   * 
   * @param builder The builder to configure.
   * @return The builder.
   */
  public static TemplateParser.Builder configure(TemplateParser.Builder builder) {
    return builder //
        .withInsertion(new Conditional()) //
        .withInsertion(new Conditionally()) //
        .withRenderTransformer(StringHolderRenderTransformer.getSharedCacheInstance());
  }

  /**
   * Creates a new {@link TemplateParser}, configured via {@link #configure(Builder)}.
   * 
   * @return The configured {@link TemplateParser}.
   */
  public static TemplateParser newConfiguredTemplateParser() {
    return configure(new TemplateParser.Builder()).build();
  }

  /**
   * Creates a new {@link TemplateParser}, configured via {@link #configure(Builder)}, adding
   * additional configuration as specified.
   * 
   * @param additionalConfiguration The additional configuration.
   * @return The new template parser.
   */
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
