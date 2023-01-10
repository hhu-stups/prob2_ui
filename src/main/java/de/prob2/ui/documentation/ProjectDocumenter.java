package de.prob2.ui.documentation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.statespace.Transition;
import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.visb.VisBStage;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.prob2.ui.documentation.DocumentUtility.*;

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
		buildLatexResources();
		saveMakeZipBash();
	}

	public void documentVelocity() throws TemplateInitException, ResourceNotFoundException, MethodInvocationException, ParseErrorException {
		initVelocityEngine();
		VelocityContext context = getVelocityContext();
		StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("de/prob2/ui/documentation/velocity_template.tex", String.valueOf(StandardCharsets.UTF_8),context,writer);
		DocumentUtility.saveStringWithExtension(writer.toString(), filename, dir, ".tex");
		if(makePdf)
			createPdf(filename,dir);
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
		context.put("DocumentUtility", DocumentUtility.class);
		context.put("Transition", Transition.class);
		context.put("i18n", i18n);
		context.put("traceHtmlPaths",tracesHtmlPaths);
		return context;
	}
	public String saveTraceHtml(Machine machine, ReplayTrace trace){
		VisBStage stage = injector.getInstance(VisBStage.class);
		TraceChecker traceChecker = injector.getInstance(TraceChecker.class);
		String filename = Transition.prettifyName(trace.getName())+".html";
		project.startAnimation(machine, project.get().getPreference(machine.getLastUsedPreferenceName()));
		project.getLoadFuture().join();
		traceChecker.check(trace,true).join();
		stage.show();
		stage.saveHTMLExportWithPath(VisBStage.VisBExportKind.CURRENT_TRACE, Paths.get(getAbsoluteHtmlPath(machine,trace)+filename));
		stage.close();
		return getHtmlPath(machine,trace)+filename;
	}

	private String getAbsoluteHtmlPath(Machine machine, ReplayTrace trace) {
		return dir+"/" + getHtmlPath(machine,trace);
	}

	private String getHtmlPath(Machine machine, ReplayTrace trace) {
		return "html_files/" + machine.getName() + "/" + Transition.prettifyName(trace.getName()) +"/";
	}
	public String getTraceHtmlCode(String relativePath){
		return readFile(Paths.get(dir +"/"+ relativePath));
	}
	private void buildLatexResources() {
		createImageDirectoryStructure();
		saveProBLogo();
		saveLatexCls();
	}

	private void createImageDirectoryStructure() {
		for (Machine machine : machines) {
			for (ReplayTrace trace : machine.getTraces()) {
				try {
					Files.createDirectories(Paths.get(getAbsoluteHtmlPath(machine, trace))
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/*--- exclusive used by Template ---*/
	public String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}
	public boolean formulaHasResult(LTLFormulaItem formula){return (formula.getResultItem() != null);}
	public boolean patternHasResult(LTLPatternItem pattern){return (pattern.getResultItem() != null);}
	public boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}
	/*---------------------------------*/
}
