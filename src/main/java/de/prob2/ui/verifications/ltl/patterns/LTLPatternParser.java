package de.prob2.ui.verifications.ltl.patterns;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.ltl.parser.LtlBaseListener;
import de.prob.ltl.parser.LtlParser;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.semantic.PatternDefinition;
import de.prob2.ui.project.machines.Machine;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
		
	private final LTLParseListener listener;
	
	@Inject
	private LTLPatternParser() {
		this.listener = new LTLParseListener();
	}
	
	/*public void parseMachine(Machine machine) {
		for(LTLPatternItem item : machine.getPatterns()) {
			parsePattern(item);
		}
	}*/
	
	public void addPattern(LTLPatternItem item) {
		
	}
	
	public void parsePattern(LTLPatternItem item, PatternManager patternManager) {
		logger.trace("Parse ltl pattern");
		LtlParser parser = new LtlParser(item.getCode());
		/*Pattern oldPattern = patternManager.getUserPattern(item.getName());
		if(oldPattern != null) {
			patternManager.removePattern(oldPattern);
		}*/
		parser.parse();
		//pattern.updateDefinitions(patternManager);
		/*for(LTLPatternMarker marker : listener.getErrorMarkers()) {
			System.out.println(marker.getMsg());
		}*/
		System.out.println(patternManager.getPatterns().size());
		System.out.println(listener.getErrorMarkers().size());
		System.out.println(listener.getWarningMarkers().size());
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
