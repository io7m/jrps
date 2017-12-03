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

package com.io7m.jrps.controller;

import com.io7m.jrps.controller.api.JRPSControllerConfiguration;
import com.io7m.jrps.controller.api.JRPSControllerErrorReceiverType;
import com.io7m.jrps.controller.api.JRPSControllerException;
import com.io7m.jrps.controller.api.JRPSControllerProviderType;
import com.io7m.jrps.controller.api.JRPSControllerType;
import com.io7m.jrps.controller.api.JRPSResourceException;
import com.io7m.jrps.controller.api.JRPSResourceType;
import com.io7m.jrps.parser.api.JRPSParseError;
import com.io7m.jrps.parser.api.JRPSParserProviderType;
import com.io7m.jrps.parser.api.JRPSParserType;
import com.io7m.jrps.parser.api.JRPSResourceReceiverType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The default controller provider.
 */

public final class JRPSControllers implements JRPSControllerProviderType
{
  private final ResourceBundle messages;

  private JRPSControllers(final ResourceBundle in_messages)
  {
    this.messages = Objects.requireNonNull(in_messages, "Messages");
  }

  /**
   * Create a controller provider with the specified locale.
   *
   * @param in_locale The locale
   *
   * @return A controller provider
   */

  public static JRPSControllerProviderType createProviderWith(
    final Locale in_locale)
  {
    Objects.requireNonNull(in_locale, "Locale");

    final ResourceBundle messages =
      ResourceBundle.getBundle(
        "com/io7m/jrps/controller/JRPSControllerMessages",
        in_locale);

    return new JRPSControllers(messages);
  }

  /**
   * Create a controller provider with the current default locale.
   *
   * @return A controller provider
   */

  public static JRPSControllerProviderType createProvider()
  {
    return createProviderWith(Locale.getDefault());
  }

  private static String formatError(
    final ResourceBundle messages,
    final String message_id,
    final Map<String, String> details)
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append(messages.getString(message_id));
    sb.append(System.lineSeparator());

    details.forEach((key, value) -> {
      sb.append("  ");
      sb.append(messages.getString(key));
      sb.append(": ");
      sb.append(value);
      sb.append(System.lineSeparator());
    });

    return sb.toString();
  }

  private static void loadResource(
    final ResourceBundle in_messages,
    final ErrorTracker errors,
    final Module module,
    final LoadedModule loaded,
    final String id,
    final String type,
    final String path)
  {
    final String path_actual;
    if (path.startsWith("/")) {
      path_actual = path.substring(1);
    } else {
      path_actual = path;
    }

    final List<String> path_split = List.of(path_actual.split("/"));
    final String package_name =
      path_split.subList(0, path_split.size() - 1)
        .stream()
        .collect(Collectors.joining("."));

    if (module.getPackages().contains(package_name)) {
      if (!module.isOpen(package_name)) {
        errors.onConfigurationError(
          module,
          Optional.of(path),
          Optional.of(id),
          in_messages.getString("module.resources.package_not_open"),
          Optional.empty());
      }
    }

    final LoadedResource res =
      new LoadedResource(in_messages, module, id, type, path);

    loaded.resources_by_id.put(id, res);
  }

  private static Optional<String> scanManifest(
    final ErrorTracker errors,
    final Module module)
  {
    final String file = "/META-INF/MANIFEST.MF";

    try (InputStream stream = module.getResourceAsStream(file)) {
      if (stream == null) {
        return Optional.empty();
      }

      final Properties props = new Properties();
      props.load(stream);
      if (props.containsKey("JRPS-Resources")) {
        return Optional.of(props.getProperty("JRPS-Resources"));
      }
    } catch (final IOException e) {
      ioError(errors, module, file, e);
    }

    return Optional.empty();
  }

  private static void ioError(
    final ErrorTracker errors,
    final Module module,
    final String file,
    final IOException e)
  {
    errors.onConfigurationError(
      module,
      Optional.of(file),
      Optional.empty(),
      e.getLocalizedMessage(),
      Optional.of(e));
  }

  private static JRPSResourceException fail(
    final ResourceBundle messages,
    final Module caller,
    final String module,
    final String id,
    final String message)
  {
    final String name =
      caller.getName() == null ? "<unnamed>" : caller.getName();

    return new JRPSResourceException(formatError(
      messages,
      message,
      Map.of("module.requesting", name,
             "module", module,
             "resource", id)));
  }

  @Override
  public JRPSControllerType createController(
    final JRPSControllerConfiguration config)
  {
    Objects.requireNonNull(config, "Configuration");

    final ModuleLayer layer = config.moduleLayer();
    final HashMap<String, LoadedModule> modules =
      new HashMap<>(layer.modules().size());

    final ErrorTracker errors = new ErrorTracker(config.errors());
    for (final Module module : layer.modules()) {
      this.scanModule(config, errors, module, modules);
    }

    if (errors.failed) {
      throw new JRPSControllerException(
        this.messages.getString("controller.configuration.error"));
    }

    return new Controller(this.messages, modules);
  }

  private void scanModule(
    final JRPSControllerConfiguration config,
    final ErrorTracker errors,
    final Module module,
    final Map<String, LoadedModule> modules)
  {
    final Optional<String> resources_opt = scanManifest(errors, module);
    if (resources_opt.isPresent()) {
      final String file = resources_opt.get();
      final LoadedModule loaded =
        this.parseModuleFile(config, errors, module, file);
      if (loaded != null) {
        modules.put(loaded.module.getName(), loaded);
      }
    }
  }

  private LoadedModule parseModuleFile(
    final JRPSControllerConfiguration config,
    final ErrorTracker errors,
    final Module module,
    final String file)
  {
    try (InputStream stream = module.getResourceAsStream(file)) {
      if (stream == null) {
        errors.onConfigurationError(
          module,
          Optional.of(file),
          Optional.empty(),
          this.messages.getString("module.resources.failed"),
          Optional.empty());
        return null;
      }

      final LoadedModule loaded = new LoadedModule(module);

      final URI uri = URI.create(
        new StringBuilder(64)
          .append("jrps:file:")
          .append(module.getName())
          .append("/")
          .append(file)
          .toString());

      final JRPSParserProviderType parsers =
        config.parsers();

      final JRPSResourceReceiverType on_resource =
        (id, type, path) ->
          loadResource(this.messages, errors, module, loaded, id, type, path);

      try (JRPSParserType parser =
             parsers.create(uri, stream, on_resource, errors)) {
        if (!parser.parse()) {
          return null;
        }
      } catch (final IOException e) {
        ioError(errors, module, file, e);
        return null;
      }

      return loaded;
    } catch (final IOException e) {
      ioError(errors, module, file, e);
      return null;
    }
  }

  private static final class ErrorTracker
    implements JRPSControllerErrorReceiverType
  {
    private final JRPSControllerErrorReceiverType receiver;
    private boolean failed;

    private ErrorTracker(
      final JRPSControllerErrorReceiverType in_receiver)
    {
      this.receiver =
        Objects.requireNonNull(in_receiver, "Receiver");
    }

    @Override
    public void onConfigurationError(
      final Module module,
      final Optional<String> file,
      final Optional<String> resource,
      final String message,
      final Optional<Exception> exception)
    {
      this.failed = true;
      this.receiver.onConfigurationError(
        module, file, resource, message, exception);
    }

    @Override
    public void onParseError(
      final JRPSParseError error)
    {
      this.failed = true;
      this.receiver.onParseError(error);
    }
  }

  private static final class LoadedModule
  {
    private final Module module;
    private final HashMap<String, LoadedResource> resources_by_id;

    LoadedModule(
      final Module in_module)
    {
      this.module = Objects.requireNonNull(in_module, "Module");
      this.resources_by_id = new HashMap<>();
    }
  }

  private static final class LoadedResource implements JRPSResourceType
  {
    private final Module module;
    private final String id;
    private final String type;
    private final String path;
    private final ResourceBundle messages;

    LoadedResource(
      final ResourceBundle in_messages,
      final Module in_module,
      final String in_id,
      final String in_type,
      final String in_path)
    {
      this.messages = Objects.requireNonNull(in_messages, "Messages");
      this.module = Objects.requireNonNull(in_module, "Module");
      this.id = Objects.requireNonNull(in_id, "ID");
      this.type = Objects.requireNonNull(in_type, "Type");
      this.path = Objects.requireNonNull(in_path, "Path");
    }

    @Override
    public InputStream openStream()
      throws IOException
    {
      final InputStream stream = this.module.getResourceAsStream(this.path);
      if (stream == null) {
        throw new JRPSResourceException(
          formatError(
            this.messages,
            "resource.misconfigured.missing",
            Map.of("module", this.module.getName(),
                   "resource", this.id,
                   "path", this.path)));
      }
      return stream;
    }

    @Override
    public Module module()
    {
      return this.module;
    }

    @Override
    public String id()
    {
      return this.id;
    }

    @Override
    public String type()
    {
      return this.type;
    }
  }

  private static final class Controller implements JRPSControllerType
  {
    private final HashMap<String, LoadedModule> modules;
    private final ResourceBundle messages;

    Controller(
      final
      ResourceBundle in_messages,
      final HashMap<String, LoadedModule> in_modules)
    {
      this.messages = Objects.requireNonNull(in_messages, "Messages");
      this.modules = Objects.requireNonNull(in_modules, "Modules");
    }

    @Override
    public JRPSResourceType resource(
      final Module caller,
      final String module,
      final String id)
      throws JRPSResourceException
    {
      Objects.requireNonNull(caller, "Caller");
      Objects.requireNonNull(module, "Module");
      Objects.requireNonNull(id, "ID");

      if (this.modules.containsKey(module)) {
        final LoadedModule m = this.modules.get(module);

        if (caller.canRead(m.module)) {
          if (m.resources_by_id.containsKey(id)) {
            return m.resources_by_id.get(id);
          }
          throw fail(this.messages, caller, module, id, "resource.nonexistent");
        }
        throw fail(this.messages, caller, module, id, "module.does_not_read");
      }
      throw fail(this.messages, caller, module, id, "module.nonexistent");
    }
  }
}
