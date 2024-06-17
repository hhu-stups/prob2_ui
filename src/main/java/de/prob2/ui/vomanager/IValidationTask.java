package de.prob2.ui.vomanager;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.type.ValidationTaskType;
import de.prob2.ui.verifications.type.ValidationTaskTypeResolver;

import javafx.beans.property.ReadOnlyObjectProperty;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "taskType")
@JsonTypeIdResolver(ValidationTaskTypeResolver.class)
public interface IValidationTask {
	String getId();

	@JsonIgnore
	ValidationTaskType<?> getTaskType();

	@JsonIgnore
	String getTaskType(I18n i18n);

	@JsonIgnore
	String getTaskDescription(I18n i18n);

	ReadOnlyObjectProperty<CheckingStatus> statusProperty();

	@JsonIgnore
	CheckingStatus getStatus();

	CompletableFuture<?> execute(CheckingExecutors executors, ExecutionContext context);

	/**
	 * Reset the parts this task's checking state that depend on the current animator instance ({@link StateSpace}),
	 * such as {@link Trace} and {@link State} objects.
	 * Unlike {@link #reset()}, this method doesn't reset {@link #statusProperty()}
	 * and preserves other task-specific state that is independent of the current animator.
	 */
	void resetAnimatorDependentState();

	/**
	 * Reset this task's entire checking state.
	 * This sets {@link #statusProperty()} to {@link CheckingStatus#NOT_CHECKED}
	 * and sets all other similar task-specific state to its initial values.
	 * All state that is reset by {@link #resetAnimatorDependentState()} is also reset by this method.
	 */
	void reset();

	/**
	 * This method should be used to check equality for serialisation and editing.
	 * All of these properties should be final!
	 * Properties that are mutable, transient, derived or only used for caching should be ignored.
	 *
	 * <ul>
	 *     <li>two tasks which have different IDs but are otherwise the same should return false</li>
	 *     <li>two tasks which have different status but are otherwise the same should return true</li>
	 * </ul>
	 *
	 * @param other other object
	 * @return true iff this == that wrt the constraints above
	 */
	boolean settingsEqual(Object other);
}
