package de.prob2.ui.verifications.ltl;

import java.util.List;

import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

public class LTLData {

	private List<LTLFormulaItem> formulas;

	private List<LTLPatternItem> patterns;

	public LTLData(List<LTLFormulaItem> formulas, List<LTLPatternItem> patterns) {
		this.formulas = formulas;
		this.patterns = patterns;
	}

	public List<LTLFormulaItem> getFormulas() {
		return formulas;
	}

	public List<LTLPatternItem> getPatterns() {
		return patterns;
	}
}
