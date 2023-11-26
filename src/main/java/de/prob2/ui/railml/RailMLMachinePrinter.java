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

	public static void printValidationMachine(Path path, String validationMachineName, String dataMachineName) throws IOException {
		initVelocityEngine();
		VelocityContext context = new VelocityContext();
		context.put("validationMachineName", validationMachineName);
		context.put("dataMachineName", dataMachineName);
		try (final Writer writer = Files.newBufferedWriter(path)) {
			Velocity.mergeTemplate("de/prob2/ui/railml/validation_template.mch.vm",
				String.valueOf(StandardCharsets.UTF_8),context,writer);
		}
	}

	private static void initVelocityEngine() {
		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);
	}
}
