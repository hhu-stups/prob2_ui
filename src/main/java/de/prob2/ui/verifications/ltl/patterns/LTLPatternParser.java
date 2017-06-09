package de.prob2.ui.verifications.ltl.patterns;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.semantic.PatternDefinition;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
						
	public void parsePattern(LTLPatternItem item, PatternManager patternManager) {
		logger.trace("Parse ltl pattern");
		LtlParser parser = new LtlParser(item.getCode());
		LTLParseListener parseListener = new LTLParseListener();
		parser.setPatternManager(patternManager);		
		parser.removeErrorListeners();
		parser.removeWarningListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		patternManager.removeUpdateListeners();
		patternManager.addUpdateListener(parseListener);
		parser.parse();
		System.out.println(patternManager.getPatterns().size());
	}
		

	private ArrayList<LTLPatternMarker> getPatternMarkers(List<PatternDefinition> patterns) {
		// TODO Auto-generated method stub
		ArrayList<LTLPatternMarker> markers = new ArrayList<LTLPatternMarker>();
		for (PatternDefinition pattern : patterns) {
			String msg = String.format("Move pattern '%s' to pattern manager.", pattern.getName());
			markers.add(new LTLPatternMarker("pattern", pattern.getContext().start, pattern.getContext().stop, pattern.getSimpleName(), msg));
		}
		return markers;
	}
}
