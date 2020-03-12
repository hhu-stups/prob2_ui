package de.prob2.ui.json;

import java.time.Instant;

import com.google.inject.Provider;

import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;

public final class JsonMetadataBuilder {
	private final Provider<VersionInfo> versionInfoProvider;
	private final Provider<CurrentProject> currentProjectProvider;
	
	private String fileType;
	private int formatVersion;
	private Instant savedAt = null;
	private String creator = null;
	private String proB2KernelVersion = null;
	private String proBCliVersion = null;
	private String modelName = null;
	
	JsonMetadataBuilder(final Provider<VersionInfo> versionInfoProvider, final Provider<CurrentProject> currentProjectProvider, final String fileType, final int formatVersion) {
		this.versionInfoProvider = versionInfoProvider;
		this.currentProjectProvider = currentProjectProvider;
		
		this.fileType = fileType;
		this.formatVersion = formatVersion;
	}
	
	JsonMetadataBuilder(final Provider<VersionInfo> versionInfoProvider, final Provider<CurrentProject> currentProjectProvider, final JsonMetadata metadata) {
		this.versionInfoProvider = versionInfoProvider;
		this.currentProjectProvider = currentProjectProvider;
		
		this.fileType = metadata.getFileType();
		this.formatVersion = metadata.getFormatVersion();
		this.savedAt = metadata.getSavedAt();
		this.creator = metadata.getCreator();
		this.proB2KernelVersion = metadata.getProB2KernelVersion();
		this.proBCliVersion = metadata.getProBCliVersion();
		this.modelName = metadata.getModelName();
	}
	
	public JsonMetadataBuilder withFileType(final String fileType) {
		this.fileType = fileType;
		return this;
	}
	
	public JsonMetadataBuilder withFormatVersion(final int formatVersion) {
		this.formatVersion = formatVersion;
		return this;
	}
	
	public JsonMetadataBuilder withSavedAt(final Instant savedAt) {
		this.savedAt = savedAt;
		return this;
	}
	
	public JsonMetadataBuilder withCreator(final String creator) {
		this.creator = creator;
		return this;
	}
	
	public JsonMetadataBuilder withProB2KernelVersion(final String proB2KernelVersion) {
		this.proB2KernelVersion = proB2KernelVersion;
		return this;
	}
	
	public JsonMetadataBuilder withProBCliVersion(final String proBCliVersion) {
		this.proBCliVersion = proBCliVersion;
		return this;
	}
	
	public JsonMetadataBuilder withModelName(final String modelName) {
		this.modelName = modelName;
		return this;
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code creator} to {@link JsonMetadata#USER_CREATOR}.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withUserCreator() {
		return this.withCreator(JsonMetadata.USER_CREATOR);
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code savedAt} to the current time.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withSavedNow() {
		return this.withSavedAt(Instant.now());
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code proB2KernelVersion} to the version currently in use.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withCurrentProB2KernelVersion() {
		final VersionInfo versionInfo = versionInfoProvider.get();
		return this.withProB2KernelVersion(versionInfo.getKernelVersion());
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code proBCliVersion} to the version currently in use.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withCurrentProBCliVersion() {
		final VersionInfo versionInfo = versionInfoProvider.get();
		return this.withProBCliVersion(versionInfo.getFormattedCliVersion());
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code savedAt} to the current time, and {@code proB2KernelVersion} and {@code proBCliVersion} to the versions currently in use.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withCurrentInfo() {
		return this.withSavedNow()
			.withCurrentProB2KernelVersion()
			.withCurrentProBCliVersion();
	}
	
	/**
	 * Shorthand for setting the built metadata's {@code modelName} to the name of the currently loaded machine.
	 * 
	 * @return {@code this}
	 */
	public JsonMetadataBuilder withCurrentModelName() {
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
