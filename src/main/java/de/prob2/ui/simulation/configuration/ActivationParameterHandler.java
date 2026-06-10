package de.prob2.ui.simulation.configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import de.prob2.ui.simulation.EvaluationMode;

@Singleton
public class ActivationParameterHandler {

	public String buildExpressionOrPredicateWithActivationParameters(String expression, Map<String, String> activationParamsVal, EvaluationMode mode) {
		String newExpression = expression;
		if(!activationParamsVal.isEmpty()) {
			switch (mode) {
				case CLASSICAL_B, XTL:
					newExpression = String.format("LET %s BE %s IN %s END",
							String.join(", ", activationParamsVal.keySet()),
							activationParamsVal.entrySet().stream()
									.map(entry -> String.format("%s = %s", entry.getKey(), entry.getValue()))
									.collect(Collectors.joining(" & ")),
							newExpression);

					break;
				case EVENT_B:
					newExpression = String.format(Locale.ROOT, "{x |-> y | x = TRUE & y : ran((%%%s.%s | %s))}(TRUE)",
							String.join(" |-> ", activationParamsVal.keySet()),
							activationParamsVal.entrySet().stream()
									.map(entry -> String.format("%s = %s", entry.getKey(), entry.getValue()))
									.collect(Collectors.joining(" & ")),
							newExpression);
					break;
				default:
					throw new RuntimeException("Evaluation mode is not supported.");
			}
		}
		return newExpression;
	}

}
