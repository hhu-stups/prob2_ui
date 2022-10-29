package de.prob2.ui.documentation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.modelchecking.ModelCheckingItem;
import de.prob2.ui.verifications.symbolicchecking.SymbolicCheckingFormulaItem;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static de.prob2.ui.documentation.Converter.readFile;

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
	}




	public String getMachineCode(Machine elem) {
		return readFile(project.getLocation().resolve(elem.getLocation()));
	}
	public void documentVelocity(){
		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
		VelocityContext context = new VelocityContext();
		context.put("documenter",this);
		context.put("machines", machines);
		context.put("modelchecking", modelchecking);
		context.put("ltl", ltl);
		context.put("symbolic", symbolic);
		context.put("converter", new Converter());
		context.put("i18n", i18n);
		StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("de/prob2/ui/documentation/velocity_template.tex", String.valueOf(StandardCharsets.UTF_8),context,writer);
		Converter.stringToTex(writer.toString(), filename, dir);
		if(makePdf)
			createPdf();
	}


	public boolean formulaHasResult(LTLFormulaItem formula){return (formula.getResultItem() != null);}
	public boolean symbolicHasResult(SymbolicCheckingFormulaItem formula){return (formula.getResultItem() != null);}

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

	public String toUIString(ModelCheckingItem item) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}
}
