package de.prob2.ui.visualisation.magiclayout;

import de.prob.json.HasMetadata;
import de.prob.json.JsonMetadata;
import de.prob.json.JsonMetadataBuilder;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MagicLayoutSettings implements HasMetadata {
	public static final String FILE_TYPE = "Magic Layout settings";
	public static final int CURRENT_FORMAT_VERSION = 1;

	private final String machineName;
	private final List<MagicNodegroup> nodegroups;
	private final List<MagicEdgegroup> edgegroups;
	private final JsonMetadata metadata;

	@JsonCreator
	public MagicLayoutSettings(
		@JsonProperty("machineName") final String machineName,
		@JsonProperty("nodegroups") final List<MagicNodegroup> nodegroups,
		@JsonProperty("edgegroups") final List<MagicEdgegroup> edgegroups,
		@JsonProperty("metadata") final JsonMetadata metadata
	) {
		this.machineName = machineName;
		this.nodegroups = nodegroups;
		this.edgegroups = edgegroups;
		this.metadata = metadata;
	}
	
	public static JsonMetadataBuilder metadataBuilder() {
		return new JsonMetadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
			.withSavedNow()
			.withUserCreator();
	}
	
	public String getMachineName() {
		return machineName;
	}
	
	public List<MagicNodegroup> getNodegroups() {
		return nodegroups;
	}
	
	public List<MagicEdgegroup> getEdgegroups() {
		return edgegroups;
	}
	
	@Override
	public JsonMetadata getMetadata() {
		return this.metadata;
	}
	
	@Override
	public MagicLayoutSettings withMetadata(final JsonMetadata metadata) {
		return new MagicLayoutSettings(
			this.getMachineName(),
			this.getNodegroups(),
			this.getEdgegroups(),
			metadata
		);
	}
}
