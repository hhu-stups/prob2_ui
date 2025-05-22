package de.prob2.ui.verifications.temporal.ltl.formula;

import java.util.ArrayList;
import java.util.List;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.LTL;
import de.prob.exception.ProBError;
import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.parserbase.ProBParserBase;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.temporal.ltl.LTLParseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LTLFormulaParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(LTLFormulaParser.class);
	
	private LTLFormulaParser() {
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
		return LTLFormulaParser.parseFormula(code, new ClassicalBParser(bParser), machine.getPatternManager());
	}
}
