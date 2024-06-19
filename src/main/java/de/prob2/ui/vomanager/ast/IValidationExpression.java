package de.prob2.ui.vomanager.ast;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import de.prob.statespace.Trace;
import de.prob.voparser.VOParser;
import de.prob.voparser.node.AAndVo;
import de.prob.voparser.node.AIdentifierVo;
import de.prob.voparser.node.AOrVo;
import de.prob.voparser.node.PVo;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.vomanager.IValidationTask;

public interface IValidationExpression {
	static IValidationExpression fromAst(final PVo ast) {
		if (ast instanceof AIdentifierVo) {
			return ValidationTaskExpression.fromAst((AIdentifierVo)ast);
		} else if (ast instanceof AAndVo) {
			return AndValidationExpression.fromAst((AAndVo)ast);
		} else if (ast instanceof AOrVo) {
			return OrValidationExpression.fromAst((AOrVo)ast);
		} else {
			throw new IllegalArgumentException("Unhandled VO expression type: " + ast.getClass());
		}
	}
	
	static IValidationExpression parse(final String expression) {
		return fromAst(VOParser.parse(expression).getPVo());
	}
	
	Stream<? extends IValidationExpression> getChildren();
	
	default Stream<ValidationTaskExpression> getAllTasks() {
		return this.getChildren().flatMap(IValidationExpression::getAllTasks);
	}
	
	void resolveTaskIds(Map<String, IValidationTask> tasksInScopeById);
	
	CheckingStatus getStatus();
	
	Trace getTrace();
	
	CompletableFuture<?> check(CheckingExecutors executors, ExecutionContext context);
}
