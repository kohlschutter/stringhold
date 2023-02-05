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

import java.util.WeakHashMap;

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

  private static final WeakHashMap<StringHolderSequence, StringHolderSequence> HOLDER_CACHE =
      new WeakHashMap<>();

  private static final StringHolderRenderTransformer INSTANCE = new StringHolderRenderTransformer(
      HOLDER_CACHE);

  private final WeakHashMap<StringHolderSequence, StringHolderSequence> holderCache;

  private StringHolderRenderTransformer(
      WeakHashMap<StringHolderSequence, StringHolderSequence> holderCache) {
    this.holderCache = holderCache;
  }

  /**
   * Returns the shared {@link StringHolderRenderTransformer} instance.
   * 
   * This instance shares a common cache for de-duplicating {@link StringHolderSequence}s, which may
   * help to significantly reduce heap allocation, at the cost of a slightly slower execution.
   * 
   * If you're using {@link StringHolderRenderTransformer} in a highly concurrent setting, see
   * {@link #newCachedInstance()} to reduce lock contention.
   * 
   * @return The instance.
   */
  public static StringHolderRenderTransformer getSharedCacheInstance() {
    return INSTANCE;
  }

  /**
   * Creates a new, cached {@link StringHolderRenderTransformer} instance.
   * 
   * This instance uses its own cache for de-duplicating {@link StringHolderSequence}s, which may
   * help to significantly reduce heap allocation, at the cost of a slightly slower execution.
   * 
   * @return The instance.
   */
  public static StringHolderRenderTransformer newCachedInstance() {
    return new StringHolderRenderTransformer(new WeakHashMap<>());
  }

  /**
   * Creates a new, uncached {@link StringHolderRenderTransformer} instance.
   * 
   * This instance doesn't use a cache for de-duplicating {@link StringHolderSequence}s, resulting
   * in an overall faster execution at the cost of heap allocations.
   * 
   * @return The instance.
   */
  public static StringHolderRenderTransformer newUncachedInstance() {
    return new StringHolderRenderTransformer(null);
  }

  @Override
  public Controller newObjectAppender(TemplateContext context, int estimatedNumberOfAppends) {
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

    return new Controller() {
      private Object result = "";
      private ObjectAppender appender = (o) -> {
        if (o instanceof StringHolder) {
          ((StringHolder) o).updateScope(scope);
        }
        result = o;

        appender = (o2) -> {
          StringHolderSequence seq = new StringHolderSequence(Math.max(3,
              estimatedNumberOfAppends));

          if (result instanceof StringHolder) {
            ((StringHolder) result).updateScope(StringHolderScope.NONE);
          }
          seq.updateScope(scope);
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
        if (obj instanceof StringHolder) {
          ((StringHolder) obj).updateScope(StringHolderScope.NONE);
        }
        appender.append(obj);
      }
    };
  }

  @Override
  public Object transformObject(TemplateContext context, Object obj) {
    if (obj instanceof StringHolder) {
      StringHolder sh = (StringHolder) obj;

      StringHolder o;
      if (sh instanceof StringHolderSequence && holderCache != null) {
        StringHolderSequence shs = (StringHolderSequence) sh;
        shs.markEffectivelyImmutable();
        shs.hashCode(); // pre-compute hashcode to reduce time under lock

        // de-duplicate identical StringHolderSequences
        synchronized (holderCache) {
          o = holderCache.computeIfAbsent(shs, (k) -> shs);
        }
      } else {
        o = sh;
      }

      return o.asContent();
    } else {
      return obj;
    }
  }
}
