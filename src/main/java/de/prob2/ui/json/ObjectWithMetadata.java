package de.prob2.ui.json;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Represents an object with attached metadata, read from JSON. The main object can have any type, but the attached metadata always has the same structure.
 * 
 * @param <T> the type of the contained main object
 */
public final class ObjectWithMetadata<T> {
	private final T object;
	private final JsonMetadata metadata;
	
	public ObjectWithMetadata(final T object, final JsonMetadata metadata) {
		super();
		
		this.object = Objects.requireNonNull(object);
		this.metadata = Objects.requireNonNull(metadata);
	}
	
	public T getObject() {
		return this.object;
	}
	
	public JsonMetadata getMetadata() {
		return this.metadata;
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		final ObjectWithMetadata<?> other = (ObjectWithMetadata<?>)obj;
		return Objects.equals(this.getObject(), other.getObject())
			&& Objects.equals(this.getMetadata(), other.getMetadata());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getObject(), getMetadata());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("object", this.getObject())
			.add("metadata", this.getMetadata())
			.toString();
	}
}
