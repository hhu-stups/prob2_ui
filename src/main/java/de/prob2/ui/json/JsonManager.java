package de.prob2.ui.json;

import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utilities for reading and writing JSON data with attached metadata, in a way that correctly handles data from older and newer UI versions.
 */
public class JsonManager<T> {
	public static class Context<T> {
		final Class<T> clazz;
		final String fileType;
		final int currentFormatVersion;
		
		public Context(final Class<T> clazz, final String fileType, final int currentFormatVersion) {
			this.clazz = Objects.requireNonNull(clazz, "clazz");
			this.fileType = Objects.requireNonNull(fileType, "fileType");
			this.currentFormatVersion = currentFormatVersion;
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonManager.class);
	
	private final JsonManagerRaw jsonManager;
	private final Gson gson;
	
	private JsonManager.Context<T> context;
	
	@Inject
	private JsonManager(final JsonManagerRaw jsonManager, final Gson gson) {
		super();
		
		this.jsonManager = jsonManager;
		this.gson = gson;
		this.context = null;
	}
	
	public JsonManager.Context<T> getContext() {
		if (this.context == null) {
			throw new IllegalStateException("context not set");
		}
		return this.context;
	}
	
	/**
	 * Set the context for this {@link JsonManager}. This method can only be called once per instance (the context cannot be changed or replaced afterwards).
	 * 
	 * @param context the context to use
	 */
	public void initContext(final JsonManager.Context<T> context) {
		if (this.context != null) {
			throw new IllegalStateException("context can only be set once");
		}
		this.context = Objects.requireNonNull(context, "context");
	}
	
	/**
	 * Create a builder for a {@link JsonMetadata} object with default settings. Subclasses may override this method to change the defaults.
	 * 
	 * @return a builder for a {@link JsonMetadata} object with default settings
	 */
	public JsonMetadataBuilder defaultMetadataBuilder() {
		return this.jsonManager.metadataBuilder(this.getContext().fileType, this.getContext().currentFormatVersion)
			.withCurrentInfo()
			.withUserCreator();
	}
	
	/**
	 * Read an object along with its metadata from the JSON data in the reader. The file type and version number are checked against the settings in the context.
	 * 
	 * @param reader the {@link Reader} from which to read the JSON data
	 * @return the read object along with its metadata
	 */
	public ObjectWithMetadata<T> read(final Reader reader) {
		LOGGER.trace("Attempting to load JSON data of type {}, current version {}", this.getContext().fileType, this.getContext().currentFormatVersion);
		final ObjectWithMetadata<JsonObject> rawWithMetadata = this.jsonManager.readRaw(reader);
		final JsonMetadata metadata = rawWithMetadata.getMetadata();
		LOGGER.trace("Found JSON data of type {}, version {}", metadata.getFileType(), metadata.getFormatVersion());
		// TODO Perform additional type validation checks if file type is missing (null)?
		if (metadata.getFileType() != null && !metadata.getFileType().equals(this.getContext().fileType)) {
			throw new JsonParseException("Expected JSON data of type " + this.getContext().fileType + " but got " + metadata.getFileType());
		}
		if (metadata.getFormatVersion() > this.getContext().currentFormatVersion) {
			throw new JsonParseException("JSON data uses format version " + metadata.getFormatVersion() + ", which is newer than the newest supported version (" + this.getContext().currentFormatVersion + ")");
		}
		if (metadata.getFormatVersion() < this.getContext().currentFormatVersion) {
			// TODO Convert/update old data
			throw new JsonParseException("JSON data uses format version " + metadata.getFormatVersion() + ", which is older than the current version (" + this.getContext().currentFormatVersion + "), and conversion of old data is not supported yet");
		}
		final T obj = this.gson.fromJson(rawWithMetadata.getObject(), this.getContext().clazz);
		return new ObjectWithMetadata<>(obj, metadata);
	}
	
	/**
	 * Write an object as JSON to the writer, along with the provided metadata.
	 * 
	 * @param writer the {@link Writer} to which to write the JSON data
	 * @param src the object to write
	 * @param metadata the metadata to attach to the JSON data
	 */
	public void write(final Writer writer, final T src, final JsonMetadata metadata) {
		this.jsonManager.writeRaw(writer, this.gson.toJsonTree(src).getAsJsonObject(), metadata);
	}
	
	/**
	 * Write an object as JSON to the writer, along with default metadata built using {@link #defaultMetadataBuilder()}.
	 *
	 * @param writer the {@link Writer} to which to write the JSON data
	 * @param src the object to write
	 */
	public void write(final Writer writer, final T src) {
		this.write(writer, src, this.defaultMetadataBuilder().build());
	}
}
