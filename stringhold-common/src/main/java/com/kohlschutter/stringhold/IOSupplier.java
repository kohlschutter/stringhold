/*
 * stringhold
 *
 * Copyright 2022-2024 Christian Kohlschütter
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
package com.kohlschutter.stringhold;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * A {@link Supplier} that may throw an {@link IOException} upon {@link #get()}.
 *
 * @param <T> The supplied type.
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface IOSupplier<T> {
  /**
   * Gets a result.
   *
   * @return a result
   * @throws IOException on error.
   */
  T get() throws IOException;
}
