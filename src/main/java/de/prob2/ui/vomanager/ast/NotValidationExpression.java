package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.node.ANotVo;
import de.prob2.ui.verifications.Checked;

public final class NotValidationExpression implements IValidationExpression {
	private final IValidationExpression expression;
	
	public NotValidationExpression(final IValidationExpression expression) {
		this.expression = expression;
	}
	
	public static NotValidationExpression fromAst(final ANotVo ast) {
		return new NotValidationExpression(IValidationExpression.fromAst(ast.getVo()));
	}
	
	public IValidationExpression getExpression() {
		return this.expression;
	}
	
	@Override
	public Stream<? extends IValidationExpression> getChildren() {
		return Stream.of(this.getExpression());
	}
	
	@Override
	public Checked getChecked() {
		final Checked exprRes = this.getExpression().getChecked();
		if (exprRes == Checked.PARSE_ERROR) {
			return Checked.PARSE_ERROR;
		} else if (exprRes == Checked.SUCCESS) {
			return Checked.FAIL;
		} else if (exprRes == Checked.FAIL) {
			return Checked.SUCCESS;
		} else {
			return Checked.UNKNOWN;
		}
	}

	@Override
	public String toString(){
		return "!"+ expression.getChildren().toString();
	}
}
