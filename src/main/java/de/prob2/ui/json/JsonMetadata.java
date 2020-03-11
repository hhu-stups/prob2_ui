package de.prob2.ui.json;

import java.io.Reader;
import java.time.Instant;
import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * <p>Describes the format for the metadata stored in the ProB 2 UI's JSON files. This metadata is used by {@link JsonManager} to ensure that when JSON data is read, it is of the expected type and is compatible with the current UI version.</p>
 * 
 * <p>Some of the metadata (e. g. {@link #creator}) is not yet used by the UI code, but is included in newly written JSON files for possible future use.</p>
 */
public final class JsonMetadata {
	/**
	 * Special value for {@link #creator} indicating that the data was created manually by the user, rather than generated or otherwise automatically created by the UI code.
	 */
	public static final String USER_CREATOR = "User";
	
	/**
	 * <p>Identifies the type of data stored in the JSON data.</p>
	 * 
	 * <p>When reading JSON data using {@link JsonManager#read(Reader, Class, String, int)}, this value is compared against the expected file type (provided by the caller), and an exception is thrown if it does not match the expected type.</p>
	 * 
	 * <p>When reading old JSON files that have no file type metadata, this value is {@code null}. In all other cases, it should never be {@code null}.</p>
	 */
	private final String fileType;
	
	/**
	 * <p>Identifies the version of the data format. Newer UI versions can (in general) read older format versions, but not the other way around. This number must be incremented when an incompatible change is made to the data format (i. e. a change that makes the data unreadable to older UI versions).</p>
	 * 
	 * <p>When reading JSON data using {@link JsonManager#read(Reader, Class, String, int)}, this value is compared against the current format version (provided by the caller), and an exception is thrown if the version in the file is higher than the current supported version.</p>
	 * 
	 * <p>When reading old JSON files that have no format version metadata, this value is {@code 0}.</p>
	 */
	private final int formatVersion;
	
	/**
	 * The point in time when the data was saved.
	 */
	private final Instant savedAt;
	
	/**
	 * Describes how the data was created. This is usually set to {@link #USER_CREATOR} (indicating that the data was created manually), but should be set to a different value if the data was generated or otherwise automatically created by the UI code.
	 */
	private final String creator;
	
	/**
	 * The version of the ProB 2 Java API used when the data was saved.
	 */
	private final String proB2KernelVersion;
	
	/**
	 * The version of the ProB CLI used when the data was saved.
	 */
	private final String proBCliVersion;
	
	/**
	 * The name of the model that this data belongs to. This value is optional and should only be set for data that belongs to a specific model (e. g. trace files). For other data, it should be set to {@code null}.
	 */
	private final String modelName;
	
	public JsonMetadata(
		final String fileType,
		final int formatVersion,
		final Instant savedAt,
		final String creator,
		final String proB2KernelVersion,
		final String proBCliVersion,
		final String modelName
	) {
		this.fileType = fileType;
		this.formatVersion = formatVersion;
		this.savedAt = savedAt;
		this.creator = creator;
		this.proB2KernelVersion = proB2KernelVersion;
		this.proBCliVersion = proBCliVersion;
		this.modelName = modelName;
	}
	
	public String getFileType() {
		return this.fileType;
	}
	
	public int getFormatVersion() {
		return this.formatVersion;
	}
	
	public Instant getSavedAt() {
		return this.savedAt;
	}
	
	public String getCreator() {
		return this.creator;
	}
	
	public String getProB2KernelVersion() {
		return this.proB2KernelVersion;
	}
	
	public String getProBCliVersion() {
		return this.proBCliVersion;
	}
	
	public String getModelName() {
		return this.modelName;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final JsonMetadata other = (JsonMetadata)obj;
		return Objects.equals(this.getFileType(), other.getFileType())
			&& this.getFormatVersion() == other.getFormatVersion()
			&& Objects.equals(this.getSavedAt(), other.getSavedAt())
			&& Objects.equals(this.getCreator(), other.getCreator())
			&& Objects.equals(this.getProB2KernelVersion(), other.getProB2KernelVersion())
			&& Objects.equals(this.getProBCliVersion(), other.getProBCliVersion())
			&& Objects.equals(this.getModelName(), other.getModelName());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
			this.getFileType(),
			this.getFormatVersion(),
			this.getSavedAt(),
			this.getCreator(),
			this.getProB2KernelVersion(),
			this.getProBCliVersion(),
			this.getModelName()
		);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("fileType", this.getFileType())
			.add("formatVersion", this.getFormatVersion())
			.add("savedAt", this.getSavedAt())
			.add("creator", this.getCreator())
			.add("proB2KernelVersion", this.getProB2KernelVersion())
			.add("proBCliVersion", this.getProBCliVersion())
			.add("modelName", this.getModelName())
			.toString();
	}
}
