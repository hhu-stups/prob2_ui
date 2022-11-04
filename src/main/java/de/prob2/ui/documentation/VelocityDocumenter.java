package de.prob2.ui.documentation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.Main;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import de.prob2.ui.visb.VisBStage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static de.prob2.ui.documentation.DocumentUtility.*;

public class VelocityDocumenter {

	private final String filename;
	private final I18n i18n;
	private final Path dir;
	private final boolean modelchecking;
	private final boolean ltl;
	private final boolean symbolic;
	private final boolean makePdf;
	private final List<Machine> machines;
	CurrentProject project;
	Injector injector;

	@Inject
	public VelocityDocumenter(CurrentProject project,
					  I18n i18n, boolean modelchecking,
					  boolean ltl, boolean symbolic,boolean makePdf,
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
		this.machines = machines;
		this.dir = dir;
		this.filename = filename;
		this.injector = injector;
		for (Machine machine : machines) {
			for (ReplayTrace trace : machine.getTraces()) {
				try {
					Files.createDirectories(Paths.get(getPath(machine, trace))
					);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public void documentVelocity() throws TemplateInitException, ResourceNotFoundException, MethodInvocationException, ParseErrorException {
		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
		VelocityContext context = getVelocityContext();
		StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("de/prob2/ui/documentation/velocity_template.tex", String.valueOf(StandardCharsets.UTF_8),context,writer);
		DocumentUtility.stringToTex(writer.toString(), filename, dir);
		if(makePdf)
			createPdf(filename,dir);
	}

	public String saveProBLogo() {
		try {
			String pathname = dir+"/images/ProB_Logo.png";
			BufferedImage proBLogo = ImageIO.read(Objects.requireNonNull(Main.class.getResource("ProB_Logo.png")));
			ImageIO.write(proBLogo, "PNG", new File(pathname));
			return pathname;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private VelocityContext getVelocityContext() {
		VelocityContext context = new VelocityContext();
		context.put("project",project);
		context.put("documenter",this);
		context.put("machines", machines);
		context.put("modelchecking", modelchecking);
		context.put("ltl", ltl);
		context.put("symbolic", symbolic);
		context.put("DocumentUtility", DocumentUtility.class);
		context.put("Transition", Transition.class);
		context.put("i18n", i18n);
		return context;
	}
	public String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}

	public List<String> saveTraceImage(Machine machine, ReplayTrace trace){
		List<String> imagePaths = new ArrayList<>();
		project.startAnimation(machine, Preference.DEFAULT);
		CurrentTrace currentTrace = injector.getInstance(CurrentTrace.class);
		//REPLAYED TRACE ZU TRACE
		currentTrace.set(trace.getAnimatedReplayedTrace());
		VisBStage stage = injector.getInstance(VisBStage.class);
		stage.initialize();
		stage.show();
		//stage.getCurrentTrace().addTransitions(trace.getAnimatedReplayedTrace().getTransitionList());
		//stage.reloadVisualisation();
		WritableImage snapshot = stage.getWebViewSnapshot();
		int count = 1;
		for(PersistentTransition transition : trace.getLoadedTrace().getTransitionList()){
			currentTrace.forward();
			String imagePath = getPath(machine,trace)+"/image"+count+".png";
			imagePaths.add(imagePath);
			Path path = Paths.get(imagePath);
			count++;
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "PNG", path.toFile());
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			stage.closeVisualisation();
		}
		stage.close();
		return imagePaths;
	}

	public boolean formulaHasResult(LTLFormulaItem formula){return (formula.getResultItem() != null);}
	public boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}
	private String getPath(Machine machine, ReplayTrace trace) {
		return dir + "/images/" + machine.getName() + "/" + Transition.prettifyName(trace.getName());
	}

}
