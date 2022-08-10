package de.prob2.ui.vomanager.ast;

import java.util.stream.Stream;

import de.prob.voparser.VOParseException;
import de.prob.voparser.VOParser;
import de.prob.voparser.node.AAndVo;
import de.prob.voparser.node.AEquivalentVo;
import de.prob.voparser.node.AIdentifierVo;
import de.prob.voparser.node.AImpliesVo;
import de.prob.voparser.node.ANotVo;
import de.prob.voparser.node.AOrVo;
import de.prob.voparser.node.ASequentialVo;
import de.prob.voparser.node.PVo;
import de.prob.voparser.node.Start;
import de.prob2.ui.verifications.Checked;

public interface IValidationExpression {
	public static IValidationExpression fromAst(final PVo ast) {
		if (ast instanceof AIdentifierVo) {
			return ValidationTaskExpression.fromAst((AIdentifierVo)ast);
		} else if (ast instanceof ANotVo) {
			return NotValidationExpression.fromAst((ANotVo)ast);
		} else if (ast instanceof AAndVo) {
			return AndValidationExpression.fromAst((AAndVo)ast);
		} else if (ast instanceof AOrVo) {
			return OrValidationExpression.fromAst((AOrVo)ast);
		} else if (ast instanceof AImpliesVo) {
			return OrValidationExpression.fromAst((AImpliesVo)ast);
		} else if (ast instanceof AEquivalentVo) {
			return AndValidationExpression.fromAst((AEquivalentVo)ast);
		} else if (ast instanceof ASequentialVo) {
			return SequentialValidationExpression.fromAst((ASequentialVo)ast);
		} else {
			throw new IllegalArgumentException("Unhandled VO expression type: " + ast.getClass());
		}
	}
	
	public static IValidationExpression parse(final VOParser parser, final String expression) throws VOParseException {
		final Start ast = parser.parseFormula(expression);
		//voParser.semanticCheck(ast); // TODO
		return fromAst(ast.getPVo());
	}
	
	public abstract Stream<? extends IValidationExpression> getChildren();
	
	public default Stream<ValidationTaskExpression> getAllTasks() {
		return this.getChildren().flatMap(IValidationExpression::getAllTasks);
	}
	
	public abstract Checked getChecked();
}
