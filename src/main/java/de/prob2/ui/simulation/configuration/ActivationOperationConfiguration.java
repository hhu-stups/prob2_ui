package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
	"id",
	"execute",
	// For all other properties, the default order (i. e. field order in class) is used.
})
public class ActivationOperationConfiguration extends ActivationConfiguration {

	public enum ActivationKind {
		SINGLE, SINGLE_MIN, SINGLE_MAX, MULTI
	}

	private final String execute;

	private final String after;

	private final int priority;

	private final String additionalGuards;

	private final ActivationKind activationKind;

	private final Map<String, String> fixedVariables;

	private final Object probabilisticVariables;

	private final List<String> activating;

	public ActivationOperationConfiguration(String id, String op, String time, int priority, String additionalGuards, ActivationKind activationKind,
			Map<String, String> fixedVariables, Object probabilisticVariables, List<String> activations) {
		super(id);
		this.execute = op;
		this.after = time;
		this.priority = priority;
		this.additionalGuards = additionalGuards;
		this.activationKind = activationKind;
		this.fixedVariables = fixedVariables;
		this.probabilisticVariables = probabilisticVariables;
		this.activating = activations;
	}

	@JsonProperty("execute")
	public String getOpName() {
		return execute;
	}

	public String getAfter() {
		return after;
	}

	public int getPriority() {
		return priority;
	}

	public String getAdditionalGuards() {
		return additionalGuards;
	}

	public ActivationKind getActivationKind() {
		return activationKind;
	}

	public Map<String, String> getFixedVariables() {
		return fixedVariables;
	}

	public Object getProbabilisticVariables() {
		return probabilisticVariables;
	}

	public List<String> getActivating() {
		return activating;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ActivationOperationConfiguration(");
		sb.append("id");
		sb.append("=");
		sb.append(id);
		sb.append(", ");
		sb.append("execute");
		sb.append("=");
		sb.append(execute);
		sb.append(", ");
		sb.append("after");
		sb.append("=");
		sb.append(after);
		sb.append(", ");
		sb.append("priority");
		sb.append("=");
		sb.append(priority);
		sb.append(", ");
		if(additionalGuards != null) {
			sb.append("additionalGuards");
			sb.append("=");
			sb.append(additionalGuards);
			sb.append(", ");
		}
		sb.append("activationKind");
		sb.append("=");
		sb.append(activationKind);

		if(fixedVariables != null) {
			sb.append(", ");
			sb.append("fixedVariables");
			sb.append("=");
			sb.append(fixedVariables);
			sb.append(", ");
		}
		if(probabilisticVariables != null) {
			sb.append(", ");
			sb.append("probabilisticVariables");
			sb.append("=");
			sb.append(probabilisticVariables);
			sb.append(", ");
		}
		if(activating != null) {
			sb.append(", ");
			sb.append("activating");
			sb.append("=");
			sb.append(activating);
		}
		sb.append(")");
		return sb.toString();
	}
}
