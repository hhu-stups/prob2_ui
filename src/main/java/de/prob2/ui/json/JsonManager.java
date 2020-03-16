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
		protected final Class<T> clazz;
		protected final String fileType;
		protected final int currentFormatVersion;
		
		public Context(final Class<T> clazz, final String fileType, final int currentFormatVersion) {
			this.clazz = Objects.requireNonNull(clazz, "clazz");
			this.fileType = Objects.requireNonNull(fileType, "fileType");
			this.currentFormatVersion = currentFormatVersion;
		}
		
		/**
		 * <p>Convert data from an older format version to the current version.</p>
		 * <p>This method must be overridden to support loading data that uses an older format version. The default implementation of this method always throws a {@link JsonParseException}.</p>
		 * <p>The converted object and metadata are returned from this method. The returned {@link JsonObject} may be a completely new object, or it may be {@code oldObject} after being modified in place. The returned {@link JsonMetadata} does <i>not</i> need to have its version number updated.</p>
		 * 
		 * @param oldObject the old data to convert
		 * @param oldMetadata the metadata attached to the old data
		 * @return the converted data and updated metadata
		 * @throws JsonParseException if the data could not be converted
		 */
		public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
			throw new JsonParseException("JSON data uses old format version " + oldMetadata.getFormatVersion() + ", which cannot be converted to the current version " + this.currentFormatVersion);
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
	 * Create a builder for a new {@link JsonMetadata} object. The file type and version are initialized based on the settings in the context.
	 *
	 * @return a builder for a new {@link JsonMetadata object}
	 */
	public JsonMetadataBuilder metadataBuilder() {
		return this.jsonManager.metadataBuilder(this.getContext().fileType, this.getContext().currentFormatVersion);
	}
	
	/**
	 * Create a builder for a {@link JsonMetadata} object based on an existing metadata object.
	 *
	 * @param metadata an existing {@link JsonMetadata} object used to initialize this builder
	 *
	 * @return a builder for a {@link JsonMetadata} object based on an existing metadata object
	 */
	public JsonMetadataBuilder metadataBuilder(final JsonMetadata metadata) {
		return this.jsonManager.metadataBuilder(metadata);
	}
	
	/**
	 * Create a builder for a {@link JsonMetadata} object with default settings. Subclasses may override this method to change the defaults.
	 * 
	 * @return a builder for a {@link JsonMetadata} object with default settings
	 */
	public JsonMetadataBuilder defaultMetadataBuilder() {
		return this.metadataBuilder()
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
		JsonObject rawObject = rawWithMetadata.getObject();
		JsonMetadata metadata = rawWithMetadata.getMetadata();
		LOGGER.trace("Found JSON data of type {}, version {}", metadata.getFileType(), metadata.getFormatVersion());
		// TODO Perform additional type validation checks if file type is missing (null)?
		if (metadata.getFileType() != null && !metadata.getFileType().equals(this.getContext().fileType)) {
			throw new JsonParseException("Expected JSON data of type " + this.getContext().fileType + " but got " + metadata.getFileType());
		}
		if (metadata.getFormatVersion() > this.getContext().currentFormatVersion) {
			throw new JsonParseException("JSON data uses format version " + metadata.getFormatVersion() + ", which is newer than the newest supported version (" + this.getContext().currentFormatVersion + ")");
		}
		if (metadata.getFormatVersion() < this.getContext().currentFormatVersion) {
			LOGGER.info("Converting JSON data from old version {} to current version {}", metadata.getFormatVersion(), this.getContext().currentFormatVersion);
			final ObjectWithMetadata<JsonObject> converted = this.getContext().convertOldData(rawObject, metadata);
			rawObject = converted.getObject();
			metadata = converted.getMetadata();
		}
		final T obj = this.gson.fromJson(rawObject, this.getContext().clazz);
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
