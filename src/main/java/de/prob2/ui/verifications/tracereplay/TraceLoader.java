package de.prob2.ui.verifications.tracereplay;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;
import com.google.inject.Inject;

import de.prob.check.tracereplay.PersistentTrace;

public class TraceLoader {
	private static final Charset PROJECT_CHARSET = Charset.forName("UTF-8");

	private final Gson gson;

	@Inject
	public TraceLoader(Gson gson) {
		this.gson = gson;
	}

	public PersistentTrace loadTrace(Path path) throws IOException {
		final Reader reader = Files.newBufferedReader(path, PROJECT_CHARSET);
		JsonStreamParser parser = new JsonStreamParser(reader);
		JsonElement element = parser.next();
		if (element.isJsonObject()) {
			return gson.fromJson(element, PersistentTrace.class);
		}
		throw new IOException("The file does not contain a valid trace.");
	}
}
