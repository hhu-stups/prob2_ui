package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.ASequentialVo;
import de.prob2.ui.verifications.Checked;

public final class SequentialValidationExpression implements IValidationExpression {
	private final IValidationExpression left;
	private final IValidationExpression right;
	
	public SequentialValidationExpression(final IValidationExpression left, final IValidationExpression right) {
		this.left = left;
		this.right = right;
	}
	
	public static SequentialValidationExpression fromAst(final ASequentialVo ast) {
		return new SequentialValidationExpression(
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
		final Checked leftRes = this.getLeft().getChecked();
		if (leftRes == Checked.SUCCESS) {
			return this.getRight().getChecked();
		} else {
			return leftRes;
		}
	}

	@Override
	public String toString(){
		return left.toString() + ";" + right.toString();
	}
}
