package de.prob2.ui.internal;

import java.util.Collection;
import java.util.random.RandomGenerator;

public final class WeightedRandomHelper {
	private WeightedRandomHelper() {
	}

	public record WeightedValue<T>(T value, double weight) {
		public WeightedValue {
			if (weight < 0 || !Double.isFinite(weight)) {
				throw new IllegalArgumentException("Weight must be non-negative");
			}
		}
	}

	public static <T> T select(RandomGenerator rng, Collection<WeightedValue<T>> values) {
		var total = values.stream().mapToDouble(WeightedValue::weight).sum();
		if (total <= 0 || !Double.isFinite(total)) {
			throw new IllegalArgumentException("There must be at least one value with positive weight");
		}

		var d = rng.nextDouble(total);
		// never select a value with weight zero
		var it = values.stream().filter(v -> v.weight() > 0).iterator();
		while (true) {
			// the first "next()"-call is unchecked, but because we throw on non-positive total
			// there has to be at least one value with positive weight
			var v = it.next();
			d -= v.weight();
			if (d <= 0 || !it.hasNext()) {
				// the "hasNext()"-check has two purposes:
				// 1. loop control
				// 2. fallback to last value when there are rounding errors
				return v.value();
			}
		}
	}
}
