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
 * An exception handler to handle {@link IOException}s in String-building methods.
 *
 * @author Christian Kohlschütter
 */
@FunctionalInterface
public interface IOExceptionHandler {
  /**
   * Determines what to do when the given exception is caught.
   *
   * @param exception The exception
   * @return The expected behavior.
   */
  ExceptionResponse onException(IOException exception);

  /**
   * What to do when an exception is thrown.
   */
  enum ExceptionResponse {
    /**
     * Throw an IllegalStateException, as this exception should not have happened.
     */
    ILLEGAL_STATE,

    /**
     * Return what has been written so far.
     */
    FLUSH,

    /**
     * Return an empty string (do not flush if possible), or at least don't add anything new.
     */
    EMPTY,

    /**
     * Return the exception message (without stacktrace); if possible, do not flush/include what has
     * been written so far.
     */
    EXCEPTION_MESSAGE,

    /**
     * Return what has been written so far, and add the exception message with stack trace.
     */
    FLUSH_AND_ADD_EXCEPTION_MESSAGE_WITH_STACKTRACE
  }
}