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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class MustacheTemplateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MustacheTemplateManager.class);

    private Mustache mustache;

    private Map<String, Object> placeholders;

    public MustacheTemplateManager(URI uri, String name) {
        this.mustache = createMustacheTemplate(uri, name);
        this.placeholders = new HashMap<>();
    }

    public static Mustache createMustacheTemplate(URI uri, String name) {
        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            String template = Files.readString(Paths.get(uri));
            return mf.compile(new StringReader(template), name);
        } catch (IOException e) {
            LOGGER.error("", e);
            return null;
        }
    }

    public void put(String key, Object object) {
        placeholders.put(key, object);
    }

    public String apply() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(stream);
            mustache.execute(writer, placeholders);
            writer.flush();
            return stream.toString();
        } catch (IOException e) {
            LOGGER.error("", e);
            return null;
        }
    }

}
