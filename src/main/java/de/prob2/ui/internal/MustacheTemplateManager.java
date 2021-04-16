package de.prob2.ui.internal;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MustacheTemplateManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(MustacheTemplateManager.class);

	private Mustache mustache;

	private Map<String, Object> placeholders;

	/**
	 * creates a {@link MustacheTemplateManager} containing the Mustache template
	 *
	 * @param inputStream the input stream from the path for the template
	 * @param name the name of the template
	 */
	public MustacheTemplateManager(InputStream inputStream, String name) {
		this.mustache = createMustacheTemplate(inputStream, name);
		this.placeholders = new HashMap<>();
	}

	/**
	 * creates a {@link Mustache} from the given parameters
	 *
	 * @param inputStream the input stream from the path for the template
	 * @param name the name of the template
	 * @return returns the resulting Mustache template
	 */
	public static Mustache createMustacheTemplate(InputStream inputStream, String name) {
		try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(inputStreamReader)) {
			MustacheFactory mf = new DefaultMustacheFactory();
			//Avoid readString for Java 8 compatibility
			String template = reader.lines().collect(Collectors.joining("\n"));
			return mf.compile(new StringReader(template), name);
		} catch (IOException e) {
			LOGGER.error("", e);
			return null;
		}
	}

	/**
	 * adds an object replacing the given key
	 *
	 * @param key the key representing the name of the placeholder
	 * @param object the object replacing the placeholder
	 */
	public void put(String key, Object object) {
		placeholders.put(key, object);
	}

	/**
	 * replaces the given placeholders in the Mustache template by its objects and returns the resulting String
	 * @return the resulting String
	 */
	public String apply() {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
			mustache.execute(writer, placeholders);
			writer.flush();
			return stream.toString();
		} catch (IOException e) {
			LOGGER.error("", e);
			return null;
		}
	}

}
