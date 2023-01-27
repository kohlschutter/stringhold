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
package com.kohlschutter.stringhold;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread-safe {@link IOSupplier} that has a cached version of the first supply.
 *
 * @param <T> The supplied type.
 * @author Christian Kohlschütter
 */
public final class CachedIOSupplier<T> implements IOSupplier<T> {
  private final IOSupplier<T> supplier;

  private final AtomicBoolean usedCached = new AtomicBoolean();
  private T cached;

  /**
   * Creates a new {@link CachedIOSupplier} with the given cached item and the supplier for the
   * subsequent items.
   *
   * If the given cached item is not {@code null}, the first call to {@link #get()} will return the
   * cached item; any subsequent calls will use the given supplier.
   *
   * If the given cached item is {@code null}, the first call to {@link #get()} will use the given
   * supplier directly.
   *
   * @param cached The first supply, or {@code null}.
   * @param supplier The supplier for the subsequent item.
   */
  public CachedIOSupplier(T cached, IOSupplier<T> supplier) {
    this.cached = cached;
    this.supplier = supplier;
    if (cached == null) {
      usedCached.set(true);
    }
  }

  @SuppressWarnings("null")
  @Override
  public T get() throws IOException {
    if (usedCached.compareAndSet(false, true)) {
      T c = Objects.requireNonNull(cached);
      cached = null;
      return c;
    } else {
      return supplier.get();
    }
  }
}
