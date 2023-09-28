package de.prob2.ui.verifications.temporal.ltl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.prob.animator.domainobjects.ErrorItem;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LTLParseListener extends BaseErrorListener {

	private final Logger LOGGER = LoggerFactory.getLogger(LTLParseListener.class);

	private List<ErrorItem> errorMarkers = new LinkedList<>();

	private static ErrorItem.Location locationFromTokenPos(final int line, final int charPositionInLine, final int length) {
		return new ErrorItem.Location("", line, charPositionInLine, line, charPositionInLine + length);
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
							String msg, RecognitionException e) {
		int length = 1;
		if (offendingSymbol instanceof Token token) {
			length = token.getStopIndex() - token.getStartIndex() + 1;
		}
		errorMarkers.add(new ErrorItem(msg, ErrorItem.Type.ERROR, Collections.singletonList(locationFromTokenPos(line, charPositionInLine, length))));
		LOGGER.trace("Parse error {}", offendingSymbol);
	}

	public List<ErrorItem> getErrorMarkers() {
		return errorMarkers;
	}
}
