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

import org.eclipse.jdt.annotation.Nullable;

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
 * <b>NOTE:</b> By default, conditionals are {@code true}. The behavior of declaring a conditional
 * tag within a conditionally block is currently undefined.
 *
 * @author Christian Kohlschütter
 * @see Conditionally
 */
public final class Conditional extends Tag {
  static final String ENVMAP_CONDITIONAL_PREFIX = " stringhold.conditional.";
  static final String ENVMAP_SUPPLIED_PREFIX = " stringhold.conditional-supplied.";

  /**
   * Constructs a new "conditional" {@link Tag}.
   */
  public Conditional() {
    super("conditional");
  }

  @SuppressWarnings("PMD.UnnecessaryBoxing")
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
        return isConditionalSet(context.getEnvironmentMap(), key);
      default:
        throw new IllegalArgumentException("Illegal conditional command: " + command);
    }

    Map<String, Object> map = context.getEnvironmentMap();
    Boolean supplied = getConditionalSuppliedState(map, key);
    if (supplied != null && supplied.booleanValue() != b) {
      throw new IllegalStateException("Conditional already accessed: " + key);
    }

    setConditional(map, key, b);

    return null;
  }

  /**
   * Checks if the conditional identified by the given key is set.
   * 
   * @param envMap The environment map.
   * @param key The key, without its internal prefix.
   * @return {@code true} if set.
   */
  public static boolean isConditionalSet(Map<String, Object> envMap, String key) {
    Object val = envMap.get(Conditional.ENVMAP_CONDITIONAL_PREFIX + key);
    return Boolean.parseBoolean(String.valueOf(val));
  }

  /**
   * Checks if the conditional identified by the given key is set.
   * 
   * @param envMap The environment map.
   * @param key The key, without its internal prefix.
   * @param on {@code true} if set, {@code false} if clear.
   */
  public static void setConditional(Map<String, Object> envMap, String key, boolean on) {
    envMap.put(Conditional.ENVMAP_CONDITIONAL_PREFIX + key, on);
  }

  /**
   * Checks the "supplied" state of the conditional identified by the given key.
   * 
   * @param envMap The environment map.
   * @param key The key, without its internal prefix.
   * @return {@code false}/{@code true} for the "supply" condition once determined, or {@code null}
   *         if not determined yet.
   */
  public static @Nullable Boolean getConditionalSuppliedState(Map<String, Object> envMap,
      String key) {
    Object val = envMap.get(Conditional.ENVMAP_SUPPLIED_PREFIX + key);
    if (val == null) {
      return null;
    }
    return Boolean.valueOf(String.valueOf(val));
  }
}
