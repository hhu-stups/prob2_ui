package de.prob2.ui.vomanager.ast;

import java.util.Map;
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
	private final String identifier;
	
	private IValidationTask task;
	
	public ValidationTaskExpression(final String identifier) {
		this.identifier = identifier;
	}
	
	public static ValidationTaskExpression fromAst(final AIdentifierVo ast) {
		return new ValidationTaskExpression(ast.getIdentifierLiteral().getText());
	}
	
	public String getIdentifier() {
		return this.identifier;
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
	
	public void setTask(final IValidationTask task) {
		this.task = task;
	}
	
	@Override
	public void resolveTaskIds(Map<String, IValidationTask> tasksInScopeById) {
		IValidationTask validationTask;
		if (tasksInScopeById.containsKey(this.getIdentifier())) {
			validationTask = tasksInScopeById.get(this.getIdentifier());
		} else {
			validationTask = new ValidationTaskNotFound(this.getIdentifier());
		}
		this.setTask(validationTask);
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
		return this.getTask().execute(executors, context);
	}
}
