package com.kohlschutter.stringhold.liqp;

import com.kohlschutter.stringhold.LimitedStringHolderScope;
import com.kohlschutter.stringhold.StringHolder;
import com.kohlschutter.stringhold.StringHolderScope;
import com.kohlschutter.stringhold.StringOnlySequence;

import liqp.RenderTransformer;
import liqp.RenderTransformer.ObjectAppender.Controller;
import liqp.TemplateContext;

/**
 * A {@link RenderTransformer} that uses {@link StringOnlySequence} instances for appending.
 * 
 * @author Christian Kohlschütter
 */
public final class StringsOnlyRenderTransformer implements RenderTransformer {
  private static final StringsOnlyRenderTransformer INSTANCE = new StringsOnlyRenderTransformer();

  private StringsOnlyRenderTransformer() {
  }

  public static StringsOnlyRenderTransformer getInstance() {
    return INSTANCE;
  }

  @Override
  public Controller newObjectAppender(TemplateContext context, int estimatedNumberOfAppends) {
    return new Controller() {
      private Object result = "";
      private ObjectAppender appender = (o) -> {
        StringHolder sh = StringHolder.withContent(o);
        result = sh;

        StringHolderScope scope = (StringHolderScope) context.getEnvironmentMap().computeIfAbsent(
            StringHolderRenderTransformer.SCOPE_KEY, (k) -> {
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
          StringOnlySequence seq = new StringOnlySequence(estimatedNumberOfAppends);
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
    return String.valueOf(obj);
  }
}
