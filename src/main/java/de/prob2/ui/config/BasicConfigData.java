package de.prob2.ui.config;

import java.util.Locale;

/**
 * A subset of the full {@link ConfigData}, which is used to load parts of the config before the injector is set up. This is needed to apply the locale override for example.
 */
public class BasicConfigData {
	public Locale localeOverride;

	BasicConfigData() {}
}
