package de.prob2.ui.railml;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RailMLMachinePrinter {

	public static void printDataMachine(Path path, String dataMachineName, String VARS_AS_TYPED_STRING_railML_identifiers,
																			String VARS_AS_TYPED_STRING_railML_contents, String all_ids, String data,
																			String SETS_railML) throws IOException {
		initVelocityEngine();
		VelocityContext context = new VelocityContext();
		context.put("dataMachineName", dataMachineName);
		context.put("VARS_AS_TYPED_STRING_railML_identifiers", VARS_AS_TYPED_STRING_railML_identifiers);
		context.put("VARS_AS_TYPED_STRING_railML_contents", VARS_AS_TYPED_STRING_railML_contents);
		context.put("all_ids", all_ids);
		context.put("data", data);
		context.put("SETS_railML", SETS_railML);
		try (final Writer writer = Files.newBufferedWriter(path)) {
			Velocity.mergeTemplate("de/prob2/ui/railml/data_template.mch.vm",
				String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	public static void printAnimationMachine(Path path, String animationMachineName, String dataMachineName,
											 boolean generateSvg, String svgFile) throws IOException {
		initVelocityEngine();
		VelocityContext context = new VelocityContext();
		context.put("animationMachineName", animationMachineName);
		context.put("dataMachineName", dataMachineName);
		context.put("generateSvg", generateSvg);
		context.put("svgFile", svgFile);
		try (final Writer writer = Files.newBufferedWriter(path)) {
			Velocity.mergeTemplate("de/prob2/ui/railml/animation_template.mch.vm",
				String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	public static void printValidationMachine(Path path, String validationMachineName, String dataMachineName) throws IOException {
		initVelocityEngine();
		VelocityContext context = new VelocityContext();
		context.put("validationMachineName", validationMachineName);
		context.put("dataMachineName", dataMachineName);
		try (final Writer writer = Files.newBufferedWriter(path)) {
			Velocity.mergeTemplate("de/prob2/ui/railml/validation_template.rmch.vm",
				String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	private static void initVelocityEngine() {
		Properties p = new Properties();
		p.setProperty("resource.loaders", "class");
		p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
	}
}
