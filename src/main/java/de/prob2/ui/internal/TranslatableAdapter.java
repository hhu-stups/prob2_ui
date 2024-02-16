package de.prob2.ui.internal;

import java.util.function.Function;

@FunctionalInterface
public interface TranslatableAdapter<T> {

	Translatable adapt(T object);

	static <T extends Translatable> TranslatableAdapter<T> identity() {
		return object -> object;
	}

	static <T> TranslatableAdapter<T> adapter(Function<? super T, ? extends String> translationKey) {
		return object -> () -> translationKey.apply(object);
	}

	static <T> TranslatableAdapter<T> adapter(Function<? super T, ? extends String> translationKey, Function<? super T, ? extends Object[]> translationArguments) {
		return object -> new Translatable() {

			@Override
			public String getTranslationKey() {
				return translationKey.apply(object);
			}

			@Override
			public Object[] getTranslationArguments() {
				return translationArguments.apply(object);
			}
		};
	}

	static <T> TranslatableAdapter<T> mappingAdapter(Function<? super T, ? extends Translatable> translatable) {
		return translatable::apply;
	}

	static <T extends Enum<T>> TranslatableAdapter<T> enumNameAdapter(String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return adapter(Enum::name);
		} else {
			return adapter(object -> prefix + '.' + object.name());
		}
	}
}
