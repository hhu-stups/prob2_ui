package de.prob2.ui.internal;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class I18n {

	private static final Logger LOGGER = LoggerFactory.getLogger(I18n.class);

	private final Injector injector;
	private volatile ResourceBundle bundle;

	@Inject
	private I18n(Injector injector/*, UIState uiState*/) {
		this.injector = injector;

		// this code allows changing the locale at runtime
		/*uiState.localeOverrideProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue != newValue) {
				Locale.setDefault(newValue);
				reset();
			}
		});*/
	}

	private synchronized void reset() {
		this.bundle = null;
	}

	private synchronized void init() {
		if (this.bundle == null) {
			this.bundle = ResourceBundle.getBundle("de.prob2.ui.prob2", locale());
		}
	}

	private Locale locale() {
		return injector.getInstance(Locale.class);
	}

	public synchronized ResourceBundle bundle() {
		init();
		return bundle;
	}

	private String translate0(String key) {
		try {
			return bundle().getString(key);
		} catch (Exception e) {
			LOGGER.error("Error while translating key '{}'", key, e);
			return key;
		}
	}

	public String translate(String key, Object... arguments) {
		String pattern = translate0(key);
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(arguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting pattern '{}' for given key '{}'", pattern, key, e);
			return pattern;
		}
	}

	public String format(String pattern, Object... arguments) {
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(arguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting given pattern '{}'", pattern, e);
			return pattern;
		}
	}
}
