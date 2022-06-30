package de.prob2.ui.internal;

import java.util.function.Function;

@FunctionalInterface
public interface FormattableAdapter<T> {

	Formattable adapt(T object);

	static <T extends Formattable> FormattableAdapter<T> identity() {
		return object -> object;
	}

	static <T> FormattableAdapter<T> adapter(Function<? super T, ? extends String> formattingPattern) {
		return object -> () -> formattingPattern.apply(object);
	}

	static <T> FormattableAdapter<T> adapter(Function<? super T, ? extends String> formattingPattern, Function<? super T, ? extends Object[]> formattingArguments) {
		return object -> new Formattable() {

			@Override
			public String getFormattingPattern() {
				return formattingPattern.apply(object);
			}

			@Override
			public Object[] getFormattingArguments() {
				return formattingArguments.apply(object);
			}
		};
	}

	static <T> FormattableAdapter<T> mappingAdapter(Function<? super T, ? extends Formattable> formattable) {
		return formattable::apply;
	}
}
