package de.prob2.ui.vomanager.ast;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import de.prob.statespace.Trace;
import de.prob.voparser.node.AAndVo;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;

public final class AndValidationExpression implements IValidationExpression {
	private final IValidationExpression left;
	private final IValidationExpression right;
	
	public AndValidationExpression(final IValidationExpression left, final IValidationExpression right) {
		this.left = left;
		this.right = right;
	}
	
	public static AndValidationExpression fromAst(final AAndVo ast) {
		return new AndValidationExpression(
			IValidationExpression.fromAst(ast.getLeft()),
			IValidationExpression.fromAst(ast.getRight())
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
		// Short-circuiting: skip calculating right-hand result
		// if the left-hand result is enough to determine the overall result.
		CheckingStatus leftRes = this.getLeft().getStatus();
		if (leftRes == CheckingStatus.INVALID_TASK) {
			return CheckingStatus.INVALID_TASK;
		} else if (leftRes == CheckingStatus.FAIL) {
			return CheckingStatus.FAIL;
		}
		
		CheckingStatus rightRes = this.getRight().getStatus();
		return leftRes.and(rightRes);
	}

	@Override
	public Trace getTrace() {
		// TODO Ideally, this should be detected as a type error ahead of time
		throw new UnsupportedOperationException("A conjunction expression does not produce a trace");
	}

	@Override
	public String toString(){
		return left.toString() + " & " + right.toString();
	}

	@Override
	public CompletableFuture<?> check(CheckingExecutors executors, ExecutionContext context) {
		return this.getLeft().check(executors, context).thenCompose(r -> {
			CompletableFuture<?> future;
			if (this.getLeft().getStatus() == CheckingStatus.SUCCESS) {
				future = this.getRight().check(executors, context);
			} else {
				future = CompletableFuture.completedFuture(r);
			}
			return future;
		});
	}
}
