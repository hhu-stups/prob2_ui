package de.prob2.ui.verifications.po;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Helper class used to save {@link ProofObligationItem}s in the project file.
 * This is needed because {@link ProofObligationItem#equals(Object)} detects some differences that aren't saved in the project,
 * which leads to unnecessary warnings about unsaved changes.
 */
public final class SavedProofObligationItem {
	private final String id;
	private final String name;
	
	@JsonCreator
	public SavedProofObligationItem(
		@JsonProperty("id") final String id,
		@JsonProperty("name") final String name
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
	}
	
	public SavedProofObligationItem(final ProofObligationItem po) {
		this(po.getId(), po.getName());
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final SavedProofObligationItem other = (SavedProofObligationItem)obj;
		return this.getId().equals(other.getId()) && this.getName().equals(other.getName());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.getId(), this.getName());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", this.getId())
			.add("name", this.getName())
			.toString();
	}
}
