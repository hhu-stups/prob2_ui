package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.beditor.BLexerSyntaxHighlighting;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;

@Singleton
@FXMLInjected
public final class ProofSequentCodeArea extends ExtendedCodeArea {

	@Inject
	private ProofSequentCodeArea(final FontSize fontSize, final I18n i18n, final StopActions stopActions) {
		super(fontSize, i18n, stopActions);
	}

	@Override
	protected boolean showLineNumbers() {
		return true;
	}

	@Override
	protected StyleSpans<Collection<String>> computeHighlighting(String text) {
		StyleSpans<Collection<String>> styleSpans = super.computeHighlighting(text);
		if (styleSpans == null || text.isEmpty()) {
			return styleSpans;
		}
		return styleSpans.overlay(computeSequentHighlighting(text), ExtendedCodeArea::combineStyleSpans);
	}

	public static StyleSpans<Collection<String>> computeSequentHighlighting(String text) {
		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		for (String line : text.split("\n", -1)) {
			String replaceLine = line.replace("═"," ").replace("✔"," ").replace("─"," ");
			if (!replaceLine.isEmpty()) {
				builder.addAll(BLexerSyntaxHighlighting.computeBFormulaHighlighting(replaceLine));
			}
			builder.add(Collections.emptyList(), 1);
		}
		return builder.create();
	}
}
