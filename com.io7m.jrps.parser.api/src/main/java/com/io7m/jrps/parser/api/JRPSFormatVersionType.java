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

import java.util.Objects;

/**
 * The type of format versions.
 */

@JRPSImmutableStyleType
@Value.Immutable
public interface JRPSFormatVersionType
  extends Comparable<JRPSFormatVersionType>
{
  /**
   * @return The format major version
   */

  @Value.Parameter
  int major();

  /**
   * @return The format minor version
   */

  @Value.Parameter
  int minor();

  @Override
  default int compareTo(
    final JRPSFormatVersionType o)
  {
    Objects.requireNonNull(o, "Other");

    final int rm = Integer.compareUnsigned(this.major(), o.major());
    if (rm == 0) {
      return Integer.compareUnsigned(this.minor(), o.minor());
    }
    return rm;
  }
}
