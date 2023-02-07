package de.prob2.ui.documentation;

import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;

import java.nio.file.Path;

public class DocumentationUtility {

	public static String getAbsoluteHtmlPath(Path directory, Machine machine, ReplayTrace trace) {
		return directory+"/" + getHtmlPath(machine,trace);
	}

	public static String getHtmlPath(Machine machine, ReplayTrace trace) {
		return "html_files/" + machine.getName() + "/";
	}

	public static String toUIString(ModelCheckingItem item, I18n i18n) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description.replaceAll(",", ",\\\\newline");
	}


	public static String latexSafe(String text) {
		return text.replace("_", "\\_");
	}

}
