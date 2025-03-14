package de.prob2.ui.simulation.configuration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public abstract sealed class DiagramConfiguration permits DiagramConfiguration.NonUi, UIListenerConfiguration {

	protected String id;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected String comment;

	protected DiagramConfiguration(String id, String comment) {
		this.id = Objects.requireNonNull(id, "id");
		this.comment = comment;
	}

	@JsonGetter("id")
	public String getId() {
		return this.id;
	}

	@JsonGetter("comment")
	public String getComment() {
		return comment;
	}

	public void setId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	// Should be used in the future to add comments via ProB2-UI
	public void setComment(String comment) {
		this.comment = comment;
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
	@JsonSubTypes({
			@JsonSubTypes.Type(ActivationOperationConfiguration.class),
			@JsonSubTypes.Type(ActivationChoiceConfiguration.class)
	})
	public static abstract sealed class NonUi extends DiagramConfiguration permits ActivationOperationConfiguration, ActivationChoiceConfiguration {

		protected NonUi(String id, String comment) {
			super(id, comment);
		}
	}
}
