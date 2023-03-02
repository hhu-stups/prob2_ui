package de.prob2.ui.verifications.temporal.ltl.formula;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLChecker;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLNotYetFinished;
import de.prob.check.LTLOk;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.parserbase.ProBParserBase;
import de.prob.statespace.StateSpace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalCheckingResultItem;
import de.prob2.ui.verifications.temporal.ltl.LTLParseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class LTLFormulaChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LTLFormulaChecker.class);
	
	private LTLFormulaChecker() {
		throw new AssertionError("Utility class");
	}
	
	public static LTL parseFormula(final String code, final ProBParserBase languageSpecificParser, final PatternManager patternManager) {
		LtlParser parser = new LtlParser(code);
		parser.setPatternManager(patternManager);
		parser.removeErrorListeners();
		LTLParseListener parseListener = new LTLParseListener();
		parser.addErrorListener(parseListener);
		parser.parse();
		
		if (parseListener.getErrorMarkers().isEmpty()) {
			final LTL formula = new LTL(code, languageSpecificParser, parser);
			if (!parseListener.getErrorMarkers().isEmpty()) {
				throw new ProBError(parseListener.getErrorMarkers());
			}
			return formula;
		} else {
			LOGGER.warn("Failed to parse LTL formula using ANTLR-based parser! Retrying using SableCC-based parser without pattern support. Formula: {}", code);
			try {
				return new LTL(code, languageSpecificParser);
			} catch (LtlParseException error) {
				final List<ErrorItem> errorMarkers = new ArrayList<>(parseListener.getErrorMarkers());
				final List<ErrorItem.Location> locations = new ArrayList<>();
				if (error.getTokenString() != null) {
					locations.add(new ErrorItem.Location("", error.getTokenLine(), error.getTokenColumn(), error.getTokenLine(), error.getTokenColumn() + error.getTokenString().length()));
				}
				errorMarkers.add(new ErrorItem(error.getMessage(), ErrorItem.Type.ERROR, locations));
				throw new ProBError(error.getMessage(), errorMarkers, error);
			}
		}
	}
	
	public static LTL parseFormula(final String code, final Machine machine, final AbstractModel model) {
		BParser bParser = new BParser();
		if (model instanceof ClassicalBModel) {
			IDefinitions definitions = ((ClassicalBModel) model).getDefinitions();
			bParser.setDefinitions(definitions);
		}
		return LTLFormulaChecker.parseFormula(code, new ClassicalBParser(bParser), machine.getPatternManager());
	}
	
	private static void handleFormulaResult(TemporalFormulaItem item, IModelCheckingResult result) {
		assert !(result instanceof LTLError);
		
		if (result instanceof LTLCounterExample) {
			item.setCounterExample(((LTLCounterExample)result).getTraceToLoopEntry());
		} else {
			item.setCounterExample(null);
		}
		
		if (result instanceof LTLOk) {
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			}
		} else if (result instanceof LTLCounterExample) {
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.example.message"));
			}
		} else if (result instanceof LTLNotYetFinished || result instanceof CheckInterrupted) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled LTL checking result type: " + result.getClass());
		}
	}
	
	private static void handleFormulaParseErrors(TemporalFormulaItem item, List<ErrorItem> errorMarkers) {
		item.setCounterExample(null);
		String errorMessage = errorMarkers.stream().map(ErrorItem::getMessage).collect(Collectors.joining("\n"));
		if(errorMessage.isEmpty()) {
			errorMessage = "Parse Error in typed formula";
		}
		item.setResultItem(new TemporalCheckingResultItem(Checked.PARSE_ERROR, errorMarkers, "common.result.message", errorMessage));
	}
	
	public static void checkFormula(TemporalFormulaItem item, Machine machine, StateSpace stateSpace) {
		try {
			final LTL formula = parseFormula(item.getCode(), machine, stateSpace.getModel());
			final LTLChecker checker = new LTLChecker(stateSpace, formula);
			final IModelCheckingResult result = checker.call();
			if (result instanceof LTLError) {
				handleFormulaParseErrors(item, ((LTLError)result).getErrors());
			} else {
				handleFormulaResult(item, result);
			}
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(item, error.getErrors());
		}
	}
}
