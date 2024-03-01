package de.prob2.ui.verifications;

public enum Checked {
	/**
	 * The task did not run yet.
	 * This status indicates nothing about the task's result.
	 */
	NOT_CHECKED,
	
	/**
	 * The task ran and completed with a definitely successful result.
	 * Re-running the task will not change this result,
	 * unless the model or the task itself changes.
	 */
	SUCCESS,
	
	/**
	 * The task ran and completed with a definitely failed result.
	 * Re-running the task will not change this result,
	 * unless the model or the task itself changes.
	 */
	FAIL,
	
	/**
	 * The task ran,
	 * but reached a user-defined limit before it could complete.
	 * This is usually a time limit (timeout),
	 * but can also be e. g. a state limit for explicit-state model checking.
	 * This result indicates that the task has not failed <em>yet</em>,
	 * but it also hasn't definitely succeeded,
	 * so it may still fail if it continued running.
	 * Re-running the task with a higher limit may yield a definitive result.
	 */
	TIMEOUT,
	
	/**
	 * The task ran,
	 * but was interrupted by the user before it could complete.
	 * This result indicates that the task has not failed <em>yet</em>,
	 * but it also hasn't definitely succeeded,
	 * so it may still fail if it continued running.
	 * Re-running the task without interrupting it may yield a definitive result.
	 */
	INTERRUPTED,
	
	/**
	 * The task itself is invalid and cannot be run.
	 * This is usually because of a parse error in a formula contained in the task.
	 * Either the model or the task must be adjusted to make the task valid.
	 */
	INVALID_TASK,
	;
	
	public Checked and(final Checked other) {
		if (this == INVALID_TASK || this == FAIL) {
			return this;
		} else if (other == INVALID_TASK || other == FAIL) {
			return other;
		} else if (this == TIMEOUT || this == INTERRUPTED) {
			return this;
		} else if (this == SUCCESS) {
			return other;
		} else {
			return this;
		}
	}
	
	public Checked or(Checked other) {
		if (this == INVALID_TASK || other == INVALID_TASK) {
			// Special case: always propagate "invalid task" results,
			// even if the other operand is not an error,
			// to make it obvious when a task is not configured correctly.
			return INVALID_TASK;
		} else if (this == SUCCESS) {
			return this;
		} else if (this == FAIL) {
			return other;
		} else if (other == FAIL) {
			return this;
		} else if (this == TIMEOUT || this == INTERRUPTED) {
			return other;
		} else if (other == TIMEOUT || other == INTERRUPTED) {
			return this;
		} else {
			return other;
		}
	}
}
