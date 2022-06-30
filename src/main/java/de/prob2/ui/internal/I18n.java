package de.prob2.ui.internal;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
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
		Objects.requireNonNull(key, "key");
		try {
			return bundle().getString(key);
		} catch (Exception e) {
			LOGGER.error("Error while translating key '{}'", key, e);
			return key;
		}
	}

	/**
	 * First translates the given key to obtain a pattern, then formats the pattern via MessageFormat.
	 *
	 * @param key       key to translate
	 * @param arguments arguments for formatting
	 * @return formatted and translated string
	 */
	public String translate(String key, Object... arguments) {
		Objects.requireNonNull(arguments, "arguments");
		String pattern = translate0(key);
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(arguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting pattern '{}' for given key '{}'", pattern, key, e);
			return pattern;
		}
	}

	/**
	 * Formats a given pattern via MessageFormat without translating.
	 *
	 * @param pattern   pattern
	 * @param arguments arguments for formatting
	 * @return formatted string
	 */
	public String format(String pattern, Object... arguments) {
		Objects.requireNonNull(pattern, "pattern");
		Objects.requireNonNull(arguments, "arguments");
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(arguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting given pattern '{}'", pattern, e);
			return pattern;
		}
	}

	/**
	 * Generates a string binding for the given translation, according to the rules of {@link I18n#translate(String, Object...)}.
	 * Observable value in the arguments will be evaluated and the translation will be redone when one of them changes.
	 *
	 * @param key       key to translate
	 * @param arguments arguments for formatting (may be observable)
	 * @return binding for the formatted and translated string
	 */
	public StringBinding translateBinding(String key, Object... arguments) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(arguments, "arguments");
		ObservableValue<?>[] dependencies = Arrays.stream(arguments)
				                                    .filter(arg -> arg instanceof ObservableValue)
				                                    .map(arg -> (ObservableValue<?>) arg)
				                                    .toArray(ObservableValue[]::new);
		return Bindings.createStringBinding(
				() -> translate(
						key,
						Arrays.stream(arguments)
								.map(arg -> arg instanceof ObservableValue ? ((ObservableValue<?>) arg).getValue() : arg)
								.toArray()
				),
				dependencies
		);
	}

	/**
	 * Generates a string binding for the given formatting, according to the rules of {@link I18n#format(String, Object...)}.
	 * Observable value in the arguments will be evaluated and the formatting will be redone when one of them changes.
	 *
	 * @param pattern   pattern
	 * @param arguments arguments for formatting (may be observable)
	 * @return binding for the formatted string
	 */
	public StringBinding formatBinding(String pattern, Object... arguments) {
		Objects.requireNonNull(pattern, "pattern");
		Objects.requireNonNull(arguments, "arguments");
		ObservableValue<?>[] dependencies = Arrays.stream(arguments)
				                                    .filter(arg -> arg instanceof ObservableValue)
				                                    .map(arg -> (ObservableValue<?>) arg)
				                                    .toArray(ObservableValue[]::new);
		return Bindings.createStringBinding(
				() -> format(
						pattern,
						Arrays.stream(arguments)
								.map(arg -> arg instanceof ObservableValue ? ((ObservableValue<?>) arg).getValue() : arg)
								.toArray()
				),
				dependencies
		);
	}
}
