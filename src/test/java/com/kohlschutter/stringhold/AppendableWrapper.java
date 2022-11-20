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

/**
 * Wraps another {@link Appendable}. Used to forcibly de-optimize code when testing.
 * 
 * @author Christian Kohlschütter
 */
final class AppendableWrapper implements Appendable {
  private final Appendable out;

  AppendableWrapper(Appendable out) {
    this.out = out;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    out.append(csq, start, end);
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    out.append(c);
    return this;
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    out.append(csq);
    return this;
  }
}