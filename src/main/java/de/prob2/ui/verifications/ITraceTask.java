package de.prob2.ui.verifications;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.prob.statespace.Trace;
import de.prob2.ui.vomanager.IValidationTask;

/**
 * An {@link IValidationTask} that may produce a {@link Trace} as a result.
 */
// TODO Once all task results are properly separated from the tasks themselves, this API should be moved to the result classes.
public interface ITraceTask extends IValidationTask {
	/**
	 * Get the trace produced by this task, if any.
	 * If this task has produced multiple traces, only the first one is returned.
	 * If this task hasn't been executed yet or its result doesn't contain a trace, {@code null} is returned.
	 * Note that this says nothing about the task's status -
	 * depending on the task type, a trace may be a successful or a failed result.
	 * 
	 * @return the trace produced by this task, or {@code null} if there is none
	 */
	@JsonIgnore
	Trace getTrace();
}
