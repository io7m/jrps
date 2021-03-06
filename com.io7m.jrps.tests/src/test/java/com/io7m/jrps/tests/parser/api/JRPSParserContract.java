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

package com.io7m.jrps.tests.parser.api;

import com.io7m.jrps.parser.api.JRPSParseError;
import com.io7m.jrps.parser.api.JRPSParserErrorReceiverType;
import com.io7m.jrps.parser.api.JRPSParserType;
import com.io7m.jrps.parser.api.JRPSResourceReceiverType;
import mockit.FullVerifications;
import mockit.Mocked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;

public abstract class JRPSParserContract
{
  protected abstract JRPSParserType createParser(
    URI uri,
    InputStream stream,
    JRPSResourceReceiverType res,
    JRPSParserErrorReceiverType error)
    throws Exception;

  protected abstract Logger log();

  protected abstract InputStream resource(
    String name)
    throws Exception;

  @Test
  public void testEmpty(
    final @Mocked JRPSResourceReceiverType resources,
    final @Mocked JRPSParserErrorReceiverType errors)
    throws Exception
  {
    try (JRPSParserType parser =
           this.createParser(URI.create("empty"),
                             this.resource("empty.xml"), resources, errors)) {
      Assertions.assertTrue(parser.parse());
    }

    new FullVerifications()
    {{
      resources.receive(this.anyString, this.anyString, this.anyString);
      this.times = 0;
      errors.onParseError((JRPSParseError) this.any);
      this.times = 0;
    }};
  }

  @Test
  public void testSimple(
    final @Mocked JRPSResourceReceiverType resources,
    final @Mocked JRPSParserErrorReceiverType errors)
    throws Exception
  {
    try (JRPSParserType parser =
           this.createParser(URI.create("simple"),
                             this.resource("simple.xml"), resources, errors)) {
      Assertions.assertTrue(parser.parse());
    }

    new FullVerifications()
    {{
      resources.receive("com.io7m.x", "text", "/x/y/z/x.txt");
      resources.receive("com.io7m.y", "image", "/x/y/z/y.txt");
      errors.onParseError((JRPSParseError) this.any);
      this.times = 0;
    }};
  }

  @Test
  public void testInvalid(
    final @Mocked JRPSResourceReceiverType resources,
    final @Mocked JRPSParserErrorReceiverType errors)
    throws Exception
  {
    try (JRPSParserType parser =
           this.createParser(URI.create("invalid"),
                             this.resource("invalid.xml"),
                             resources,
                             errors)) {
      Assertions.assertFalse(parser.parse());
    }

    new FullVerifications()
    {{
      errors.onParseError((JRPSParseError) this.any);
      this.times = 3;
    }};
  }
}