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

package com.io7m.jrps.tests.controller.api;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.io7m.jrps.controller.api.JRPSControllerConfiguration;
import com.io7m.jrps.controller.api.JRPSControllerErrorReceiverType;
import com.io7m.jrps.controller.api.JRPSControllerException;
import com.io7m.jrps.controller.api.JRPSControllerType;
import com.io7m.jrps.controller.api.JRPSResourceException;
import com.io7m.jrps.controller.api.JRPSResourceType;
import com.io7m.jrps.parser.JRPSParserProvider;
import com.io7m.jrps.parser.api.JRPSParseError;
import com.io7m.junreachable.UnreachableCodeException;
import mockit.FullVerifications;
import mockit.Mocked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class JRPSControllerContract
{
  protected abstract Logger log();

  private static FileSystem setupFilesystem(
    final Map<String, String> resources)
    throws IOException
  {
    final FileSystem fs =
      MemoryFileSystemBuilder.newLinux().build("test");

    final Path root = fs.getRootDirectories().iterator().next();

    resources.forEach((resource, file) -> {
      try (InputStream stream =
             JRPSControllerContract.class.getResourceAsStream(
               "/com/io7m/jrps/tests/controller/" + resource)) {
        final Path out_path = root.resolve(file);
        try (OutputStream out = Files.newOutputStream(out_path)) {
          stream.transferTo(out);
        }
      } catch (final IOException e) {
        throw new UnreachableCodeException(e);
      }
    });

    return fs;
  }

  protected abstract JRPSControllerType controller(
    JRPSControllerConfiguration configuration)
    throws Exception;

  @Test
  public void testEmpty(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    final JRPSControllerConfiguration configuration =
      JRPSControllerConfiguration.builder()
        .setModuleLayer(ModuleLayer.empty())
        .setParsers(JRPSParserProvider.provider())
        .setErrors(errors)
        .build();

    final JRPSControllerType c =
      this.controller(configuration);

    new FullVerifications()
    {{
      errors.onParseError((JRPSParseError) this.any);
      this.times = 0;
      errors.onConfigurationError(
        (Module) this.any,
        (Optional<String>) this.any,
        (Optional<String>) this.any,
        (String) this.any,
        (Optional<Exception>) this.any);
      this.times = 0;
    }};
  }

  @Test
  public void testNonexistentModule(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    final JRPSControllerConfiguration configuration =
      JRPSControllerConfiguration.builder()
        .setModuleLayer(ModuleLayer.empty())
        .setParsers(JRPSParserProvider.provider())
        .setErrors(errors)
        .build();

    final JRPSControllerType c = this.controller(configuration);

    new FullVerifications()
    {{
      errors.onParseError((JRPSParseError) this.any);
      this.times = 0;
      errors.onConfigurationError(
        (Module) this.any,
        (Optional<String>) this.any,
        (Optional<String>) this.any,
        (String) this.any,
        (Optional<Exception>) this.any);
      this.times = 0;
    }};

    final JRPSResourceException ex = Assertions.assertThrows(
      JRPSResourceException.class,
      () -> c.resource(
        JRPSControllerContract.class.getModule(),
        "nonexistent",
        "a.b.c"));

    Assertions.assertTrue(ex.getMessage().contains("Nonexistent module"));
  }

  @Test
  public void testModuleMissingConfiguration(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.missing_config.jar",
        "com.io7m.tests.missing_config.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.missing_config");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      Assertions.assertThrows(
        JRPSControllerException.class,
        () -> this.controller(configuration));

      new FullVerifications()
      {{
        errors.onParseError((JRPSParseError) this.any);
        this.times = 0;
        errors.onConfigurationError(
          (Module) this.any,
          (Optional<String>) this.any,
          (Optional<String>) this.any,
          "Failed to parse module resources configuration",
          (Optional<Exception>) this.any);
        this.times = 1;
      }};
    }
  }

  private ModuleLayer createLayer(
    final FileSystem fs,
    final String module_name)
  {
    final ModuleFinder finder =
      ModuleFinder.of(fs.getRootDirectories().iterator().next());
    final ModuleLayer parent =
      ModuleLayer.boot();
    final Configuration cf =
      parent.configuration().resolve(
        finder, ModuleFinder.of(), Set.of(module_name));
    final ClassLoader scl =
      ClassLoader.getSystemClassLoader();
    return parent.defineModulesWithOneLoader(cf, scl);
  }

  @Test
  public void testModuleBrokenConfiguration(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.broken_config.jar",
        "com.io7m.tests.broken_config.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.broken_config");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      Assertions.assertThrows(
        JRPSControllerException.class,
        () -> this.controller(configuration));

      new FullVerifications()
      {{
        errors.onParseError((JRPSParseError) this.any);
        this.times = 1;
        errors.onConfigurationError(
          (Module) this.any,
          (Optional<String>) this.any,
          (Optional<String>) this.any,
          (String) this.any,
          (Optional<Exception>) this.any);
        this.times = 1;
      }};
    }
  }

  @Test
  public void testModuleBrokenConfiguration2(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.broken_config2.jar",
        "com.io7m.tests.broken_config2.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.broken_config2");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      Assertions.assertThrows(
        JRPSControllerException.class,
        () -> this.controller(configuration));

      new FullVerifications()
      {{
        errors.onParseError((JRPSParseError) this.any);
        this.times = 1;
      }};
    }
  }

  @Test
  public void testModuleEmpty(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "empty.jar",
        "empty.jar"))) {

      final ModuleFinder finder =
        ModuleFinder.of(fs.getRootDirectories().iterator().next());
      final ModuleLayer parent =
        ModuleLayer.boot();
      final Configuration cf =
        parent.configuration().resolve(
          finder, ModuleFinder.of(), Set.of());
      final ClassLoader scl =
        ClassLoader.getSystemClassLoader();
      final ModuleLayer layer =
        parent.defineModulesWithOneLoader(cf, scl);

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      this.controller(configuration);
    }
  }

  @Test
  public void testModuleSimple(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.simple_config.jar",
        "com.io7m.tests.simple_config.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.simple_config");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      final JRPSControllerType c = this.controller(configuration);

      final Module caller = JRPSControllerContract.class.getModule();

      {
        final JRPSResourceType r =
          c.resource(caller, "com.io7m.tests.simple_config", "com.io7m.x");

        Assertions.assertEquals("com.io7m.x", r.id());
        Assertions.assertEquals("text", r.type());
        Assertions.assertEquals(
          layer.findModule("com.io7m.tests.simple_config").get(), r.module());

        try (InputStream stream = r.openStream()) {
          final byte[] buf = new byte[4];
          stream.read(buf);
          Assertions.assertArrayEquals(
            new byte[]{(byte) 'p', (byte) 'a', (byte) 'c', (byte) 'k'},
            buf);
        }
      }

      {
        final JRPSResourceType r =
          c.resource(caller, "com.io7m.tests.simple_config", "com.io7m.y");

        Assertions.assertEquals("com.io7m.y", r.id());
        Assertions.assertEquals("class", r.type());
        Assertions.assertEquals(
          layer.findModule("com.io7m.tests.simple_config").get(), r.module());

        try (InputStream stream = r.openStream()) {
          final byte[] buf = new byte[4];
          stream.read(buf);
          Assertions.assertArrayEquals(
            new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe},
            buf);
        }
      }

      {
        final JRPSResourceType r =
          c.resource(caller, "com.io7m.tests.simple_config", "com.io7m.z");

        Assertions.assertEquals("com.io7m.z", r.id());
        Assertions.assertEquals("class", r.type());
        Assertions.assertEquals(
          layer.findModule("com.io7m.tests.simple_config").get(), r.module());

        final JRPSResourceException ex =
          Assertions.assertThrows(JRPSResourceException.class, r::openStream);

        Assertions.assertTrue(ex.getMessage().contains("Misconfigured"));
      }
    }
  }

  @Test
  public void testModuleSimpleNonexistentResource()
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.simple_config.jar",
        "com.io7m.tests.simple_config.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.simple_config");

      final JRPSControllerErrorReceiverType errors =
        new JRPSControllerErrorReceiverType()
        {
          @Override
          public void onParseError(
            final JRPSParseError error)
          {
            JRPSControllerContract.this.log().error(
              "onParseError: {}", error);
          }

          @Override
          public void onConfigurationError(
            final Module module,
            final Optional<String> file,
            final Optional<String> resource,
            final String message,
            final Optional<Exception> exception)
          {
            JRPSControllerContract.this.log().error(
              "onConfigurationError: {} {} {} {} {}",
              module,
              file,
              resource,
              message,
              exception);
          }
        };

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      final JRPSControllerType c = this.controller(configuration);

      final Module caller =
        JRPSControllerContract.class.getModule();

      final JRPSResourceException ex =
        Assertions.assertThrows(
          JRPSResourceException.class,
          () -> {
            c.resource(
              caller,
              "com.io7m.tests.simple_config",
              "nonexistent");
          });

      Assertions.assertTrue(ex.getMessage().contains("Nonexistent resource"));
    }
  }

  @Test
  public void testModuleSimpleNotOpen(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.simple_not_open.jar",
        "com.io7m.tests.simple_not_open.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.simple_not_open");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      Assertions.assertThrows(
        JRPSControllerException.class,
        () -> this.controller(configuration));

      new FullVerifications()
      {{
        errors.onConfigurationError(
          layer.findModule("com.io7m.tests.simple_not_open").get(),
          Optional.of("/com/io7m/tests/simple_not_open/A.java"),
          Optional.of("com.io7m.x"),
          "Module specifies a resource file in a package that is not 'open'.",
          Optional.empty());

        errors.onConfigurationError(
          layer.findModule("com.io7m.tests.simple_not_open").get(),
          Optional.of("/com/io7m/tests/simple_not_open/A.class"),
          Optional.of("com.io7m.y"),
          "Module specifies a resource file in a package that is not 'open'.",
          Optional.empty());
      }};
    }
  }

  @Test
  public void testModuleDoesNotRead(
    final @Mocked JRPSControllerErrorReceiverType errors)
    throws Exception
  {
    try (FileSystem fs = setupFilesystem(
      Map.of(
        "com.io7m.tests.triple_a.jar", "com.io7m.tests.triple_a.jar",
        "com.io7m.tests.triple_b.jar", "com.io7m.tests.triple_b.jar",
        "com.io7m.tests.triple_c.jar", "com.io7m.tests.triple_c.jar"))) {

      final ModuleLayer layer =
        this.createLayer(fs, "com.io7m.tests.triple_c");

      final JRPSControllerConfiguration configuration =
        JRPSControllerConfiguration.builder()
          .setModuleLayer(layer)
          .setParsers(JRPSParserProvider.provider())
          .setErrors(errors)
          .build();

      final JRPSControllerType c = this.controller(configuration);

      final JRPSResourceException ex =
        Assertions.assertThrows(
          JRPSResourceException.class,
          () -> {
            c.resource(
              layer.findModule("com.io7m.tests.triple_a").get(),
              "com.io7m.tests.triple_b",
              "com.io7m.x");
          });

      Assertions.assertTrue(ex.getMessage().contains(
        "Requesting module does not 'require' the target module"));
    }
  }
}
