package de.prob2.ui.verifications.temporal.ctl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.CTL;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.check.CTLChecker;
import de.prob.check.CTLCouldNotDecide;
import de.prob.check.CTLCounterExample;
import de.prob.check.CTLError;
import de.prob.check.CTLNotYetFinished;
import de.prob.check.CTLOk;
import de.prob.check.CheckInterrupted;
import de.prob.check.IModelCheckingResult;
import de.prob.exception.ProBError;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.parserbase.ProBParserBase;
import de.prob.statespace.StateSpace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingResultItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalCheckingResultItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CTLFormulaChecker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CTLFormulaChecker.class);
	
	private CTLFormulaChecker() {
		throw new AssertionError("Utility class");
	}
	
	public static CTL parseFormula(final String code, final ProBParserBase languageSpecificParser) {
		try {
			return new CTL(code, languageSpecificParser);
		} catch (LtlParseException error) {
			final List<ErrorItem> errorMarkers = new ArrayList<>();
			final List<ErrorItem.Location> locations = new ArrayList<>();
			if (error.getTokenString() != null) {
				locations.add(new ErrorItem.Location("", error.getTokenLine(), error.getTokenColumn(), error.getTokenLine(), error.getTokenColumn() + error.getTokenString().length()));
			}
			errorMarkers.add(new ErrorItem(error.getMessage(), ErrorItem.Type.ERROR, locations));
			throw new ProBError(error.getMessage(), errorMarkers, error);
		}
	}
	
	public static CTL parseFormula(final String code, final AbstractModel model) {
		BParser bParser = new BParser();
		if (model instanceof ClassicalBModel) {
			IDefinitions definitions = ((ClassicalBModel) model).getDefinitions();
			bParser.setDefinitions(definitions);
		}
		return CTLFormulaChecker.parseFormula(code, new ClassicalBParser(bParser));
	}
	
	private static void handleFormulaResult(TemporalFormulaItem item, IModelCheckingResult result) {
		assert !(result instanceof CTLError);

		if (result instanceof CTLOk) {
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			}
		} else if (result instanceof CTLCounterExample) {
			item.setCounterExample(null); // TODO
			if(item.getExpectedResult()) {
				item.setResultItem(new CheckingResultItem(Checked.FAIL, "verifications.ltl.result.counterExampleFound.message"));
			} else {
				item.setResultItem(new CheckingResultItem(Checked.SUCCESS, "verifications.ltl.result.succeeded.example.message"));
			}
		} else if (result instanceof CTLNotYetFinished || result instanceof CheckInterrupted || result instanceof  CTLCouldNotDecide) {
			item.setResultItem(new CheckingResultItem(Checked.INTERRUPTED, "common.result.message", result.getMessage()));
		} else {
			throw new AssertionError("Unhandled CTL checking result type: " + result.getClass());
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
	
	public static void checkFormula(TemporalFormulaItem item, StateSpace stateSpace) {
		try {
			final CTL formula = parseFormula(item.getCode(), stateSpace.getModel());
			final CTLChecker checker = new CTLChecker(stateSpace, formula);
			final IModelCheckingResult result = checker.call();
			if (result instanceof CTLError) {
				handleFormulaParseErrors(item, ((CTLError) result).getErrors());
			} else {
				handleFormulaResult(item, result);
			}
		} catch (ProBError error) {
			LOGGER.error("Could not parse LTL formula: ", error);
			handleFormulaParseErrors(item, error.getErrors());
		}
	}
}
