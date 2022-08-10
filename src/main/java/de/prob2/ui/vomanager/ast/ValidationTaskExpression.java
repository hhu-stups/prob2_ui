package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.AIdentifierVo;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.vomanager.IValidationTask;

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
	public Checked getChecked() {
		return this.getTask().getChecked();
	}
}
