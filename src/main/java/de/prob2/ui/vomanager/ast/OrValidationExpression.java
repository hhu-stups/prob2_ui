package de.prob2.ui.vomanager.ast;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import de.prob.statespace.Trace;
import de.prob.voparser.node.AOrVo;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IValidationTask;

public final class OrValidationExpression implements IValidationExpression {
	private final IValidationExpression left;
	private final IValidationExpression right;
	
	public OrValidationExpression(final IValidationExpression left, final IValidationExpression right) {
		this.left = left;
		this.right = right;
	}
	
	public static OrValidationExpression fromAst(AOrVo ast, Map<String, IValidationTask> tasksInScopeById) {
		return new OrValidationExpression(
			IValidationExpression.fromAst(ast.getLeft(), tasksInScopeById),
			IValidationExpression.fromAst(ast.getRight(), tasksInScopeById)
		);
	}
	
	public IValidationExpression getLeft() {
		return this.left;
	}
	
	public IValidationExpression getRight() {
		return this.right;
	}
	
	@Override
	public Stream<? extends IValidationExpression> getChildren() {
		return Stream.of(this.getLeft(), this.getRight());
	}
	
	@Override
	public CheckingStatus getStatus() {
		CheckingStatus leftRes = this.getLeft().getStatus();
		// Short-circuiting: skip calculating right-hand result
		// if the left-hand result is enough to determine the overall result.
		if (leftRes == CheckingStatus.INVALID_TASK) {
			return CheckingStatus.INVALID_TASK;
		} else if (leftRes == CheckingStatus.SUCCESS) {
			return CheckingStatus.SUCCESS;
		}
		
		CheckingStatus rightRes = this.getRight().getStatus();
		return leftRes.or(rightRes);
	}

	@Override
	public Trace getTrace() {
		if (this.getLeft().getStatus() == CheckingStatus.SUCCESS) {
			return this.getLeft().getTrace();
		} else if (this.getRight().getStatus() == CheckingStatus.SUCCESS) {
			return this.getRight().getTrace();
		} else {
			return null;
		}
	}

	@Override
	public String toString(){
		return left.toString() + " or " + right.toString();
	}

	@Override
	public CompletableFuture<?> check(CheckingExecutors executors, ExecutionContext context) {
		return this.getLeft().check(executors, context).thenCompose(r -> {
			CompletableFuture<?> future;
			if (this.getLeft().getStatus() == CheckingStatus.SUCCESS) {
				future = CompletableFuture.completedFuture(r);
			} else {
				future = this.getRight().check(executors, context);
			}
			return future;
		});
	}
}
