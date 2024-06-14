package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.AOrVo;
import de.prob2.ui.verifications.CheckingStatus;

public final class OrValidationExpression implements IValidationExpression {
	private final IValidationExpression left;
	private final IValidationExpression right;
	
	public OrValidationExpression(final IValidationExpression left, final IValidationExpression right) {
		this.left = left;
		this.right = right;
	}
	
	public static OrValidationExpression fromAst(final AOrVo ast) {
		return new OrValidationExpression(
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
	public String toString(){
		return left.toString() + " or " + right.toString();
	}
}
