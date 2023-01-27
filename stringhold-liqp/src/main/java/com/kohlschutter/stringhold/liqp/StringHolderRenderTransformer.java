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

import com.kohlschutter.stringhold.LimitedStringHolderScope;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderScope;
import com.kohlschutter.stringhold.StringHolderSequence;

import liqp.RenderTransformer;
import liqp.RenderTransformer.ObjectAppender.Controller;
import liqp.TemplateContext;

/**
 * A {@link RenderTransformer} that uses {@link StringHolder} instances for appending.
 * 
 * @author Christian Kohlschütter
 */
public final class StringHolderRenderTransformer implements RenderTransformer {
  static final String SCOPE_KEY = StringHolderScope.class.getName();

  private static final StringHolderRenderTransformer INSTANCE = new StringHolderRenderTransformer();

  private StringHolderRenderTransformer() {
  }

  /**
   * Returns the {@link StringHolderRenderTransformer} singleton instance.
   * 
   * @return The instance.
   */
  public static StringHolderRenderTransformer getInstance() {
    return INSTANCE;
  }

  @Override
  public Controller newObjectAppender(TemplateContext context, int estimatedNumberOfAppends) {
    return new Controller() {
      private Object result = "";
      private ObjectAppender appender = (o) -> {
        StringHolder sh = StringHolder.withContent(o);
        result = sh;

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        StringHolderScope scope = (StringHolderScope) context.getEnvironmentMap().computeIfAbsent(
            SCOPE_KEY, (k) -> {
              int maxLen = context.getParser().getProtectionSettings().maxSizeRenderedString;
              if (maxLen != Integer.MAX_VALUE) {
                return LimitedStringHolderScope.withUpperLimitForMinimumLength(maxLen, () -> {
                  throw new RuntimeException("rendered string exceeds " + maxLen);
                });
              } else {
                return StringHolderScope.NONE;
              }
            });

        sh.updateScope(scope);

        appender = (o2) -> {
          StringHolderSequence seq = new StringHolderSequence(Math.max(3,
              estimatedNumberOfAppends));
          seq.updateScope(scope);

          // do not double-count
          ((StringHolder) result).updateScope(null);
          seq.append(result);

          result = seq;

          seq.append(o2);
          appender = seq::append;
        };
      };

      @Override
      public Object getResult() {
        return transformObject(context, result);
      }

      @Override
      public void append(Object obj) {
        appender.append(obj);
      }
    };
  }

  @Override
  public Object transformObject(TemplateContext context, Object obj) {
    if (obj instanceof StringHolder) {
      return ((StringHolder) obj).asContent();
    } else {
      return obj;
    }
  }
}
