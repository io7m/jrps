/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jrps.controller.api;

import com.io7m.jrps.parser.api.JRPSParserErrorReceiverType;

import java.util.Optional;

/**
 * The type of error receivers.
 */

public interface JRPSControllerErrorReceiverType
  extends JRPSParserErrorReceiverType
{
  /**
   * An error occurred whilst constructing a controller.
   *
   * @param module    The module that caused the error
   * @param file      The file in question, if any
   * @param resource  The resource in question, if any
   * @param message   The error message
   * @param exception The exception raised, if any
   */

  void onConfigurationError(
    Module module,
    Optional<String> file,
    Optional<String> resource,
    String message,
    Optional<Exception> exception);
}
