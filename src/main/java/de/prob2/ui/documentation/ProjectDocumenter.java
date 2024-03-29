package de.prob2.ui.documentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.ExportVisBForHistoryCommand;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;
import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

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
	private final HashMap<String,String> tracesHtmlPaths;

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
	}

	private static Process createPdf(String filename, Path directory) throws IOException {
		final ProcessBuilder builder = new ProcessBuilder("pdflatex", "--shell-escape", "-interaction=nonstopmode", filename + ".tex");
		builder.directory(directory.toFile());
		builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.redirectError(ProcessBuilder.Redirect.INHERIT);
		return builder.start();
	}

	private static void copyFile(Path path, InputStream resource) throws IOException {
		try (InputStream resourceAsStream = resource) {
			assert resourceAsStream != null;
			Files.copy(resourceAsStream, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void buildLatexResources(Path directory) throws IOException {
		Path latexResourcesDir = directory.resolve("latex_resources");
		Files.createDirectories(latexResourcesDir);
		copyFile(latexResourcesDir.resolve("ProB_Logo.png"), Main.class.getResourceAsStream("ProB_Logo.png"));
		copyFile(latexResourcesDir.resolve("autodoc.cls"), ProjectDocumenter.class.getResourceAsStream("autodoc.cls"));
	}

	public Optional<Process> documentVelocity() throws IOException {
		buildLatexResources(directory);
		initVelocityEngine();
		VelocityContext context = getVelocityContext();
		String templateName = getLanguageTemplate();
		try (final Writer writer = Files.newBufferedWriter(directory.resolve(filename + ".tex"))) {
			Velocity.mergeTemplate(templateName, String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
		if (makePdf) {
			return Optional.of(createPdf(filename, directory));
		} else{
			return Optional.empty();
		}
	}

	// future translations can be added here
	private String getLanguageTemplate() {
		String language = injector.getInstance(Locale.class).getLanguage();
		return switch (language) {
			case "de" -> "de/prob2/ui/documentation/velocity_template_german.tex.vm";
			default -> "de/prob2/ui/documentation/velocity_template_english.tex.vm";
		};
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

	public String saveTraceHtml(Machine machine, ReplayTrace trace) throws IOException {
		Path htmlDirectory = getHtmlDirectory(machine);
		Files.createDirectories(directory.resolve(htmlDirectory));
		Path htmlPath = htmlDirectory.resolve(trace.getName() + ".html");
		/* reloadMachine works with completable futures. Project access before its finished Loading, can create null Exceptions.
		* To solve this Problem, wait on the CompletableFuture. */
		project.reloadMachine(machine).join();
		final StateSpace stateSpace = injector.getInstance(CurrentTrace.class).getStateSpace();
		TraceChecker.checkNoninteractive(trace, stateSpace);
		ExportVisBForHistoryCommand cmd = new ExportVisBForHistoryCommand(trace.getAnimatedReplayedTrace(), directory.resolve(htmlPath));
		stateSpace.execute(cmd);
		return htmlPath.toString();
	}

	public Path getDirectory() {
		return directory;
	}

	public static Path getHtmlDirectory(Machine machine) {
		return Paths.get("html_files", machine.getName());
	}
}
