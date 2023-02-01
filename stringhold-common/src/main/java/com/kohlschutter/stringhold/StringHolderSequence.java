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
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An {@link Appendable} sequence of strings or {@link StringHolder}s.
 *
 * @author Christian Kohlschütter
 */
public class StringHolderSequence extends StringHolder implements Appendable {
  final List<Object> sequence;

  /**
   * Constructs a new, empty {@link StringHolderSequence}.
   */
  public StringHolderSequence() {
    this(10);
  }

  /**
   * Constructs a new, empty {@link StringHolderSequence}.
   *
   * @param estimatedNumberOfAppends Estimated number of calls to {@link #append(Object)}, etc.
   */
  public StringHolderSequence(int estimatedNumberOfAppends) {
    super(0);
    sequence = new ArrayList<>(estimatedNumberOfAppends);
  }

  /**
   * Checks if the to-be-appended {@link StringHolder} should be added as a {@link String} (with
   * conversion via {@link StringHolder#toString()}) instead of adding it directly.
   *
   * This is false by default for all objects. Subclasses may override this selectively. If all
   * objects should be converted to strings, use {@link StringOnlySequence}.
   *
   * @param sh The {@link StringHolder}.
   * @return {@code true} if it should be appended as a string.
   * @see StringOnlySequence
   */
  protected boolean needsStringConversion(StringHolder sh) {
    return false;
  }

  @Override
  public StringHolderSequence append(CharSequence csq, int start, int end) {
    if (end == start) {
      return this;
    }

    if (csq instanceof String) {
      append(((String) csq).substring(start, end));
    } else if (csq instanceof StringBuilder) {
      append(((StringBuilder) csq).substring(start, end));
    } else if (csq instanceof StringBuffer) {
      append(((StringBuffer) csq).substring(start, end));
    } else {
      append(csq.subSequence(start, end));
    }
    return this;
  }

  @Override
  public StringHolderSequence append(char c) {
    addSequence(String.valueOf(c));
    return this;
  }

  /**
   * Appends the given {@link String}, unless it is empty.
   *
   * @param s The string.
   * @return This instance.
   */
  public StringHolderSequence append(String s) {
    addSequence(s);
    return this;
  }

  private void addSequence(String o) {
    int len = o.length();
    if (len == 0) {
      return;
    }

    uncache();
    sequence.add(o);
    resizeBy(len, len);
  }

  /**
   * Appends the given {@link StringHolder}, unless it is known to be empty.
   *
   * @param s The string.
   * @return This instance.
   */
  public StringHolderSequence append(StringHolder s) {
    if (s.isKnownEmpty()) {
      return this;
    } else if (s.isString() || needsStringConversion(s)) {
      addSequence(s.toString());
      return this;
    }

    uncache();
    sequence.add(s);
    resizeBy(s.getMinimumLength(), s.getExpectedLength());

    return this;
  }

  /**
   * Appends the given {@link CharSequence}, unless it is known to be empty.
   *
   * @param s The string.
   */
  @Override
  public StringHolderSequence append(CharSequence s) {
    if (!CharSequenceReleaseShim.isEmpty(s)) {
      addSequence(String.valueOf(s));
    }
    return this;
  }

  /**
   * Appends all given objects.
   *
   * @param objects The objects to append.
   * @return This instance.
   */
  public StringHolderSequence appendAll(Object... objects) {
    Objects.requireNonNull(objects);

    for (Object obj : objects) {
      append(obj);
    }
    return this;
  }

  /**
   * Appends all given objects.
   *
   * @param objects The objects to append.
   * @return This instance.
   */
  public StringHolderSequence appendAll(Iterable<Object> objects) {
    Objects.requireNonNull(objects);

    for (Object obj : objects) {
      append(obj);
    }
    return this;
  }

  /**
   * Appends the given object, unless it is known to be empty.
   *
   * The object
   *
   * @param s The string.
   * @return This instance.
   */
  public StringHolderSequence append(Object s) {
    if (s instanceof StringHolder) {
      return append((StringHolder) s);
    } else if (s instanceof String) {
      return append((String) s);
    } else if (s instanceof CharSequence) {
      return append((CharSequence) s);
    } else {
      return append(String.valueOf(s));
    }
  }

  @Override
  protected int appendToAndReturnLengthDefaultImpl(Appendable out) throws IOException {
    int len = 0;
    for (Object obj : sequence) {
      if (obj instanceof StringHolder) {
        StringHolder holder = (StringHolder) obj;
        if (holder.isKnownEmpty()) {
          continue;
        }

        len += holder.appendToAndReturnLength(out);
      } else {
        String s = (String) obj;
        len += s.length();
        out.append(s);
      }
    }
    return len;
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuilder out) {
    out.ensureCapacity(out.length() + getMinimumLength());

    int len = 0;
    for (Object obj : sequence) {
      if (obj instanceof StringHolder) {
        StringHolder holder = (StringHolder) obj;
        if (holder.isKnownEmpty()) {
          continue;
        }

        len += holder.appendToAndReturnLength(out);
      } else {
        String s = (String) obj;
        len += s.length();
        out.append(s);
      }
    }
    return len;
  }

  @Override
  protected int appendToAndReturnLengthImpl(StringBuffer out) {
    out.ensureCapacity(out.length() + getMinimumLength());

    int len = 0;
    for (Object obj : sequence) {
      if (obj instanceof StringHolder) {
        StringHolder holder = (StringHolder) obj;
        if (holder.isKnownEmpty()) {
          continue;
        }

        len += holder.appendToAndReturnLength(out);
      } else {
        String s = (String) obj;
        len += s.length();
        out.append(s);
      }
    }
    return len;
  }

  /**
   * Appends to a list that only holds Strings or StringSuppliers, excluding
   * {@link StringHolderSequence}s, whose contents are flattened to the list.
   *
   * @param flatList The list to append to.
   * @return The minimum for the estimated length.
   */
  final int appendToFlatList(List<Object> flatList) {
    int len = 0;
    for (Object obj : sequence) {
      if (obj instanceof StringHolder) {
        StringHolder holder = (StringHolder) obj;
        if (holder.isKnownEmpty()) {
          continue;
        }

        if (obj instanceof StringHolderSequence) {
          len += ((StringHolderSequence) obj).appendToFlatList(flatList);
        } else {
          len += holder.getMinimumLength();
          flatList.add(obj);
        }
      } else {
        String s = (String) obj;
        len += s.length();
        flatList.add(s);
      }
    }
    return len;
  }

  @Override
  protected String getString() {
    StringBuilder sb = new StringBuilder(Math.max(16, getExpectedLength()));
    int len = appendToAndReturnLength(sb);

    final String s;
    sequence.clear();
    if (len == 0) {
      s = "";
    } else {
      s = sb.toString();
      sequence.add(s);
    }

    return s;
  }

  @Override
  protected Reader newReader() {
    if (sequence.isEmpty()) {
      return new StringReader("");
    }

    final List<Object> flatList = new ArrayList<>(sequence.size());
    int len = appendToFlatList(flatList);
    if (len == 0) {
      return new StringReader("");
    }

    return new StringSequenceReader(flatList.iterator());
  }

  private static final class StringSequenceReader extends Reader {
    private boolean closed = false;

    private String currentString = null;
    private int currentPos;

    private final Iterator<Object> flatObjectIterator;

    private StringSequenceReader(Iterator<Object> flatObjectIterator) {
      super();
      this.flatObjectIterator = flatObjectIterator;
    }

    private void ensureOpen() throws IOException {
      if (closed) {
        throw new IOException("Stream closed");
      }
    }

    @Override
    public boolean ready() throws IOException {
      ensureOpen();
      return true;
    }

    @Override
    public void close() throws IOException {
      closed = true;
    }

    int ensureObject() {
      if (currentString != null) {
        int len = currentString.length();
        if (currentPos >= len) {
          currentString = null;
          return ensureObject();
        } else {
          return len;
        }
      } else {
        while (flatObjectIterator.hasNext()) {
          currentString = flatObjectIterator.next().toString();
          currentPos = 0;
          int len = currentString.length();
          if (len > 0) {
            return len;
          }
        }
        currentString = null;
        return 0;
      }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      int currentLen = ensureObject();
      if (currentLen == 0) {
        return -1;
      }

      len = Math.min(currentLen - currentPos, Math.min(cbuf.length - off, len));
      currentString.getChars(currentPos, currentPos + len, cbuf, off);
      currentPos += len;
      return len;
    }

    @Override
    public int read() throws IOException {
      int currentLen = ensureObject();
      if (currentLen == 0) {
        return -1;
      }
      return currentString.charAt(currentPos++);
    }
  }

  /**
   * Returns the number of appends (calls to {@link #append(Object)}, etc.) made to this instance so
   * far, minus the number of calls that were decided avoidable (e.g., zero-length appends).
   *
   * @return The number of appends.
   */
  public int numberOfAppends() {
    return sequence.size();
  }

  /**
   * Returns a simplified version of the contents of this sequence, if possible.
   *
   * <ol>
   * <li>If the content is a string already, the string is returned.</li>
   * <li>If there's no element stored, an empty string is returned.</li>
   * <li>If there is only a single element stored, its content is returned as if
   * {@link #asContent()} was called on that element.</li>
   * </ol>
   */
  @Override
  public Object asContent() {
    if (isString()) {
      return toString();
    }
    if (sequence.isEmpty()) {
      return "";
    } else if (sequence.size() == 1) {
      Object obj = sequence.get(0);
      if (obj instanceof String) {
        return obj;
      }
      StringHolder sc = (StringHolder) obj;
      return sc.asContent();
    } else {
      return this;
    }
  }

  @Override
  @SuppressWarnings("PMD.CognitiveComplexity")
  public char charAt(int index) {
    int offset = 0;

    if (index < 0) {
      throw new IndexOutOfBoundsException();
    }

    for (Object obj : sequence) {
      final int len;

      if (obj instanceof StringHolder) {
        StringHolder sh = (StringHolder) obj;
        if (sh.isEmpty()) {
          len = 0;
        } else {
          if (index == offset) {
            return sh.charAt(index - offset);
          }
          len = sh.length();
          if (index < offset + len) {
            return sh.charAt(index - offset);
          }
        }
      } else {
        String s = (String) obj;
        len = s.length();
        if (index < offset + len) {
          return s.charAt(index - offset);
        }
      }
      offset += len;
    }

    throw new IndexOutOfBoundsException();
  }
}
