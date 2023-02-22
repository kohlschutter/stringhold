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

import com.kohlschutter.annotations.compiletime.ExcludeFromCodeCoverageGeneratedReport;

import liqp.ParseSettings;
import liqp.RenderSettings;
import liqp.blocks.Capture;

public final class StringholdLiqpSettings {

  @ExcludeFromCodeCoverageGeneratedReport
  private StringholdLiqpSettings() {
    throw new IllegalStateException("No instances");
  }

  public static final ParseSettings PARSE_SETTINGS = new ParseSettings.Builder() //
      .with(new Conditional()) //
      .with(new Conditionally()) //
      .build();

  public static final RenderSettings RENDER_SETTINGS = new RenderSettings.Builder() //
      .withRenderTransformer(StringHolderRenderTransformer.getSharedCacheInstance()) //
      .build();
}
