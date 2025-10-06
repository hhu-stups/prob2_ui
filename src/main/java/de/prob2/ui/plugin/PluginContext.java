package de.prob2.ui.plugin;

import org.pf4j.PluginWrapper;

public record PluginContext(
		PluginWrapper pluginWrapper,
		ProBPluginManager proBPluginManager
) {
}
