package de.prob2.ui.internal;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MustacheTemplateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MustacheTemplateManager.class);

    private Mustache mustache;

    private Map<String, Object> placeholders;

    /**
     * creates a {@link MustacheTemplateManager} containing the Mustache template
     *
     * @param uri the uri of the path for the template
     * @param name the name of the template
     */
    public MustacheTemplateManager(URI uri, String name) {
        this.mustache = createMustacheTemplate(uri, name);
        this.placeholders = new HashMap<>();
    }

    /**
     * creates a {@link Mustache} from the given parameters
     *
     * @param uri the uri of the path for the template
     * @param name the name of the template
     * @return returns the resulting Mustache template
     */
    public static Mustache createMustacheTemplate(URI uri, String name) {
        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            //Avoid readString for Java 8 compatibility
            String template = new String(Files.readAllBytes(Paths.get(uri)));
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
