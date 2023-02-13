package de.prob2.ui.documentation;

import com.google.inject.Inject;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.prob2.ui.documentation.Converter.*;

@FXMLInjected
public class Documenter {
	private final String filename;
	private final I18n i18n;
	private final Path dir;
	private final boolean modelchecking;
	private final boolean ltl;
	private final boolean symbolic;
	private final boolean makePdf;
	private final List<Machine> machines;
	StringBuilder latexBody = new StringBuilder();
	CurrentProject project;
	@Inject
	public Documenter(CurrentProject project,
					  I18n i18n, boolean modelchecking,
					  boolean ltl, boolean symbolic,boolean makePdf,
					  List<Machine> machines,
					  Path dir,
					  String filename) throws IOException {
		this.project = project;
		this.i18n = i18n;
		this.modelchecking = modelchecking;
		this.ltl = ltl;
		this.symbolic = symbolic;
		this.makePdf = makePdf;
		this.machines = machines;
		this.dir = dir;
		this.filename = filename;
		latexBody.append(readResource(this, "latexBody.tex"));
	}

	private String documentMachines() throws IOException {
		StringBuilder machineString = new StringBuilder();
		for (Machine elem : machines) {
			machineString.append(machineTexTemplate(elem));
		}
		return machineString.toString();
	}

	private String machineTexTemplate(Machine elem) throws IOException {
		String machineTemplate = readResource(this, "machine.tex");
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("name", latexSafe(elem.getName()));
		valuesMap.put("code", getMachineCode(elem));
		valuesMap.put("modelchecking", modelchecking ? getModelcheckingString(elem) : "");
		valuesMap.put("ltl", ltl ? getLTLString(elem) : "");
		valuesMap.put("symbolic", symbolic ? getSymbolicString(elem) : "");
		valuesMap.put("traces", getTracesString(elem));
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		return sub.replace(machineTemplate);
	}

	String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}

	private String getModelcheckingString(Machine elem) throws IOException {
		String modelcheckingTemplate = readResource(this, "modelchecking.tex");
		Map<String, String> valuesMap = new HashMap<String, String>();
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		StringBuilder modelcheckingString = new StringBuilder();
		for (ModelCheckingItem item : elem.getModelcheckingItems()) {
			StringBuilder resultString = new StringBuilder();
			if (item.getSteps().isEmpty()){
				resultString.append("Modelchecking not solved");
			}
			else {
				item.getSteps().forEach(result ->  resultString.append(result.getMessage()));
			}
			valuesMap.put("modname", toUIString(item));
			valuesMap.put("modresult",String.valueOf(resultString));
			modelcheckingString.append(sub.replace(readResource(this, "modelcheckingCell.tex")));
		}
		valuesMap.put("items", String.valueOf(modelcheckingString));
		return sub.replace(modelcheckingTemplate);
	}


	private String getLTLString(Machine elem) throws IOException {
		String ltlTemplate = readResource(this, "ltl.tex");
		Map<String, String> valuesMap = new HashMap<String, String>();
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		StringBuilder ltlFormulars = new StringBuilder();
		StringBuilder ltlPatterns = new StringBuilder();
		for (LTLFormulaItem formular : elem.getLTLFormulas()) {
			if (formular.selected()) {
				valuesMap.put("fcode", formular.getCode());
				valuesMap.put("fresult", (formular.getResultItem() != null) ? i18n.translate(formular.getResultItem().getHeaderBundleKey()) : "Formular not Solved");
				ltlFormulars.append(sub.replace(readResource(this, "formularCell.tex")));
			}
		}
		for (LTLPatternItem pattern : elem.getLTLPatterns()) { //TODO FIX TABLE ALLIGNMENT
			if (pattern.getResultItem() != null) {
				valuesMap.put("pname", pattern.getName());
				valuesMap.put("pcode", pattern.getCode());
				valuesMap.put("presult", i18n.translate(pattern.getResultItem().getHeaderBundleKey())); //CHANGE LANGUANGE
				ltlPatterns.append(sub.replace(readResource(this, "patternCell.tex")));
			}
		}
		valuesMap.put("formulars", String.valueOf(ltlFormulars));
		valuesMap.put("patterns", String.valueOf(ltlPatterns));
		return sub.replace(ltlTemplate);
	}

	private String getSymbolicString(Machine elem) {
		//SYMBOLIC STRING
		return "";
	}

	private String getTracesString(Machine elem) throws IOException {
		String traceTemplate = readResource(this, "traces.tex");
		StringBuilder table = new StringBuilder();
		Map<String, String> valuesMap = new HashMap<String, String>();
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		if (elem.getTraces().isEmpty()) {
			return "";
		}
		for (ReplayTrace trace : elem.getTraces()) {
			StringBuilder cellString = new StringBuilder();
			trace.load();
			int i = 0;
			for (PersistentTransition transition : trace.getLoadedTrace().getTransitionList()) {
				valuesMap.put("pos", String.valueOf(i));
				valuesMap.put("transition", latexSafe(Transition.prettifyName(transition.getOperationName())));
				cellString.append(sub.replace(readResource(this, "traceCell.tex")));
				i++;
			}
			valuesMap.put("name", latexSafe(trace.getName()));
			valuesMap.put("item", String.valueOf(cellString));
			table.append(sub.replace(readResource(this, "traceItemTable.tex")));
		}
		valuesMap.put("traceitemtables", String.valueOf(table));
		return sub.replace(traceTemplate);
	}

	public void document() throws IOException{
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("machines", documentMachines());
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		latexBody = new StringBuilder(sub.replace(latexBody.toString()));
		Converter.stringToTex(latexBody.toString(), filename, dir);
		if(makePdf)
			createPdf();
	}

	private void createPdf() {
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File(dir.toString()));
		builder.command("bash", "-c", "pdflatex -interaction=nonstopmode " + filename + ".tex");
		try {
			builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String toUIString(ModelCheckingItem item) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}
}
