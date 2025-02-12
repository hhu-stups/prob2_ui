package de.prob2.ui.simulation.configuration;

import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
		@JsonSubTypes.Type(ProbabilisticVariables.PerTransition.class),
		@JsonSubTypes.Type(ProbabilisticVariables.PerVariable.class)
})
public sealed interface ProbabilisticVariables permits ProbabilisticVariables.PerTransition, ProbabilisticVariables.PerVariable {

	enum PerTransition implements ProbabilisticVariables {
		/**
		 * Always choose first matching transition.
		 */
		FIRST("first"),
		/**
		 * Select one transition uniformly from all matching transitions.
		 */
		UNIFORM("uniform");

		private final String name;

		PerTransition(String name) {
			this.name = name;
		}

		@JsonValue
		public String getName() {
			return this.name;
		}

		public static PerTransition fromName(String name) {
			return switch (name) {
				case "first" -> FIRST;
				case "uniform" -> UNIFORM;
				default -> throw new IllegalArgumentException("Unknown ProbabilisticVariables value: " + name);
			};
		}
	}

	record PerVariable(
			@JsonUnwrapped Map<String, Map<String, String>> probabilities
	) implements ProbabilisticVariables {
		public PerVariable {
			if (probabilities != null) {
				probabilities = probabilities.entrySet().stream()
						.collect(Collectors.toUnmodifiableMap(
								Map.Entry::getKey,
								e -> Map.copyOf(e.getValue())
						));
			} else {
				probabilities = Map.of();
			}
		}
	}
}
