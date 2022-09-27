package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.AOrVo;
import de.prob2.ui.verifications.Checked;

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
	public Checked getChecked() {
		final Checked leftRes = this.getLeft().getChecked();
		if (leftRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (leftRes == Checked.SUCCESS) {
			return Checked.SUCCESS;
		}
		
		final Checked rightRes = this.getRight().getChecked();
		if (rightRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (rightRes == Checked.SUCCESS) {
			return Checked.SUCCESS;
		}
		
		if (leftRes == Checked.FAIL && rightRes == Checked.FAIL) {
			return Checked.FAIL;
		} else {
			return Checked.UNKNOWN;
		}
	}

	@Override
	public String toString(){
		return left.toString() + " or " + right.toString();
	}
}
