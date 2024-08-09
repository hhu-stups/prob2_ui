package de.prob2.ui.beditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.*;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;
import java.util.Set;

@Singleton
@FXMLInjected
public final class InternalRepresentationCodeArea extends ExtendedCodeArea {
	private final CurrentProject currentProject;

	@Inject
	private InternalRepresentationCodeArea(final FontSize fontSize, final I18n i18n, final StopActions stopActions,
	                                       final CurrentProject currentProject) {
		super(fontSize, i18n, stopActions);
		this.currentProject = currentProject;
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

		StyleSpans<Collection<String>> highlighting;
		if (currentProject.getCurrentMachine() == null) {
			// Prompt text is a comment
			highlighting = StyleSpans.singleton(Set.of("editor_comment"), text.length());
		} else {
			highlighting = BLexerSyntaxHighlighting.computeBHighlighting(text);
		}

		return styleSpans.overlay(highlighting, ExtendedCodeArea::combineStyleSpans);
	}
}
