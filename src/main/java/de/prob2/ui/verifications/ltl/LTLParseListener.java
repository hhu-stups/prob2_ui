package de.prob2.ui.verifications.ltl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;
import de.prob.ltl.parser.WarningListener;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.ltl.parser.pattern.PatternUpdateListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class LTLParseListener extends BaseErrorListener implements WarningListener, PatternUpdateListener {

	private final Logger logger = LoggerFactory.getLogger(LTLParseListener.class);

	private List<ErrorItem> warningMarkers = new LinkedList<>();
	private List<ErrorItem> errorMarkers = new LinkedList<>();

	private static ErrorItem.Location locationFromTokenPos(final int line, final int charPositionInLine, final int length) {
		return new ErrorItem.Location("", line, charPositionInLine, line, charPositionInLine + length);
	}

	@Override
	public void warning(Token token, String msg) {
		int length = token.getStopIndex() - token.getStartIndex() + 1;
		warningMarkers.add(new ErrorItem(msg, ErrorItem.Type.WARNING, Collections.singletonList(locationFromTokenPos(token.getLine(), token.getCharPositionInLine(), length))));
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
							String msg, RecognitionException e) {
		int length = 1;
		if (offendingSymbol instanceof Token) {
			Token token = (Token) offendingSymbol;
			length = token.getStopIndex() - token.getStartIndex() + 1;
		}
		errorMarkers.add(new ErrorItem(msg, ErrorItem.Type.ERROR, Collections.singletonList(locationFromTokenPos(line, charPositionInLine, length))));
		logger.trace("Parse error {}", offendingSymbol);
	}

	public List<ErrorItem> getWarningMarkers() {
		return warningMarkers;
	}

	public List<ErrorItem> getErrorMarkers() {
		return errorMarkers;
	}

	@Override
	public void patternUpdated(Pattern pattern, PatternManager patternManager) {
		logger.trace("Pattern updated {}", pattern.getName());
	}
	
}
