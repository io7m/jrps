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

package com.io7m.jrps.parser;

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jrps.parser.api.JRPSFormatVersion;
import com.io7m.jrps.parser.api.JRPSFormatVersionRange;
import com.io7m.jrps.parser.api.JRPSParseError;
import com.io7m.jrps.parser.api.JRPSParserErrorReceiverType;
import com.io7m.jrps.parser.api.JRPSParserProviderType;
import com.io7m.jrps.parser.api.JRPSParserType;
import com.io7m.jrps.parser.api.JRPSResourceReceiverType;
import com.io7m.jrps.schema.JRPSSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

/**
 * The default parser implementation.
 */

public final class JRPSParserProvider implements JRPSParserProviderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(JRPSParserProvider.class);

  private static final JRPSParserProvider INSTANCE = new JRPSParserProvider();

  private final SchemaFactory schema_factory;
  private final SAXParserFactory parsers;
  private final Set<JRPSFormatVersionRange> supported;

  private JRPSParserProvider()
  {
    this.parsers =
      SAXParserFactory.newInstance();
    this.schema_factory =
      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    this.supported = Set.of(
      JRPSFormatVersionRange.of(
        JRPSFormatVersion.of(1, 0),
        JRPSFormatVersion.of(1, 0)));
  }

  /**
   * @return The parser provider
   */

  public static JRPSParserProviderType provider()
  {
    return INSTANCE;
  }

  @Override
  public Set<JRPSFormatVersionRange> supported()
  {
    return this.supported;
  }

  @Override
  public JRPSParserType create(
    final URI uri,
    final InputStream stream,
    final JRPSResourceReceiverType resources,
    final JRPSParserErrorReceiverType errors)
    throws IOException
  {
    Objects.requireNonNull(uri, "URI");
    Objects.requireNonNull(stream, "Stream");
    Objects.requireNonNull(resources, "Resources");
    Objects.requireNonNull(errors, "Errors");

    try {
      final Schema schema =
        this.schema_factory.newSchema(JRPSSchema.schemaURL());

      this.parsers.setFeature(
        XMLConstants.FEATURE_SECURE_PROCESSING,
        true);
      this.parsers.setFeature(
        "http://xml.org/sax/features/external-general-entities",
        false);
      this.parsers.setFeature(
        "http://xml.org/sax/features/external-parameter-entities",
        false);
      this.parsers.setFeature(
        "http://apache.org/xml/features/validation/warn-on-duplicate-attdef",
        true);
      this.parsers.setFeature(
        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
        false);
      this.parsers.setFeature(
        "http://apache.org/xml/features/disallow-doctype-decl",
        true);
      this.parsers.setFeature(
        "http://apache.org/xml/features/standard-uri-conformant",
        true);
      this.parsers.setFeature(
        "http://apache.org/xml/features/xinclude",
        false);

      this.parsers.setNamespaceAware(true);
      this.parsers.setSchema(schema);

      final SAXParser sax_parser = this.parsers.newSAXParser();
      return new Parser(uri, stream, sax_parser, resources, errors);
    } catch (final ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }

  private static final class Parser
    extends DefaultHandler implements JRPSParserType
  {
    private final JRPSResourceReceiverType receiver_res;
    private final JRPSParserErrorReceiverType receiver_errors;
    private final URI uri;
    private final SAXParser parser;
    private final InputStream stream;

    private Locator locator;
    private String uri_expected;
    private boolean failed;

    private Parser(
      final URI in_uri,
      final InputStream in_stream,
      final SAXParser sax_parser,
      final JRPSResourceReceiverType resources,
      final JRPSParserErrorReceiverType errors)
    {
      this.uri = in_uri;
      this.parser = sax_parser;
      this.stream = in_stream;
      this.receiver_res = resources;
      this.receiver_errors = errors;
    }

    @Override
    public InputSource resolveEntity(
      final String in_public_id,
      final String in_system_id)
      throws IOException, SAXException
    {
      LOG.trace("resolveEntity: {} {}", in_public_id, in_system_id);
      return super.resolveEntity(in_public_id, in_system_id);
    }

    @Override
    public void notationDecl(
      final String in_name,
      final String in_public_id,
      final String in_system_id)
      throws SAXException
    {
      LOG.trace("notationDecl: {} {} {}", in_name, in_public_id, in_system_id);
    }

    @Override
    public void unparsedEntityDecl(
      final String in_name,
      final String in_public_id,
      final String in_system_id,
      final String in_notation)
      throws SAXException
    {
      LOG.trace(
        "unparsedEntityDecl: {} {} {} {}",
        in_name,
        in_public_id,
        in_system_id,
        in_notation);
    }

    @Override
    public void setDocumentLocator(
      final Locator in_locator)
    {
      LOG.trace("setDocumentLocator: {}", in_locator);
      this.locator = Objects.requireNonNull(in_locator, "Locator");
    }

    @Override
    public void startDocument()
      throws SAXException
    {
      LOG.trace("startDocument");
    }

    @Override
    public void endDocument()
      throws SAXException
    {
      LOG.trace("endDocument");
    }

    @Override
    public void startPrefixMapping(
      final String in_prefix,
      final String in_uri)
      throws SAXException
    {
      LOG.trace("startPrefixMapping: {} {}", in_prefix, in_uri);

      this.uri_expected = JRPSSchema.schemaNamespaceURI().toString();
      if (!Objects.equals(in_uri, this.uri_expected)) {
        throw new SAXParseException(
          new StringBuilder(64)
            .append("Unexpected document type.")
            .append(System.lineSeparator())
            .append("  Expected: ")
            .append(this.uri_expected)
            .append(System.lineSeparator())
            .append("  Received: ")
            .append(in_uri)
            .append(System.lineSeparator())
            .toString(),
          this.locator);
      }
    }

    @Override
    public void endPrefixMapping(
      final String prefix)
      throws SAXException
    {
      LOG.trace("endPrefixMapping: {}", prefix);
    }

    @Override
    public void startElement(
      final String in_uri,
      final String in_local_name,
      final String in_q_name,
      final Attributes attributes)
      throws SAXException
    {
      LOG.trace("startElement: {} {} {} {}",
                in_uri, in_local_name, in_q_name, attributes);

      if (this.failed) {
        return;
      }

      if (!Objects.equals(in_uri, this.uri_expected)) {
        return;
      }

      switch (in_local_name) {
        case "resources": {
          break;
        }

        case "resource": {
          this.onResource(attributes);
          break;
        }

        default: {
          break;
        }
      }
    }

    private void onResource(
      final Attributes attributes)
    {
      String id = null;
      String type = null;
      String path = null;

      for (int index = 0; index < attributes.getLength(); ++index) {
        switch (attributes.getLocalName(index)) {
          case "id": {
            id = attributes.getValue(index);
            break;
          }
          case "type": {
            type = attributes.getValue(index);
            break;
          }
          case "path": {
            path = attributes.getValue(index);
            break;
          }
          default: {
            break;
          }
        }
      }

      Objects.requireNonNull(id, "Id");
      Objects.requireNonNull(type, "Type");
      Objects.requireNonNull(path, "Path");

      try {
        this.receiver_res.receive(id, type, path);
      } catch (final Exception e) {
        this.receiver_errors.onParseError(
          JRPSParseError.builder()
            .setLexical(LexicalPosition.<URI>builder()
                          .setLine(this.locator.getLineNumber())
                          .setColumn(this.locator.getColumnNumber())
                          .setFile(this.uri)
                          .build())
            .setSeverity(JRPSParseError.Severity.ERROR)
            .setMessage(e.getMessage())
            .setException(e)
            .build());
        this.failed = true;
      }
    }

    @Override
    public void endElement(
      final String in_uri,
      final String in_local_name,
      final String in_qname)
      throws SAXException
    {
      LOG.trace("endElement: {} {} {}", in_uri, in_local_name, in_qname);

      if (this.failed) {
        return;
      }

      if (!Objects.equals(in_uri, this.uri_expected)) {
        return;
      }

      switch (in_local_name) {
        case "resources": {
          break;
        }

        case "resource": {
          break;
        }

        default: {
          break;
        }
      }
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      LOG.trace(
        "characters: {} {}",
        Integer.valueOf(start),
        Integer.valueOf(length));
    }

    @Override
    public void ignorableWhitespace(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      LOG.trace(
        "ignorableWhitespace: {} {}",
        Integer.valueOf(start),
        Integer.valueOf(length));
    }

    @Override
    public void processingInstruction(
      final String target,
      final String data)
      throws SAXException
    {
      LOG.trace("processingInstruction: {} {}", target, data);
    }

    @Override
    public void skippedEntity(
      final String name)
      throws SAXException
    {
      LOG.trace("skippedEntity: {}", name);
    }

    @Override
    public void warning(
      final SAXParseException e)
      throws SAXException
    {
      this.receiver_errors.onParseError(
        JRPSParseError.builder()
          .setLexical(LexicalPosition.<URI>builder()
                        .setLine(e.getLineNumber())
                        .setColumn(e.getColumnNumber())
                        .setFile(this.uri)
                        .build())
          .setSeverity(JRPSParseError.Severity.WARNING)
          .setMessage(e.getMessage())
          .setException(e)
          .build());
    }

    @Override
    public void error(
      final SAXParseException e)
      throws SAXException
    {
      this.receiver_errors.onParseError(
        JRPSParseError.builder()
          .setLexical(LexicalPosition.<URI>builder()
                        .setLine(e.getLineNumber())
                        .setColumn(e.getColumnNumber())
                        .setFile(this.uri)
                        .build())
          .setSeverity(JRPSParseError.Severity.ERROR)
          .setMessage(e.getMessage())
          .setException(e)
          .build());

      this.failed = true;
    }

    @Override
    public void fatalError(
      final SAXParseException e)
      throws SAXException
    {
      this.receiver_errors.onParseError(
        JRPSParseError.builder()
          .setLexical(LexicalPosition.<URI>builder()
                        .setLine(e.getLineNumber())
                        .setColumn(e.getColumnNumber())
                        .setFile(this.uri)
                        .build())
          .setSeverity(JRPSParseError.Severity.CRITICAL)
          .setMessage(e.getMessage())
          .setException(e)
          .build());

      this.failed = true;
      throw e;
    }

    @Override
    public boolean parse()
      throws IOException
    {
      try {
        this.parser.parse(this.stream, this);
        return !this.failed;
      } catch (final SAXException e) {
        throw new IOException(e);
      }
    }

    @Override
    public void close()
      throws IOException
    {

    }
  }
}
