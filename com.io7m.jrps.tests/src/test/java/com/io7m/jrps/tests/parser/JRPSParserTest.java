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

package com.io7m.jrps.tests.parser;

import com.io7m.jrps.parser.JRPSParserProvider;
import com.io7m.jrps.parser.api.JRPSParserType;
import com.io7m.jrps.parser.api.JRPSParserErrorReceiverType;
import com.io7m.jrps.parser.api.JRPSResourceReceiverType;
import com.io7m.jrps.tests.parser.api.JRPSParserContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.NoSuchFileException;

public final class JRPSParserTest extends JRPSParserContract
{

  @Override
  protected JRPSParserType createParser(
    final URI uri,
    final InputStream stream,
    final JRPSResourceReceiverType res,
    final JRPSParserErrorReceiverType error)
    throws Exception
  {
    return JRPSParserProvider.provider().create(uri, stream, res, error);
  }

  @Override
  protected Logger log()
  {
    return LoggerFactory.getLogger(JRPSParserTest.class);
  }

  @Override
  protected InputStream resource(
    final String name)
    throws Exception
  {
    final InputStream stream = JRPSParserTest.class.getResourceAsStream(
      "/com/io7m/jrps/tests/parser/" + name);
    if (stream == null) {
      throw new NoSuchFileException(name);
    }
    return stream;
  }
}