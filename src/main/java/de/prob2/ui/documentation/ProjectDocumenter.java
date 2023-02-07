package de.prob2.ui.documentation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.visb.VisBStage;
import de.prob2.ui.vomanager.IValidationTask;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.prob2.ui.documentation.DocumentationProcessHandler.*;
import static de.prob2.ui.documentation.DocumentationResourceBuilder.*;
import static de.prob2.ui.documentation.DocumentationUtility.getAbsoluteHtmlPath;
import static de.prob2.ui.documentation.DocumentationUtility.getHtmlPath;

public class ProjectDocumenter {
	private final String filename;
	private final I18n i18n;
	private final Path dir;
	private final boolean modelchecking;
	private final boolean ltl;
	private final boolean symbolic;
	private final boolean makePdf;
	private final boolean printHtmlCode;
	private final List<Machine> machines;
	private final CurrentProject project;
	private final Injector injector;
	private HashMap<String,String> tracesHtmlPaths;

	@Inject
	public ProjectDocumenter(CurrentProject project,
							 I18n i18n, boolean modelchecking,
							 boolean ltl, boolean symbolic, boolean makePdf, boolean printHtmlCode,
							 List<Machine> machines,
							 Path dir,
							 String filename,
							 Injector injector){
		this.project = project;
		this.i18n = i18n;
		this.modelchecking = modelchecking;
		this.ltl = ltl;
		this.symbolic = symbolic;
		this.makePdf = makePdf;
		this.printHtmlCode = printHtmlCode;
		this.machines = machines;
		this.dir = dir;
		this.filename = filename;
		this.injector = injector;
		tracesHtmlPaths = new HashMap<>();
		buildLatexResources(dir,machines);
	}

	public void documentVelocity() throws TemplateInitException, ResourceNotFoundException, MethodInvocationException, ParseErrorException {
		initVelocityEngine();
		VelocityContext context = getVelocityContext();
		StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("de/prob2/ui/documentation/velocity_template.tex", String.valueOf(StandardCharsets.UTF_8),context,writer);
		DocumentationProcessHandler.saveStringWithExtension(writer.toString(), filename, dir, ".tex");
		if(makePdf)
			createPdf(filename,dir);
		saveMakeZipBash();
	}

	private static void initVelocityEngine() {
		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
	}

	private VelocityContext getVelocityContext() {
		VelocityContext context = new VelocityContext();
		context.put("project",project);
		context.put("documenter",this);
		context.put("machines", machines);
		context.put("modelchecking", modelchecking);
		context.put("ltl", ltl);
		context.put("symbolic", symbolic);
		context.put("printHtmlCode",printHtmlCode);
		context.put("documentationUtility", DocumentationUtility.class);
		context.put("Transition", Transition.class);
		context.put("i18n", i18n);
		context.put("traceHtmlPaths",tracesHtmlPaths);
		return context;
	}

	private void saveMakeZipBash() {
		switch (DocumentationProcessHandler.getOS()){
			case LINUX:
				createPortableDocumentationScriptLinux(filename,dir);
				break;
			case MAC:
				createPortableDocumentationScriptMac(filename,dir);
				break;
			case WINDOWS:
				createPortableDocumentationScriptWindows(filename,dir);
				break;
		}
	}
	public String saveTraceHtml(Machine machine, ReplayTrace trace){
		VisBStage stage = injector.getInstance(VisBStage.class);
		TraceChecker traceChecker = injector.getInstance(TraceChecker.class);
		String filename = Transition.prettifyName(trace.getName())+".html";
		/* startAnimation works with completable futures. Project access before its finished Loading, can create null Exceptions.
		* To solve this Problem the completable future is saved as an field that can be accessed to be synced  */
		project.startAnimation(machine, project.get().getPreference(machine.getLastUsedPreferenceName()));
		project.getLoadFuture().join();
		traceChecker.check(trace,true).join();
		stage.show();
		stage.saveHTMLExportWithPath(VisBStage.VisBExportKind.CURRENT_TRACE, Paths.get(getAbsoluteHtmlPath(dir,machine,trace)+filename));
		stage.close();
		return getHtmlPath(machine,trace)+filename;
	}

	/*--- exclusive used by Template ---*/
	public String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}
	public String getTraceHtmlCode(String relativePath){
		return readFile(Paths.get(dir +"/"+ relativePath));
	}
	public boolean formulaHasResult(LTLFormulaItem formula){return (formula.getResultItem() != null);}
	public boolean patternHasResult(LTLPatternItem pattern){return (pattern.getResultItem() != null);}
	public boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}

	public int getNumberSelectedTasks(List<IValidationTask> validationTasks){
		long selectedTasksCount = validationTasks.stream()
				.filter(IExecutableItem::selected)
				.count();
		return Math.toIntExact(selectedTasksCount);
	}
	public int getNumberSuccessfulTasks(List<IValidationTask> validationTasks){
		long countSuccessful = validationTasks.stream()
											  .filter(task -> task.selected() && task.getChecked().equals(Checked.SUCCESS))
										      .count();
		return Math.toIntExact(countSuccessful);
	}
	public int getNumberNotCheckedTasks(List<IValidationTask> validationTasks){
		long countNotChecked = validationTasks.stream()
				.filter(task -> task.selected() && task.getChecked().equals(Checked.NOT_CHECKED))
				.count();
		return Math.toIntExact(countNotChecked);
	}
	public int getNumberFailedTasks(List<IValidationTask> validationTasks){
		long countFailed= validationTasks.stream()
				.filter(task -> task.selected() && (!task.getChecked().equals(Checked.NOT_CHECKED) && !task.getChecked().equals(Checked.SUCCESS)))
				.count();
		return Math.toIntExact(countFailed);
	}

	public boolean ltlDescriptionColumnNecessary(List<LTLFormulaItem> ltlFormulas){
		for (LTLFormulaItem formula : ltlFormulas) {
			if (!formula.getDescription().isEmpty()) {
				return true;
			}
		}
		return false;
	}
	public boolean symbolicConfigurationColumnNecessary(List<SymbolicCheckingFormulaItem> symbolicFormulas){
		for (SymbolicCheckingFormulaItem formula : symbolicFormulas) {
			if (!formula.getCode().isEmpty()) {
				return true;
			}
		}
		return false;
	}
	/*---------------------------------*/
}
