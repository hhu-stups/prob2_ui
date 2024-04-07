package de.prob2.ui.simulation.configuration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonPropertyOrder({
	"id",
	"execute",
	// For all other properties, the default order (i. e. field order in class) is used.
})
public class ActivationOperationConfiguration extends DiagramConfiguration {

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

	private String execute;

	private String after;

	private int priority;

	private String additionalGuards;

	private ActivationKind activationKind;

	private Map<String, String> fixedVariables;

	private Object probabilisticVariables;

	private List<String> activating;

	private boolean activatingOnlyWhenExecuted;

	private Map<String, String> updating;

	private String withPredicate;

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

	public void setOpName(String execute) {
		this.execute = execute;
	}

	public String getAfter() {
		return after;
	}

	public void setAfter(String after) {
		this.after = after;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getAdditionalGuards() {
		return additionalGuards;
	}

	public void setAdditionalGuards(String additionalGuards) {
		this.additionalGuards = additionalGuards;
	}

	@JsonIgnore
	public String getAdditionalGuardsAsString() {
		return additionalGuards == null ? "" : additionalGuards;
	}

	public ActivationKind getActivationKind() {
		return activationKind;
	}

	public void setActivationKind(ActivationKind activationKind) {
		this.activationKind = activationKind;
	}

	public Map<String, String> getFixedVariables() {
		return fixedVariables;
	}

	public void setFixedVariables(Map<String, String> fixedVariables) {
		this.fixedVariables = fixedVariables;
	}

	@JsonIgnore
	public String getFixedVariablesAsString() {
		return fixedVariables == null ? "" : fixedVariables.toString();
	}

	public Object getProbabilisticVariables() {
		return probabilisticVariables;
	}

	public void setProbabilisticVariables(Object probabilisticVariables) {
		this.probabilisticVariables = probabilisticVariables;
	}

	@JsonIgnore
	public String getProbabilisticVariablesAsString() {
		return probabilisticVariables == null ? "" : probabilisticVariables.toString();
	}

	public List<String> getActivating() {
		return activating;
	}

	public void setActivating(List<String> activating) {
		this.activating = activating;
	}

	@JsonIgnore
	public String getActivatingAsString() {
		return activating == null ? "" : activating.toString().substring(1, activating.toString().length() - 1);
	}

	public boolean isActivatingOnlyWhenExecuted() {
		return activatingOnlyWhenExecuted;
	}

	public void setActivatingOnlyWhenExecuted(boolean activatingOnlyWhenExecuted) {
		this.activatingOnlyWhenExecuted = activatingOnlyWhenExecuted;
	}

	public Map<String, String> getUpdating() {
		return updating;
	}

	public void setUpdating(Map<String, String> updating) {
		this.updating = updating;
	}

	public String getWithPredicate() {
		return withPredicate;
	}

	public void setWithPredicate(String withPredicate) {
		this.withPredicate = withPredicate;
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
