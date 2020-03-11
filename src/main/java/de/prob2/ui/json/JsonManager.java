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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utilities for reading and writing JSON data with attached metadata, in a way that correctly handles data from older and newer UI versions.
 */
@Singleton
public final class JsonManager {
	public final class MetadataBuilder {
		private String fileType;
		private int formatVersion;
		private Instant savedAt = null;
		private String creator = null;
		private String proB2KernelVersion = null;
		private String proBCliVersion = null;
		private String modelName = null;
		
		private MetadataBuilder(final String fileType, final int formatVersion) {
			this.fileType = fileType;
			this.formatVersion = formatVersion;
		}
		
		private MetadataBuilder(final JsonMetadata metadata) {
			this.fileType = metadata.getFileType();
			this.formatVersion = metadata.getFormatVersion();
			this.savedAt = metadata.getSavedAt();
			this.creator = metadata.getCreator();
			this.proB2KernelVersion = metadata.getProB2KernelVersion();
			this.proBCliVersion = metadata.getProBCliVersion();
			this.modelName = metadata.getModelName();
		}
		
		public MetadataBuilder withFileType(final String fileType) {
			this.fileType = fileType;
			return this;
		}
		
		public MetadataBuilder withFormatVersion(final int formatVersion) {
			this.formatVersion = formatVersion;
			return this;
		}
		
		public MetadataBuilder withSavedAt(final Instant savedAt) {
			this.savedAt = savedAt;
			return this;
		}
		
		public MetadataBuilder withCreator(final String creator) {
			this.creator = creator;
			return this;
		}
		
		public MetadataBuilder withProB2KernelVersion(final String proB2KernelVersion) {
			this.proB2KernelVersion = proB2KernelVersion;
			return this;
		}
		
		public MetadataBuilder withProBCliVersion(final String proBCliVersion) {
			this.proBCliVersion = proBCliVersion;
			return this;
		}
		
		public MetadataBuilder withModelName(final String modelName) {
			this.modelName = modelName;
			return this;
		}
		
		/**
		 * Shorthand for setting the built metadata's {@code creator} to {@link JsonMetadata#USER_CREATOR}.
		 * 
		 * @return {@code this}
		 */
		public MetadataBuilder withUserCreator() {
			return this.withCreator(JsonMetadata.USER_CREATOR);
		}
		
		/**
		 * Shorthand for setting the built metadata's {@code savedAt} to the current time, and {@code proB2KernelVersion} and {@code proBCliVersion} to the versions currently in use.
		 * 
		 * @return {@code this}
		 */
		public MetadataBuilder withCurrentInfo() {
			final VersionInfo versionInfo = versionInfoProvider.get();
			return this.withSavedAt(Instant.now())
				.withProB2KernelVersion(versionInfo.getKernelVersion())
				.withProBCliVersion(versionInfo.getFormattedCliVersion());
		}
		
		/**
		 * Shorthand for setting the built metadata's {@code modelName} to the name of the currently loaded machine.
		 * 
		 * @return {@code this}
		 */
		public MetadataBuilder withCurrentModelName() {
			final CurrentProject currentProject = currentProjectProvider.get();
			if (currentProject.getCurrentMachine() == null) {
				throw new IllegalStateException("withCurrentModelName() can only be called while a machine is loaded");
			}
			return this.withModelName(currentProject.getCurrentMachine().getName());
		}
		
		public JsonMetadata build() {
			return new JsonMetadata(
				this.fileType,
				this.formatVersion,
				this.savedAt,
				this.creator,
				this.proB2KernelVersion,
				this.proBCliVersion,
				this.modelName
			);
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonManager.class);
	
	private static final String METADATA_PROPERTY = "metadata";
	public static final JsonMetadata MISSING_METADATA = new JsonMetadata(null, 0, null, null, null, null, null);
	
	public static final DateTimeFormatter OLD_METADATA_DATE_FORMATTER = new DateTimeFormatterBuilder()
		.parseCaseInsensitive()
		.parseLenient()
		.appendPattern("d MMM yyyy hh:mm:ssa O")
		.toFormatter();
	
	private final Gson gson;
	private final Provider<VersionInfo> versionInfoProvider;
	private final Provider<CurrentProject> currentProjectProvider;
	
	@Inject
	private JsonManager(final Gson gson, final Provider<VersionInfo> versionInfoProvider, final Provider<CurrentProject> currentProjectProvider) {
		super();
		
		this.gson = gson;
		this.versionInfoProvider = versionInfoProvider;
		this.currentProjectProvider = currentProjectProvider;
	}
	
	/**
	 * Create a builder for a new {@link JsonMetadata} object.
	 * 
	 * @param fileType identifier for the type of data that this metadata belongs to, should never be {@code null}
	 * @param formatVersion version of the data format
	 * 
	 * @return a builder for a new {@link JsonMetadata object}
	 */
	public MetadataBuilder metadataBuilder(final String fileType, final int formatVersion) {
		return new MetadataBuilder(fileType, formatVersion);
	}
	
	/**
	 * Create a builder for a {@link JsonMetadata} object based on an existing metadata object.
	 * 
	 * @param metadata an existing {@link JsonMetadata} object used to initialize this builder
	 * 
	 * @return a builder for a {@link JsonMetadata} object based on an existing metadata object
	 */
	public MetadataBuilder metadataBuilder(final JsonMetadata metadata) {
		return new MetadataBuilder(metadata);
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
	
	private ObjectWithMetadata<JsonObject> readRaw(final Reader reader) {
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
	
	public <T> ObjectWithMetadata<T> read(final Reader reader, final Class<T> classOfT, final String expectedFileType, final int currentFormatVersion) {
		LOGGER.trace("Attempting to load JSON data of type {}, current version {}", expectedFileType, currentFormatVersion);
		final ObjectWithMetadata<JsonObject> rawWithMetadata = this.readRaw(reader);
		final JsonMetadata metadata = rawWithMetadata.getMetadata();
		LOGGER.trace("Found JSON data of type {}, version {}", metadata.getFileType(), metadata.getFormatVersion());
		// TODO Perform additional type validation checks if file type is missing (null)?
		if (metadata.getFileType() != null && !metadata.getFileType().equals(expectedFileType)) {
			throw new JsonParseException("Expected JSON data of type " + expectedFileType + " but got " + metadata.getFileType());
		}
		if (metadata.getFormatVersion() > currentFormatVersion) {
			throw new JsonParseException("JSON data uses format version " + metadata.getFormatVersion() + ", which is newer than the newest supported version (" + currentFormatVersion + ")");
		}
		if (metadata.getFormatVersion() < currentFormatVersion) {
			// TODO Convert/update old data
			throw new JsonParseException("JSON data uses format version " + metadata.getFormatVersion() + ", which is older than the current version (" + currentFormatVersion + "), and conversion of old data is not supported yet");
		}
		final T obj = this.gson.fromJson(rawWithMetadata.getObject(), classOfT);
		return new ObjectWithMetadata<>(obj, metadata);
	}
	
	public <T> void write(final Writer writer, final T src, final JsonMetadata metadata) {
		final JsonWriter jsonWriter = new JsonWriter(writer);
		jsonWriter.setHtmlSafe(false);
		jsonWriter.setIndent("  ");
		final JsonObject root = this.gson.toJsonTree(src).getAsJsonObject();
		root.add(METADATA_PROPERTY, this.gson.toJsonTree(metadata));
		this.gson.toJson(root, jsonWriter);
	}
}
