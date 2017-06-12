package de.prob2.ui.verifications.ltl.patterns;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.semantic.PatternDefinition;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLResultHandler.Checked;
import javafx.scene.control.Alert.AlertType;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
	
	private final LTLResultHandler resultHandler;
	
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler) {
		this.resultHandler = resultHandler;
	}
		
	public void parsePattern(LTLPatternItem item, PatternManager patternManager) {
		logger.trace("Parse ltl pattern");
		/*LtlParser parser = new LtlParser(item.getCode());
		LTLParseListener parseListener = new LTLParseListener();
		parser.setPatternManager(patternManager);		
		parser.removeErrorListeners();
		parser.removeWarningListeners();
		parser.addErrorListener(parseListener);
		parser.addWarningListener(parseListener);
		patternManager.removeUpdateListeners();
		patternManager.addUpdateListener(parseListener);
		parser.parse();
		System.out.println(patternManager.getPatterns().size());*/
		LTLResultHandler.LTLResultItem resultItem = null;
		if(!patternManager.patternExists(item.getName())) {
			Pattern pattern = new Pattern();
			pattern.setBuiltin(false);
			pattern.setName(item.getName());
			pattern.setDescription(item.getDescription());
			pattern.setCode(item.getCode());
			patternManager.getPatterns().add(pattern);
			LTLParseListener parseListener = new LTLParseListener();
			pattern.removeErrorListeners();
			pattern.removeWarningListeners();
			pattern.removeUpdateListeners();
			pattern.addErrorListener(parseListener);
			pattern.addWarningListener(parseListener);
			pattern.addUpdateListener(parseListener);
			pattern.updateDefinitions(patternManager);
			for (LTLPatternMarker marker: parseListener.getErrorMarkers()) {
				resultItem = new LTLResultHandler.LTLResultItem(AlertType.INFORMATION, Checked.SUCCESS, "LTL Check succeeded", "Success");
			}
		}
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
