package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonPropertyOrder({
	"id",
	"execute",
	// For all other properties, the default order (i. e. field order in class) is used.
})
public class ActivationOperationConfiguration extends ActivationConfiguration {

	public enum ActivationKind {
		SINGLE("single"),
		SINGLE_MIN("single:min"),
		SINGLE_MAX("single:max"),
		MULTI("multi");

		private final String name;

		ActivationKind(String name) {
			this.name = name;
		}

		@JsonValue
		public String getName() {
			return name;
		}

	}

	private final String execute;

	private final String after;

	private final int priority;

	private final String additionalGuards;

	private final ActivationKind activationKind;

	private final Map<String, String> fixedVariables;

	private final Object probabilisticVariables;

	private final List<String> activating;

	private final boolean activatingOnlyWhenExecuted;

	private final Map<String, String> updating;

	private final String withPredicate;

	public ActivationOperationConfiguration(String id, String op, String time, int priority, String additionalGuards, ActivationKind activationKind,
			Map<String, String> fixedVariables, Object probabilisticVariables, List<String> activations, boolean activatingOnlyWhenExecuted,
			Map<String, String> updating, String withPredicate) {
		super(id);
		this.execute = op;
		this.after = time;
		this.priority = priority;
		this.additionalGuards = additionalGuards;
		this.activationKind = activationKind;
		this.fixedVariables = fixedVariables;
		this.probabilisticVariables = probabilisticVariables;
		this.activating = activations;
		this.activatingOnlyWhenExecuted = activatingOnlyWhenExecuted;
		this.updating = updating;
		this.withPredicate = withPredicate;
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

	public boolean isActivatingOnlyWhenExecuted() {
		return activatingOnlyWhenExecuted;
	}

	public Map<String, String> getUpdating() {
		return updating;
	}

	public String getWithPredicate() {
		return withPredicate;
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
		sb.append(", ");
		sb.append("activatingOnlyWhenExecuted");
		sb.append("=");
		sb.append(activatingOnlyWhenExecuted);
		sb.append(")");
		if(updating != null) {
			sb.append(", ");
			sb.append("updating");
			sb.append("=");
			sb.append(updating);
		}
		if(withPredicate != null) {
			sb.append(", ");
			sb.append("withPredicate");
			sb.append("=");
			sb.append(withPredicate);
		}
		return sb.toString();
	}
}
