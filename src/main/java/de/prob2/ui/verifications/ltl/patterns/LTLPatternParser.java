package de.prob2.ui.verifications.ltl.patterns;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.semantic.PatternDefinition;
import de.prob2.ui.verifications.ltl.LTLResultHandler;
import de.prob2.ui.verifications.ltl.LTLView;

public class LTLPatternParser {
	
	private static final Logger logger = LoggerFactory.getLogger(LTLPatternParser.class);
	
	private final LTLResultHandler resultHandler;
	
	private final Injector injector;
	
	@Inject
	private LTLPatternParser(final LTLResultHandler resultHandler, final Injector injector) {
		this.resultHandler = resultHandler;
		this.injector = injector;
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
			resultHandler.handlePatternResult(parseListener, item);
			injector.getInstance(LTLView.class).refreshPattern();
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
