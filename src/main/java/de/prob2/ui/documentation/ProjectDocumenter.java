package de.prob2.ui.documentation;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.visb.VisBStage;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class ProjectDocumenter {
	private final String filename;
	private final I18n i18n;
	private final Path directory;
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
		this.directory = dir;
		this.filename = filename;
		this.injector = injector;
		tracesHtmlPaths = new HashMap<>();
		DocumentationResourceBuilder.buildLatexResources(dir,machines);
	}

	public void documentVelocity() throws IOException {
		initVelocityEngine();
		VelocityContext context = getVelocityContext();
		String templateName = getLanguageTemplate();
		try (final Writer writer = Files.newBufferedWriter(directory.resolve(filename + ".tex"))) {
			Velocity.mergeTemplate(templateName, String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
		if(makePdf)
			DocumentationProcessHandler.createPdf(filename, directory);
		DocumentationProcessHandler.createPortableDocumentationScript(filename, directory);
	}

	//only proof of concept for bachelor thesis. can be deleted later
	public void documentModelcheckingTableMarkdown() throws IOException {
		initVelocityEngine();
		VelocityContext context = new VelocityContext();
		context.put("machines", machines);
		context.put("util", TemplateUtility.class);
		context.put("i18n", i18n);
		try (final Writer writer = Files.newBufferedWriter(directory.resolve(filename + ".md"))) {
			Velocity.mergeTemplate("de/prob2/ui/documentation/modelchecking_table.md.vm", String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	// future translations can be added here
	private String getLanguageTemplate() {
		String language = injector.getInstance(Locale.class).getLanguage();
		switch (language){
			case "de":
				return "de/prob2/ui/documentation/velocity_template_german.tex.vm";
			default:
				return "de/prob2/ui/documentation/velocity_template_english.tex.vm";
		}
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
		context.put("util", TemplateUtility.class);
		context.put("Transition", Transition.class);
		context.put("i18n", i18n);
		context.put("traceHtmlPaths",tracesHtmlPaths);
		return context;
	}

	public String saveTraceHtml(Machine machine, ReplayTrace trace){
		VisBStage stage = injector.getInstance(VisBStage.class);
		String filename = Transition.prettifyName(trace.getName())+".html";
		/* startAnimation works with completable futures. Project access before its finished Loading, can create null Exceptions.
		* To solve this Problem the completable future is saved as an field that can be accessed to be synced  */
		project.startAnimation(machine, project.get().getPreference(machine.getLastUsedPreferenceName()));
		project.getLoadFuture().join();
		final StateSpace stateSpace = injector.getInstance(CurrentTrace.class).getStateSpace();
		TraceChecker.checkNoninteractive(trace, stateSpace);
		stage.show();
		stage.saveHTMLExportWithPath(trace.getReplayedTrace().getTrace(stateSpace), VisBStage.VisBExportKind.CURRENT_TRACE, Paths.get(getAbsoluteHtmlPath(directory,machine,trace)+filename));
		stage.close();
		return getHtmlPath(machine,trace)+filename;
	}

	public Path getDirectory() {
		return directory;
	}

	public static String getAbsoluteHtmlPath(Path directory, Machine machine, ReplayTrace trace) {
		return directory+"/" + getHtmlPath(machine,trace);
	}

	public static String getHtmlPath(Machine machine, ReplayTrace trace) {
		return "html_files/" + machine.getName() + "/";
	}

}
