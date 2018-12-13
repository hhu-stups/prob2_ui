package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;
import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.prob2fx.CurrentProject;

public class TraceLoader {
	private static final Charset PROJECT_CHARSET = StandardCharsets.UTF_8;

	private final Gson gson;
	private final CurrentProject currentProject;

	@Inject
	public TraceLoader(Gson gson, CurrentProject currentProject) {
		this.gson = gson;
		this.currentProject = currentProject;
	}

	public PersistentTrace loadTrace(Path path) throws InvalidFileFormatException, IOException {
		path = currentProject.get().getLocation().resolve(path);
		final Reader reader = Files.newBufferedReader(path, PROJECT_CHARSET);
		JsonStreamParser parser = new JsonStreamParser(reader);
		JsonElement element = parser.next();
		if (element.isJsonObject()) {
			PersistentTrace trace = gson.fromJson(element, PersistentTrace.class);
			if(isValidTrace(trace)) {
				return trace;
			}
		}
		throw new InvalidFileFormatException("The file does not contain a valid trace.");
	}

	private boolean isValidTrace(PersistentTrace trace) {
		return trace.getTransitionList() != null;
	}
}
