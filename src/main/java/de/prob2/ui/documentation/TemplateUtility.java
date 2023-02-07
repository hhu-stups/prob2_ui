package de.prob2.ui.documentation;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.vomanager.IValidationTask;

import java.nio.file.Paths;
import java.util.List;

import static de.prob2.ui.documentation.DocumentationProcessHandler.readFile;

// All Methods of this class are exclusively used by the documentation Template
public class TemplateUtility {

	public static String toUIString(ModelCheckingItem item, I18n i18n) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description.replaceAll(",", ",\\\\newline");
	}
	public static String getMachineCode(Machine elem, CurrentProject project) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}
	public static String getTraceHtmlCode(String relativePath, ProjectDocumenter projectDocumenter){
		return readFile(Paths.get(projectDocumenter.getDirectory() +"/"+ relativePath));
	}
	public static boolean formulaHasResult(LTLFormulaItem formula){return (formula.getResultItem() != null);}
	public static boolean patternHasResult(LTLPatternItem pattern){return (pattern.getResultItem() != null);}
	public static boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}

	public static int getNumberSelectedTasks(List<IValidationTask> validationTasks){
		long selectedTasksCount = validationTasks.stream()
				.filter(IExecutableItem::selected)
				.count();
		return Math.toIntExact(selectedTasksCount);
	}
	public static int getNumberSuccessfulTasks(List<IValidationTask> validationTasks){
		long countSuccessful = validationTasks.stream()
				.filter(task -> task.selected() && task.getChecked().equals(Checked.SUCCESS))
				.count();
		return Math.toIntExact(countSuccessful);
	}
	public static int getNumberNotCheckedTasks(List<IValidationTask> validationTasks){
		long countNotChecked = validationTasks.stream()
				.filter(task -> task.selected() && task.getChecked().equals(Checked.NOT_CHECKED))
				.count();
		return Math.toIntExact(countNotChecked);
	}
	public static int getNumberFailedTasks(List<IValidationTask> validationTasks){
		long countFailed= validationTasks.stream()
				.filter(task -> task.selected() && (!task.getChecked().equals(Checked.NOT_CHECKED) && !task.getChecked().equals(Checked.SUCCESS)))
				.count();
		return Math.toIntExact(countFailed);
	}

	public static boolean ltlDescriptionColumnNecessary(List<LTLFormulaItem> ltlFormulas){
		for (LTLFormulaItem formula : ltlFormulas) {
			if (!formula.getDescription().isEmpty()) {
				return true;
			}
		}
		return false;
	}
	public static boolean symbolicConfigurationColumnNecessary(List<SymbolicCheckingFormulaItem> symbolicFormulas){
		for (SymbolicCheckingFormulaItem formula : symbolicFormulas) {
			if (!formula.getCode().isEmpty()) {
				return true;
			}
		}
		return false;
	}
	public static String latexSafe(String text) {
		return text.replace("_", "\\_");
	}

}
