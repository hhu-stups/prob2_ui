package de.prob2.ui.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some internals for {@link JsonManager}. This class only handles reading and writing untyped JSON structures (along with metadata). It does not convert these structures to proper objects or check the metadata (type and version) in any way - that is handled by {@link JsonManager}.
 */
@Singleton
final class JsonManagerRaw {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonManagerRaw.class);
	
	private static final String METADATA_PROPERTY = "metadata";
	public static final JsonMetadata MISSING_METADATA = new JsonMetadata(null, 0, null, null, null, null, null);
	
	public static final DateTimeFormatter OLD_METADATA_DATE_FORMATTER = new DateTimeFormatterBuilder()
		.parseCaseInsensitive()
		.parseLenient()
		.appendPattern("d MMM yyyy hh:mm:ssa O")
		.toFormatter();
	
	private final Gson gson;
	
	@Inject
	private JsonManagerRaw(final Gson gson) {
		super();
		
		this.gson = gson;
	}
	
	private static JsonMetadata convertOldMetadata(final JsonElement metadataElement) {
		final JsonObject metadataObject = metadataElement.getAsJsonObject();
		final String oldCreationDateString = metadataObject.get("Creation Date").getAsString();
		Instant creationDateTime;
		try {
			creationDateTime = OLD_METADATA_DATE_FORMATTER.parse(oldCreationDateString, Instant::from);
		} catch (DateTimeParseException e) {
			LOGGER.warn("Failed to parse creation date from old metadata, replacing with null", e);
			creationDateTime = null;
		}
		final String creator = metadataObject.get("Created by").getAsString();
		final String proB2KernelVersion = metadataObject.get("ProB 2.0 kernel Version").getAsString();
		final String proBCliVersion = metadataObject.get("ProB CLI Version").getAsString();
		final JsonElement modelNameElement = metadataObject.get("Model");
		final String modelName = modelNameElement == null ? null : modelNameElement.getAsString();
		return new JsonMetadata(null, 0, creationDateTime, creator, proB2KernelVersion, proBCliVersion, modelName);
	}
	
	public ObjectWithMetadata<JsonObject> readRaw(final Reader reader) {
		final JsonReader jsonReader = new JsonReader(reader);
		// Read the main object from the reader.
		final JsonObject root = JsonParser.parseReader(jsonReader).getAsJsonObject();
		
		final JsonMetadata metadata;
		if (root.has(METADATA_PROPERTY)) {
			// Main object contains metadata, use it.
			LOGGER.trace("Found JSON metadata in main object");
			final JsonElement metadataElement = root.remove(METADATA_PROPERTY);
			metadata = this.gson.fromJson(metadataElement, JsonMetadata.class);
		} else {
			// Main object doesn't contain metadata, check for old metadata as a second JSON object stored directly after the main object.
			// To do this, the reader needs to be set to lenient.
			// Otherwise the parser will consider the second JSON object invalid.
			// (The parser is right about this - JSON does not allow multiple top-level objects in one file - but our old code generated data like this, so we need to handle it.)
			jsonReader.setLenient(true);
			JsonToken firstMetadataToken;
			try {
				firstMetadataToken = jsonReader.peek();
			} catch (IOException e) {
				throw new JsonIOException(e);
			}
			if (firstMetadataToken == JsonToken.END_DOCUMENT) {
				// There is no second JSON object, so we don't have any metadata - substitute an empty default object instead.
				LOGGER.trace("No JSON metadata found");
				metadata = MISSING_METADATA;
			} else {
				// Found a second JSON object, parse it and convert it to the current metadata format.
				LOGGER.trace("Found old JSON metadata after main object");
				metadata = convertOldMetadata(JsonParser.parseReader(jsonReader));
			}
		}
		return new ObjectWithMetadata<>(root, metadata);
	}
	
	public void writeRaw(final Writer writer, final JsonObject src, final JsonMetadata metadata) {
		final JsonWriter jsonWriter = new JsonWriter(writer);
		jsonWriter.setHtmlSafe(false);
		jsonWriter.setIndent("  ");
		src.add(METADATA_PROPERTY, this.gson.toJsonTree(metadata));
		this.gson.toJson(src, jsonWriter);
	}
}
