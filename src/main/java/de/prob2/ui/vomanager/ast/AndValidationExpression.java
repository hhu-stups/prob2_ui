package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.AAndVo;
import de.prob2.ui.verifications.Checked;

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
	public Checked getChecked() {
		// Short-circuiting: skip calculating right-hand result
		// if the left-hand result is enough to determine the overall result.
		final Checked leftRes = this.getLeft().getChecked();
		if (leftRes == Checked.INVALID_TASK) {
			return Checked.INVALID_TASK;
		} else if (leftRes == Checked.FAIL) {
			return Checked.FAIL;
		}
		
		final Checked rightRes = this.getRight().getChecked();
		return leftRes.and(rightRes);
	}

	@Override
	public String toString(){
		return left.toString() + " & " + right.toString();
	}
}
