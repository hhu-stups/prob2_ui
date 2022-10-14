package de.prob2.ui.beditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.ModelFactory;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;
import java.util.Collections;

@Singleton
@FXMLInjected
public class BEditor extends ExtendedCodeArea {

	private final CurrentProject currentProject;

	@Inject
	private BEditor(final FontSize fontSize, final I18n i18n, final CurrentProject currentProject, final StopActions stopActions) {
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
		Machine machine = currentProject.getCurrentMachine();
		if (machine == null) {
			// Prompt text is a comment text
			return styleSpans.overlay(StyleSpans.singleton(Collections.singleton("editor_comment"), text.length()), ExtendedCodeArea::combineCollections);
		}
		Class<? extends ModelFactory<?>> modelFactoryClass = machine.getModelFactoryClass();
		if (modelFactoryClass == ClassicalBFactory.class) {
			return styleSpans.overlay(BLexerSyntaxHighlighting.computeBHighlighting(text), ExtendedCodeArea::combineCollections);
		} else if (RegexSyntaxHighlighting.canHighlight(modelFactoryClass)) {
			return styleSpans.overlay(RegexSyntaxHighlighting.computeHighlighting(modelFactoryClass, text), ExtendedCodeArea::combineCollections);
		} else {
			// Do not highlight unknown languages.
			return styleSpans;
		}
	}
}
