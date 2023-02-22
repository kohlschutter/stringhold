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

import com.kohlschutter.stringhold.StringHolder;

import liqp.TemplateContext;
import liqp.blocks.Block;
import liqp.nodes.LNode;

/**
 * Defines a conditional section, which may be excluded from the final output.
 * <p>
 * Example: <pre><tt>
 * {% conditional set: someState %}
 * {% conditionally someState %}
 * Hello World
 * {% endconditionally %}
 * {% conditional clear: someState %} // remove line to include content from the above section
 * </pre></tt>
 * </p>
 * 
 * <b>IMPORTANT:</b> This requires the use of {@link StringHolderRenderTransformer}.
 *
 * @author Christian Kohlschütter
 * @see Conditional
 */
public final class Conditionally extends Block {
  static final String ENVMAP_CONDITIONAL_PREFIX = " stringhold.conditional.";
  static final String ENVMAP_SUPPLIED_PREFIX = " stringhold.conditional-supplied.";

  /**
   * Constructs a new "condtionally" {@link Block}.
   */
  public Conditionally() {
    super("conditionally");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {
    Object conditional = nodes[0].render(context);
    Object blockNode = nodes[1].render(context);

    String key = String.valueOf(conditional);
    if (key.isBlank()) {
      return null;
    }

    StringHolder sh = StringHolder.withSupplier(() -> {
      context.getEnvironmentMap().put(ENVMAP_SUPPLIED_PREFIX + key, true);
      return blockNode;
    });

    return StringHolder.withConditionalStringHolder(sh, (o) -> {
      Object val = context.getEnvironmentMap().get(ENVMAP_CONDITIONAL_PREFIX + key);

      return Boolean.valueOf(String.valueOf(val));
    });
  }
}
