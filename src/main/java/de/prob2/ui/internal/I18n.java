package de.prob2.ui.internal;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class I18n {

	private static final Logger LOGGER = LoggerFactory.getLogger(I18n.class);

	private final ResourceBundle bundle;

	@Inject
	private I18n(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	public String translate(String key) {
		try {
			return bundle.getString(key);
		} catch (Exception e) {
			LOGGER.warn("Error while translating key '{}'", key, e);
			return key;
		}
	}

	public String format(String key, Object... arguments) {
		String pattern = translate(key);
		try {
			MessageFormat messageFormat = new MessageFormat(pattern);
			return messageFormat.format(arguments);
		} catch (Exception e) {
			LOGGER.warn("Error while formatting pattern '{}' for key '{}'", pattern, key, e);
			return key;
		}
	}
}
