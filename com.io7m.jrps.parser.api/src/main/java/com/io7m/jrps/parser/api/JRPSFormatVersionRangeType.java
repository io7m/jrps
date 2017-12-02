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

package com.io7m.jrps.parser.api;

import com.io7m.jrps.core.JRPSImmutableStyleType;
import org.immutables.value.Value;

/**
 * The type of format version ranges.
 */

@JRPSImmutableStyleType
@Value.Immutable
public interface JRPSFormatVersionRangeType
{
  /**
   * @return The inclusive lower bound of the range
   */

  @Value.Parameter
  JRPSFormatVersion lowerInclusive();

  /**
   * @return The inclusive upper bound of the range
   */

  @Value.Parameter
  JRPSFormatVersion upperInclusive();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (this.lowerInclusive().compareTo(this.upperInclusive()) > 0) {
      throw new IllegalArgumentException("Lower bound must be <= upper bound");
    }
  }
}
