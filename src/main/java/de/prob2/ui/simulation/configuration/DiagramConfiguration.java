package de.prob2.ui.simulation.configuration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public abstract sealed class DiagramConfiguration permits DiagramConfiguration.NonUi, UIListenerConfiguration {

	protected String id;

	protected DiagramConfiguration(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	@JsonGetter("id")
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
	@JsonSubTypes({
			@JsonSubTypes.Type(ActivationOperationConfiguration.class),
			@JsonSubTypes.Type(ActivationChoiceConfiguration.class)
	})
	public static abstract sealed class NonUi extends DiagramConfiguration permits ActivationOperationConfiguration, ActivationChoiceConfiguration {

		protected NonUi(String id) {
			super(id);
		}
	}
}
