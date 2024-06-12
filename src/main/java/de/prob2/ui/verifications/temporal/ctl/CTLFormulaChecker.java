package de.prob2.ui.verifications.temporal.ctl;

import java.util.ArrayList;
import java.util.List;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ClassicalBParser;
import de.be4.classicalb.core.parser.IDefinitions;
import de.be4.ltl.core.parser.LtlParseException;
import de.prob.animator.domainobjects.CTL;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.exception.ProBError;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.parserbase.ProBParserBase;

public final class CTLFormulaChecker {
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
}
