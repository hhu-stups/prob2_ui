package de.prob2.ui.simulation.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;

import de.prob.statespace.Transition;

@JsonPropertyOrder({
		"id",
		"execute",
		"after",
		"priority",
		"additionalGuards",
		"activationKind",
		"fixedVariables",
		"probabilisticVariables",
		"activating",
		"activatingOnlyWhenExecuted",
		"updating",
		"withPredicate"
})
public final class ActivationOperationConfiguration extends DiagramConfiguration.NonUi {

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
			return this.name;
		}

		public static ActivationKind fromName(String name) {
			return switch (name) {
				case "single" -> SINGLE;
				case "single:min" -> SINGLE_MIN;
				case "single:max" -> SINGLE_MAX;
				case "multi" -> MULTI;
				default -> throw new IllegalArgumentException("Unknown activation kind: " + name);
			};
		}
	}

	private String execute;
	private String after;
	private int priority;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String additionalGuards;
	private ActivationKind activationKind;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, String> fixedVariables;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Object probabilisticVariables;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> activating;
	private boolean activatingOnlyWhenExecuted;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Map<String, String> updating;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String withPredicate;

	@JsonCreator
	public ActivationOperationConfiguration(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "execute", required = true) String op,
			@JsonProperty(value = "after", defaultValue = "0") String after,
			@JsonProperty(value = "priority", defaultValue = "0") Integer priority,
			@JsonProperty("additionalGuards") String additionalGuards,
			@JsonProperty(value = "activationKind", defaultValue = "multi") ActivationKind activationKind,
			@JsonProperty("fixedVariables") Map<String, String> fixedVariables,
			@JsonProperty("probabilisticVariables") Object probabilisticVariables,
			@JsonProperty("activating") List<String> activating,
			@JsonProperty(value = "activatingOnlyWhenExecuted", defaultValue = "true") Boolean activatingOnlyWhenExecuted,
			@JsonProperty("updating") Map<String, String> updating,
			@JsonProperty("withPredicate") String withPredicate
	) {
		super(id);
		this.execute = Objects.requireNonNull(op, "execute");
		this.after = after != null && !after.isEmpty() ? after : "0";
		if (Transition.INITIALISE_MACHINE_NAME.equals(this.execute)) {
			this.priority = 1;
		} else if (Transition.SETUP_CONSTANTS_NAME.equals(this.execute)) {
			this.priority = 0;
		} else {
			this.priority = priority != null ? priority : 0;
		}
		this.additionalGuards = additionalGuards != null && !additionalGuards.isEmpty() && !"1=1".equals(additionalGuards) ? additionalGuards : null;
		this.activationKind = activationKind != null ? activationKind : ActivationKind.MULTI;
		this.fixedVariables = fixedVariables != null ? Map.copyOf(fixedVariables) : Map.of();
		this.probabilisticVariables = sanitizeProbabilities(probabilisticVariables);
		this.activating = activating != null ? List.copyOf(activating) : List.of();
		this.activatingOnlyWhenExecuted = activatingOnlyWhenExecuted != null ? activatingOnlyWhenExecuted : true;
		this.updating = updating != null ? Map.copyOf(updating) : Map.of();
		this.withPredicate = withPredicate != null && !withPredicate.isEmpty() && !"1=1".equals(withPredicate) ? withPredicate : null;
	}

	@SuppressWarnings("unchecked")
	private static Object sanitizeProbabilities(Object o) {
		return switch (o) {
			case null -> null;
			case String s -> s;
			case Map<?, ?> m -> {
				Map<String, Map<String, String>> copy = new HashMap<>();
				m.forEach((k, v) -> copy.put((String) k, Map.copyOf((Map<String, String>) v)));
				yield Map.copyOf(copy);
			}
			default -> throw new IllegalArgumentException("Invalid probabilities: " + o);
		};
	}

	@JsonProperty("execute")
	public String getExecute() {
		return this.execute;
	}

	public void setExecute(String execute) {
		this.execute = execute;
	}

	@JsonIgnore
	public String getAfter() {
		return this.after;
	}

	@JsonGetter("after")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String getAfterForJson() {
		return "0".equals(this.after) ? null : this.after;
	}

	public void setAfter(String after) {
		this.after = after != null && !after.isEmpty() ? after : "0";
	}

	@JsonIgnore
	public int getPriority() {
		return this.priority;
	}

	@JsonGetter("priority")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer getPriorityForJson() {
		return Transition.INITIALISE_MACHINE_NAME.equals(this.getExecute()) || Transition.SETUP_CONSTANTS_NAME.equals(this.getExecute()) || this.priority == 0 ? null : this.priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@JsonGetter("additionalGuards")
	public String getAdditionalGuards() {
		return this.additionalGuards;
	}

	public void setAdditionalGuards(String additionalGuards) {
		this.additionalGuards = additionalGuards != null && !additionalGuards.isEmpty() && !"1=1".equals(additionalGuards) ? additionalGuards : null;
	}

	@JsonIgnore
	public ActivationKind getActivationKind() {
		return this.activationKind;
	}

	@JsonGetter("activationKind")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private ActivationKind getActivationKindForJson() {
		return ActivationKind.MULTI.equals(this.activationKind) ? null : this.activationKind;
	}

	public void setActivationKind(ActivationKind activationKind) {
		this.activationKind = activationKind != null ? activationKind : ActivationKind.MULTI;
	}

	@JsonGetter("fixedVariables")
	public Map<String, String> getFixedVariables() {
		return this.fixedVariables;
	}

	public void setFixedVariables(Map<String, String> fixedVariables) {
		this.fixedVariables = fixedVariables != null ? Map.copyOf(fixedVariables) : Map.of();
	}

	@JsonGetter("probabilisticVariables")
	public Object getProbabilisticVariables() {
		return this.probabilisticVariables;
	}

	public void setProbabilisticVariables(Object probabilisticVariables) {
		this.probabilisticVariables = sanitizeProbabilities(probabilisticVariables);
	}

	@JsonGetter("activating")
	public List<String> getActivating() {
		return this.activating;
	}

	public void setActivating(List<String> activating) {
		this.activating = activating != null ? List.copyOf(activating) : List.of();
	}

	@JsonIgnore
	public boolean isActivatingOnlyWhenExecuted() {
		return this.activatingOnlyWhenExecuted;
	}

	@JsonGetter("activatingOnlyWhenExecuted")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean isActivatingOnlyWhenExecutedForJson() {
		return this.activatingOnlyWhenExecuted ? null : false;
	}

	public void setActivatingOnlyWhenExecuted(boolean activatingOnlyWhenExecuted) {
		this.activatingOnlyWhenExecuted = activatingOnlyWhenExecuted;
	}

	@JsonGetter("updating")
	public Map<String, String> getUpdating() {
		return this.updating;
	}

	public void setUpdating(Map<String, String> updating) {
		this.updating = updating != null ? Map.copyOf(updating) : Map.of();
	}

	@JsonGetter("withPredicate")
	public String getWithPredicate() {
		return this.withPredicate;
	}

	public void setWithPredicate(String withPredicate) {
		this.withPredicate = withPredicate != null && !withPredicate.isEmpty() && !"1=1".equals(withPredicate) ? withPredicate : null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ActivationOperationConfiguration that)) {
			return false;
		} else {
			return Objects.equals(this.getId(), that.getId()) && Objects.equals(this.getExecute(), that.getExecute()) && Objects.equals(this.getAfter(), that.getAfter()) && this.getPriority() == that.getPriority() && Objects.equals(this.getAdditionalGuards(), that.getAdditionalGuards()) && Objects.equals(this.getActivationKind(), that.getActivationKind()) && Objects.equals(this.getFixedVariables(), that.getFixedVariables()) && Objects.equals(this.getProbabilisticVariables(), that.getProbabilisticVariables()) && Objects.equals(this.getActivating(), that.getActivating()) && this.isActivatingOnlyWhenExecuted() == that.isActivatingOnlyWhenExecuted() && Objects.equals(this.getUpdating(), that.getUpdating()) && Objects.equals(this.getWithPredicate(), that.getWithPredicate());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getExecute(), this.getAfter(), this.getPriority(), this.getAdditionalGuards(), this.getActivationKind(), this.getFixedVariables(), this.getProbabilisticVariables(), this.getActivating(), this.isActivatingOnlyWhenExecuted(), this.getUpdating(), this.getWithPredicate());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.omitEmptyValues()
				.add("id", this.getId())
				.add("execute", this.getExecute())
				.add("after", this.getAfter())
				.add("priority", this.getPriority())
				.add("additionalGuards", this.getAdditionalGuards())
				.add("activationKind", this.getActivationKind())
				.add("fixedVariables", this.getFixedVariables())
				.add("probabilisticVariables", this.getProbabilisticVariables())
				.add("activating", this.getActivating())
				.add("activatingOnlyWhenExecuted", this.isActivatingOnlyWhenExecuted())
				.add("updating", this.getUpdating())
				.add("withPredicate", this.getWithPredicate())
				.toString();
	}
}
