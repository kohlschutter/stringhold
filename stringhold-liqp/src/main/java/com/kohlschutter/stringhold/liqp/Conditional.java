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

import java.util.Map;

import liqp.TemplateContext;
import liqp.nodes.LNode;
import liqp.tags.Tag;

/**
 * Sets/gets the conditional state for a certain conditional-key.
 * <p>
 * Examples: <pre><code>
 * {% conditional get: someState %} // false
 * {% conditional set: someState %} // no output
 * {% conditional get: someState %} // true
 * {% conditional clear: someState %} // no output
 * {% conditional get: someState %} // false
 * </code></pre>
 *
 * <b>NOTE:</b> The behavior of declaring a conditional tag within a conditionally block is
 * currently undefined.
 *
 * @author Christian Kohlschütter
 * @see Conditionally
 */
public final class Conditional extends Tag {

  /**
   * Constructs a new "conditional" {@link Tag}.
   */
  public Conditional() {
    super("conditional");
  }

  @Override
  public Object render(TemplateContext context, LNode... nodes) {
    String args = String.valueOf(nodes[0].render(context));

    String[] parts = args.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException(args);
    }

    String command = parts[0];
    String key = parts[1];

    boolean b;
    switch (command) {
      case "set":
        b = true;
        break;
      case "clear":
        b = false;
        break;
      case "get":
        return Boolean.valueOf(String.valueOf(context.getEnvironmentMap().get(
            Conditionally.ENVMAP_CONDITIONAL_PREFIX + key)));
      default:
        throw new IllegalArgumentException("Illegal conditional command: " + command);
    }

    Map<String, Object> map = context.getEnvironmentMap();
    Object existingValue = map.get(Conditionally.ENVMAP_SUPPLIED_PREFIX + key);
    if (existingValue != null && Boolean.valueOf(String.valueOf(existingValue)) != b) {
      throw new IllegalStateException("Conditional already accessed: " + key);
    }

    map.put(Conditionally.ENVMAP_CONDITIONAL_PREFIX + key, b);

    return null;
  }
}
