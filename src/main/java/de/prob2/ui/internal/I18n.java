package de.prob2.ui.internal;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internationalization class.
 */
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
	 * Translates the given object with the given {@link TranslatableAdapter}.
	 *
	 * @param adapter adapter
	 * @param object  translatable object
	 * @return formatted and translated string representation
	 */
	public <T> String translate(TranslatableAdapter<T> adapter, T object) {
		Objects.requireNonNull(adapter, "adapter");
		Objects.requireNonNull(object, "object");
		return translate(adapter.adapt(object));
	}

	/**
	 * Translates the given {@link Translatable} object.
	 *
	 * @param object translatable object
	 * @return formatted and translated string representation
	 */
	public String translate(Translatable object) {
		Objects.requireNonNull(object, "object");
		return translate(object.getTranslationKey(), object.getTranslationArguments());
	}

	/**
	 * First translates the given key to obtain a pattern, then formats the pattern via MessageFormat with the given arguments.
	 * The arguments will be evaluated before use: {@link ObservableValue}s will be queried and {@link Translatable}/{@link Formattable} objects will be resolved.
	 *
	 * @param key       key to translate
	 * @param arguments arguments for formatting
	 * @return formatted and translated string
	 */
	public String translate(String key, Object... arguments) {
		String pattern = translate0(key);
		Object[] evaluatedArguments = evaluateArguments(arguments);
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(evaluatedArguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting pattern '{}' for given key '{}'", pattern, key, e);
			return pattern;
		}
	}

	/**
	 * Formats the given object with the given {@link FormattableAdapter}.
	 *
	 * @param adapter adapter
	 * @param object  formattable object
	 * @return formatted string representation
	 */
	public <T> String format(FormattableAdapter<T> adapter, T object) {
		Objects.requireNonNull(adapter, "adapter");
		Objects.requireNonNull(object, "object");
		return format(adapter.adapt(object));
	}

	/**
	 * Formats the given {@link Formattable} object.
	 *
	 * @param object formattable object
	 * @return formatted string representation
	 */
	public String format(Formattable object) {
		Objects.requireNonNull(object, "object");
		return format(object.getFormattingPattern(), object.getFormattingArguments());
	}

	/**
	 * Formats a given pattern via MessageFormat with the given arguments without translating.
	 * The arguments will be evaluated before use: {@link ObservableValue}s will be queried and {@link Translatable}/{@link Formattable} objects will be resolved.
	 *
	 * @param pattern   pattern
	 * @param arguments arguments for formatting
	 * @return formatted string
	 */
	public String format(String pattern, Object... arguments) {
		Objects.requireNonNull(pattern, "pattern");
		Object[] evaluatedArguments = evaluateArguments(arguments);
		try {
			MessageFormat mf = new MessageFormat(pattern, locale());
			return mf.format(evaluatedArguments);
		} catch (Exception e) {
			LOGGER.error("Error while formatting given pattern '{}'", pattern, e);
			return pattern;
		}
	}

	/**
	 * Generates a string binding for the given object with the given {@link TranslatableAdapter}, according to the rules of {@link I18n#translateBinding(String, Object...)}.
	 *
	 * @param adapter adapter
	 * @param object  translatable object
	 * @return binding for the formatted and translated string representation
	 */
	public <T> StringBinding translateBinding(TranslatableAdapter<T> adapter, T object) {
		Objects.requireNonNull(adapter, "adapter");
		Objects.requireNonNull(object, "object");
		return translateBinding(adapter.adapt(object));
	}

	/**
	 * Generates a string binding for the given {@link Translatable} object, according to the rules of {@link I18n#translateBinding(String, Object...)}.
	 *
	 * @param object translatable object
	 * @return binding for the formatted and translated string representation
	 */
	public StringBinding translateBinding(Translatable object) {
		Objects.requireNonNull(object, "object");
		return translateBinding(object.getTranslationKey(), object.getTranslationArguments());
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
		Object[] copiedArguments = Arrays.copyOf(arguments, arguments.length);
		ObservableValue<?>[] dependencies = collectDependencies(key, copiedArguments);
		return Bindings.createStringBinding(
				() -> {
					try {
						return translate(key, evaluateArguments(copiedArguments));
					} catch (NullPointerException ignored) {
						return ""; // return a dummy value, because Bindings.when always evaluates both expressions
					}
				},
				dependencies
		);
	}

	/**
	 * Generates a string binding for the given translation, according to the rules of {@link I18n#translate(String, Object...)}.
	 * Observable value in the key and arguments will be evaluated and the translation will be redone when one of them changes.
	 *
	 * @param key       key to translate
	 * @param arguments arguments for formatting (may be observable)
	 * @return binding for the formatted and translated string
	 */
	public StringBinding translateBinding(ObservableValue<String> key, Object... arguments) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(arguments, "arguments");
		Object[] copiedArguments = Arrays.copyOf(arguments, arguments.length);
		ObservableValue<?>[] dependencies = collectDependencies(key, copiedArguments);
		return Bindings.createStringBinding(
				() -> {
					try {
						return translate(key.getValue(), evaluateArguments(copiedArguments));
					} catch (NullPointerException ignored) {
						return ""; // return a dummy value, because Bindings.when always evaluates both expressions
					}
				},
				dependencies
		);
	}

	/**
	 * Generates a string binding for the given object with the given {@link FormattableAdapter}, according to the rules of {@link I18n#formatBinding(String, Object...)}.
	 *
	 * @param adapter adapter
	 * @param object  translatable object
	 * @return binding for the formatted string representation
	 */
	public <T> StringBinding formatBinding(FormattableAdapter<T> adapter, T object) {
		Objects.requireNonNull(adapter, "adapter");
		Objects.requireNonNull(object, "object");
		return formatBinding(adapter.adapt(object));
	}

	/**
	 * Generates a string binding for the given {@link Formattable} object, according to the rules of {@link I18n#formatBinding(String, Object...)}.
	 *
	 * @param object formattable object
	 * @return binding for the formatted string representation
	 */
	public StringBinding formatBinding(Formattable object) {
		Objects.requireNonNull(object, "object");
		return translateBinding(object.getFormattingPattern(), object.getFormattingArguments());
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
		Object[] copiedArguments = Arrays.copyOf(arguments, arguments.length);
		ObservableValue<?>[] dependencies = collectDependencies(pattern, copiedArguments);
		return Bindings.createStringBinding(
				() -> {
					try {
						return format(pattern, evaluateArguments(copiedArguments));
					} catch (NullPointerException ignored) {
						return ""; // return a dummy value, because Bindings.when always evaluates both expressions
					}
				},
				dependencies
		);
	}

	/**
	 * Generates a string binding for the given formatting, according to the rules of {@link I18n#format(String, Object...)}.
	 * Observable value in the pattern or arguments will be evaluated and the formatting will be redone when one of them changes.
	 *
	 * @param pattern   pattern
	 * @param arguments arguments for formatting (may be observable)
	 * @return binding for the formatted string
	 */
	public StringBinding formatBinding(ObservableValue<String> pattern, Object... arguments) {
		Objects.requireNonNull(pattern, "pattern");
		Objects.requireNonNull(arguments, "arguments");
		Object[] copiedArguments = Arrays.copyOf(arguments, arguments.length);
		ObservableValue<?>[] dependencies = collectDependencies(pattern, copiedArguments);
		return Bindings.createStringBinding(
				() -> {
					try {
						return format(pattern.getValue(), evaluateArguments(copiedArguments));
					} catch (NullPointerException ignored) {
						return ""; // return a dummy value, because Bindings.when always evaluates both expressions
					}
				},
				dependencies
		);
	}

	/**
	 * String converter that translates, according to the rules of {@link I18n#translate(String, Object...)}.
	 *
	 * @return string converter
	 */
	public <T extends Translatable> StringConverter<T> translateConverter() {
		return translateConverter((String) null);
	}

	/**
	 * String converter that translates, according to the rules of {@link I18n#translate(String, Object...)}.
	 *
	 * @param adapter adapter to make the objects translatable
	 * @return string converter
	 */
	public <T> StringConverter<T> translateConverter(TranslatableAdapter<? super T> adapter) {
		return translateConverter(adapter, null);
	}

	/**
	 * String converter that translates, according to the rules of {@link I18n#translate(String, Object...)}.
	 *
	 * @param nullTranslationKey optional translation key used for null objects
	 * @return string converter
	 */
	public <T extends Translatable> StringConverter<T> translateConverter(String nullTranslationKey) {
		return new StringConverter<T>() {

			@Override
			public String toString(T object) {
				if (object != null) {
					return translate(object);
				}

				if (nullTranslationKey != null) {
					return translate(nullTranslationKey);
				}

				return "";
			}

			@Override
			public T fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String not supported");
			}
		};
	}

	/**
	 * String converter that translates, according to the rules of {@link I18n#translate(String, Object...)}.
	 *
	 * @param adapter            adapter to make the objects translatable
	 * @param nullTranslationKey optional translation key used for null objects
	 * @return string converter
	 */
	public <T> StringConverter<T> translateConverter(TranslatableAdapter<? super T> adapter, String nullTranslationKey) {
		Objects.requireNonNull(adapter, "adapter");
		return new StringConverter<T>() {

			@Override
			public String toString(T object) {
				if (object != null) {
					Translatable t = adapter.adapt(object);
					if (t != null) {
						return translate(t);
					}
				}

				if (nullTranslationKey != null) {
					return translate(nullTranslationKey);
				}

				return "";
			}

			@Override
			public T fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String not supported");
			}
		};
	}

	/**
	 * String converter that formats, according to the rules of {@link I18n#format(String, Object...)}.
	 *
	 * @return string converter
	 */
	public <T extends Formattable> StringConverter<T> formatConverter() {
		return formatConverter((String) null);
	}

	/**
	 * String converter that formats, according to the rules of {@link I18n#format(String, Object...)}.
	 *
	 * @param adapter adapter to make the objects formattable
	 * @return string converter
	 */
	public <T> StringConverter<T> formatConverter(FormattableAdapter<? super T> adapter) {
		return formatConverter(adapter, null);
	}

	/**
	 * String converter that formats, according to the rules of {@link I18n#format(String, Object...)}.
	 *
	 * @param nullTranslationKey optional translation key used for null objects
	 * @return string converter
	 */
	public <T extends Formattable> StringConverter<T> formatConverter(String nullTranslationKey) {
		return new StringConverter<T>() {

			@Override
			public String toString(T object) {
				if (object != null) {
					return format(object);
				}

				if (nullTranslationKey != null) {
					return translate(nullTranslationKey);
				}

				return "";
			}

			@Override
			public T fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String not supported");
			}
		};
	}

	/**
	 * String converter that formats, according to the rules of {@link I18n#format(String, Object...)}.
	 *
	 * @param adapter            adapter to make the objects formattable
	 * @param nullTranslationKey optional translation key used for null objects
	 * @return string converter
	 */
	public <T> StringConverter<T> formatConverter(FormattableAdapter<? super T> adapter, String nullTranslationKey) {
		Objects.requireNonNull(adapter, "adapter");
		return new StringConverter<T>() {

			@Override
			public String toString(T object) {
				if (object != null) {
					Formattable f = adapter.adapt(object);
					if (f != null) {
						return format(f);
					}
				}

				if (nullTranslationKey != null) {
					return translate(nullTranslationKey);
				}

				return "";
			}

			@Override
			public T fromString(String string) {
				throw new UnsupportedOperationException("Conversion from String not supported");
			}
		};
	}

	private Object evaluateArgument(Object arg) {
		if (arg == null) {
			return "null";
		} else if (arg instanceof Translatable) {
			return translate((Translatable) arg);
		} else if (arg instanceof Formattable) {
			return format((Formattable) arg);
		} else if (arg instanceof ObservableValue) {
			return evaluateArgument(((ObservableValue<?>) arg).getValue());
		}

		return arg;
	}

	private Object[] evaluateArguments(Object... arguments) {
		Objects.requireNonNull(arguments, "arguments");
		return Arrays.stream(arguments)
				       .map(this::evaluateArgument)
				       .toArray();
	}

	private static ObservableValue<?>[] collectDependencies(Object keyOrPattern, Object... arguments) {
		return Stream.concat(Stream.of(keyOrPattern), Arrays.stream(arguments))
				       .filter(arg -> arg instanceof ObservableValue)
				       .map(arg -> (ObservableValue<?>) arg)
				       .toArray(ObservableValue[]::new);
	}
}
