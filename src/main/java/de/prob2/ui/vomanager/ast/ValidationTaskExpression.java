package de.prob2.ui.vomanager.ast;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import de.prob.statespace.Trace;
import de.prob.voparser.node.AIdentifierVo;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ITraceTask;
import de.prob2.ui.verifications.IValidationTask;
import de.prob2.ui.vomanager.ValidationTaskNotFound;

public final class ValidationTaskExpression implements IValidationExpression {
	private final IValidationTask task;
	
	public ValidationTaskExpression(IValidationTask task) {
		this.task = Objects.requireNonNull(task, "task");
	}
	
	public static ValidationTaskExpression fromAst(AIdentifierVo ast, Map<String, IValidationTask> tasksInScopeById) {
		String identifier = ast.getIdentifierLiteral().getText();
		IValidationTask validationTask;
		if (tasksInScopeById.containsKey(identifier)) {
			validationTask = tasksInScopeById.get(identifier);
		} else {
			validationTask = new ValidationTaskNotFound(identifier);
		}
		return new ValidationTaskExpression(validationTask);
	}
	
	@Override
	public Stream<? extends IValidationExpression> getChildren() {
		return Stream.empty();
	}
	
	@Override
	public Stream<ValidationTaskExpression> getAllTasks() {
		return Stream.of(this);
	}
	
	public IValidationTask getTask() {
		return task;
	}
	
	@Override
	public CheckingStatus getStatus() {
		return this.getTask().getStatus();
	}

	@Override
	public Trace getTrace() {
		if (this.getTask() instanceof ITraceTask traceTask) {
			return traceTask.getTrace();
		} else {
			// TODO Ideally, this should be detected as a type error ahead of time
			throw new UnsupportedOperationException("This task type cannot produce a trace: " + this.getTask().getClass());
		}
	}

	@Override
	public String toString(){
		return task.toString();
	}

	@Override
	public CompletableFuture<?> check(CheckingExecutors executors, ExecutionContext context) {
		if (this.getTask().getStatus() == CheckingStatus.NOT_CHECKED || this.getTask().getStatus() == CheckingStatus.INTERRUPTED) {
			// Task hasn't been executed yet or was interrupted, so the result will be available after executing it.
			return this.getTask().execute(executors, context);
		} else {
			// Task has already been checked, so the result is available immediately.
			return CompletableFuture.completedFuture(null);
		}
	}
}
