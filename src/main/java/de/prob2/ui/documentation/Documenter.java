package de.prob2.ui.documentation;

import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.visb.VisBFileHandler;
import de.prob2.ui.visb.VisBStage;
import de.prob2.ui.visb.visbobjects.VisBVisualisation;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.prob2.ui.documentation.DocumentUtility.*;

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
	private final HashMap<String, String> valuesMap;
	private final StringSubstitutor sub;
	StringBuilder latexBody = new StringBuilder();
	CurrentProject project;
	Injector injector;
	@Inject
	public Documenter(CurrentProject project,
					  I18n i18n, boolean modelchecking,
					  boolean ltl, boolean symbolic,boolean makePdf,
					  List<Machine> machines,
					  Path dir,
					  String filename,
					  Injector injector) throws IOException {
		this.project = project;
		this.i18n = i18n;
		this.modelchecking = modelchecking;
		this.ltl = ltl;
		this.symbolic = symbolic;
		this.makePdf = makePdf;
		this.machines = machines;
		this.dir = dir;
		this.filename = filename;
		this.injector = injector;
		latexBody.append(readResource(this, "latexBody.tex"));
		valuesMap = new HashMap<String, String>();
		sub = new StringSubstitutor(valuesMap);
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
		valuesMap.put("machineName", latexSafe(elem.getName()));
		valuesMap.put("machineCode", getMachineCode(elem));
		valuesMap.put("modelchecking", modelchecking && !elem.getModelcheckingItems().isEmpty() ? getModelcheckingString(elem) : "");
		valuesMap.put("ltl", ltl ? getLTLString(elem) : "");
		valuesMap.put("symbolic", symbolic && !elem.getSymbolicCheckingFormulas().isEmpty() ? getSymbolicString(elem) : "");
		valuesMap.put("traces", getTracesString(elem));
		return sub.replace(machineTemplate);
	}

	public String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}

	public String getModelcheckingString(Machine elem) throws IOException {
		String modelcheckingTemplate = readResource(this, "modelcheckingTable.tex");
		StringBuilder modelcheckingString = new StringBuilder();
		for (ModelCheckingItem item : elem.getModelcheckingItems()) {
			StringBuilder resultString = new StringBuilder();
			if (item.getItems().isEmpty()) {
				resultString.append("Modelchecking not solved");
			} else {
				item.getItems().forEach(result -> resultString.append(result.getMessage()));
			}
			valuesMap.put("modelcheckingItemName", toUIString(item, i18n));
			valuesMap.put("modelcheckingItemResult", String.valueOf(resultString));
			modelcheckingString.append(sub.replace(readResource(this, "modelcheckingCell.tex")));
		}
		valuesMap.put("modelcheckingItems", String.valueOf(modelcheckingString));
		return sub.replace(modelcheckingTemplate);
	}


	private String getLTLString(Machine elem) throws IOException {
		String ltlTemplate = readResource(this, "ltlTable.tex");
		StringBuilder ltlFormulars = new StringBuilder();
		StringBuilder ltlPatterns = new StringBuilder();

		for (LTLFormulaItem formula : elem.getLTLFormulas()) {
			if (formula.selected()) {
				valuesMap.put("formulaCode", formula.getCode());
				valuesMap.put("formulaResult", (formula.getResultItem() != null) ? i18n.translate(formula.getResultItem().getHeaderBundleKey()) : "Formular not Solved");
				ltlFormulars.append(sub.replace(readResource(this, "formularCell.tex")));
			}
		}
		for (LTLPatternItem pattern : elem.getLTLPatterns()) { //TODO FIX TABLE ALLIGNMENT
			if (pattern.selected() && pattern.getResultItem() != null) {
				valuesMap.put("patternName", pattern.getName());
				valuesMap.put("patternCode", pattern.getCode());
				valuesMap.put("patternResult", i18n.translate(pattern.getResultItem().getHeaderBundleKey())); //CHANGE LANGUANGE
				ltlPatterns.append(sub.replace(readResource(this, "patternCell.tex")));
			}
		}
		valuesMap.put("ltlFormulars", String.valueOf(ltlFormulars));
		valuesMap.put("ltlPatterns", String.valueOf(ltlPatterns));
		return sub.replace(ltlTemplate);
	}

	private String getSymbolicString(Machine elem) throws IOException {
		String symbolicTemplate = readResource(this, "symbolicTable.tex");
		StringBuilder symbolicFormulas = new StringBuilder();
		for(SymbolicCheckingFormulaItem formula : elem.getSymbolicCheckingFormulas()){
			if(formula.selected()) {
				valuesMap.put("symbolicItemType", latexSafe(String.valueOf(formula.getType())));
				valuesMap.put("symbolicItemConfig", latexSafe(formula.getCode()));
				valuesMap.put("symbolicItemResult", (formula.getResultItem() != null) ? i18n.translate(formula.getResultItem().getHeaderBundleKey()) : "Formular not Solved");
				symbolicFormulas.append(sub.replace(readResource(this, "symbolicCell.tex")));
			}
		}
		valuesMap.put("symbolicFormulars", String.valueOf(symbolicFormulas));
		return sub.replace(symbolicTemplate);
	}

	private String getTracesString(Machine elem) throws IOException {
		String traceTemplate = readResource(this, "traces.tex");
		StringBuilder table = new StringBuilder();
		if (elem.getTraces().isEmpty()) {
			return "";
		}
		for (ReplayTrace trace : elem.getTraces()) {
			StringBuilder cellString = new StringBuilder();
			trace.load();
			int i = 0;
			for (PersistentTransition transition : trace.getLoadedTrace().getTransitionList()) {
				valuesMap.put("position", String.valueOf(i));
				valuesMap.put("transition", latexSafe(Transition.prettifyName(transition.getOperationName())));
				cellString.append(sub.replace(readResource(this, "traceCell.tex")));
				i++;
			}
			valuesMap.put("traceName", latexSafe(trace.getName()));
			valuesMap.put("traceItems", String.valueOf(cellString));
			table.append(sub.replace(readResource(this, "traceItemTable.tex")));
			//saveTraceImage(elem, trace);
		}
		valuesMap.put("traceItemTables", String.valueOf(table));
		return sub.replace(traceTemplate);
	}

	public void document() throws IOException{
		Files.createDirectories(Paths.get(dir+"/resources"));
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("machines", documentMachines());
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		latexBody = new StringBuilder(sub.replace(latexBody.toString()));
		DocumentUtility.stringToTex(latexBody.toString(), filename, dir);
		if(makePdf)
			createPdf(filename, dir);
	}

}
