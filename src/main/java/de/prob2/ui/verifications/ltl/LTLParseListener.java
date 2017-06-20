package de.prob2.ui.verifications.ltl;

import de.prob.ltl.parser.WarningListener;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.pattern.PatternUpdateListener;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;


import org.slf4j.LoggerFactory;

public class LTLParseListener extends BaseErrorListener implements WarningListener, PatternUpdateListener {

	private final Logger logger = LoggerFactory.getLogger(LTLParseListener.class);

	private List<LTLMarker> warningMarkers = new LinkedList<>();
	private List<LTLMarker> errorMarkers = new LinkedList<>();

	@Override
	public void warning(Token token, String msg) {
		int length = token.getStopIndex() - token.getStartIndex() + 1;
		warningMarkers.add(new LTLMarker("warning", token.getLine(), token.getCharPositionInLine(), length, msg));
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
							String msg, RecognitionException e) {
		int length = 1;
		if (offendingSymbol != null && offendingSymbol instanceof Token) {
			Token token = (Token) offendingSymbol;
			length = token.getStopIndex() - token.getStartIndex() + 1;
		}
		errorMarkers.add(new LTLMarker("error", line, charPositionInLine, length, msg));
		logger.trace("Parse error {}", offendingSymbol);
	}

	public List<LTLMarker> getWarningMarkers() {
		return warningMarkers;
	}

	public List<LTLMarker> getErrorMarkers() {
		return errorMarkers;
	}

	@Override
	public void patternUpdated(Pattern pattern, PatternManager patternManager) {
		logger.trace("Pattern updated {}", pattern.getName());
	}
	
}
