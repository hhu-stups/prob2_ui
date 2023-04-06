package de.prob2.ui.documentation;

import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.verifications.temporal.TemporalFormulaItem;
import de.prob2.ui.verifications.temporal.ltl.patterns.LTLPatternItem;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.prob2.ui.documentation.DocumentationProcessHandler.readFile;
import static de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingType.DEADLOCK;

// All Methods of this class are exclusively used by the documentation Template
public class TemplateUtility {

	public static String transitionToUIString(PersistentTransition transition) {
		String operationName = Transition.prettifyName(transition.getOperationName());
		Map<String, String> parameters = transition.getParameters();
		if(parameters.isEmpty()) {
			return latexSafe(operationName);
		}
		List<String> keyValuePairs = parameters.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.toList());
		return latexSafe(operationName + "(" + String.join(", ", keyValuePairs) + ")");
	}

	public static String symbolicConfigString(SymbolicCheckingFormulaItem formula, I18n i18n) {
		String config = formula.getCode();
		// no configuration is internally represented as 1=1, but that is not intended in the output
		if(formula.getType().equals(DEADLOCK) && config.equals("1=1")){
			return latexSafe(i18n.translate("verifications.symbolicchecking.view.deadlock.noPredicate"));
		}
		// & is replaced because otherwise a column change takes place in the template table
		config = config.replaceAll("&",", \\\\newline");
		return latexSafe(config);
	}
	public static String modelcheckingToUIString(ModelCheckingItem item, I18n i18n) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		//replaces comma between 2 digits. statelimit parameter(integer) breaks the line otherwise
		description = description.replaceAll("(\\d),(\\d)", "$1$2");
		return description.replaceAll(",", ",\\\\newline");
	}
	public static String getMachineCode(Machine elem, CurrentProject project) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}
	public static String getTraceHtmlCode(String relativePath, ProjectDocumenter projectDocumenter){
		return readFile(Paths.get(projectDocumenter.getDirectory() +"/"+ relativePath));
	}
	public static boolean formulaHasResult(TemporalFormulaItem formula){return (formula.getResultItem() != null);}
	public static boolean patternHasResult(LTLPatternItem pattern){return (pattern.getResultItem() != null);}
	public static boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}

	public static int getNumberSelectedTasks(List<? extends IExecutableItem> validationTasks){
		long selectedTasksCount = validationTasks.stream()
				.filter(IExecutableItem::selected)
				.count();
		return Math.toIntExact(selectedTasksCount);
	}
	public static int getNumberSuccessfulTasks(List<? extends IExecutableItem> validationTasks){
		long countSuccessful = validationTasks.stream()
				.filter(task -> task.selected() && task.getChecked().equals(Checked.SUCCESS))
				.count();
		return Math.toIntExact(countSuccessful);
	}
	public static int getNumberNotCheckedTasks(List<? extends IExecutableItem> validationTasks){
		long countNotChecked = validationTasks.stream()
				.filter(task -> task.selected() && task.getChecked().equals(Checked.NOT_CHECKED))
				.count();
		return Math.toIntExact(countNotChecked);
	}
	public static int getNumberFailedTasks(List<? extends IExecutableItem> validationTasks){
		long countFailed= validationTasks.stream()
				.filter(task -> task.selected() && (!task.getChecked().equals(Checked.NOT_CHECKED) && !task.getChecked().equals(Checked.SUCCESS)))
				.count();
		return Math.toIntExact(countFailed);
	}

	public static boolean ltlDescriptionColumnNecessary(List<TemporalFormulaItem> ltlFormulas){
		for (TemporalFormulaItem formula : ltlFormulas) {
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
