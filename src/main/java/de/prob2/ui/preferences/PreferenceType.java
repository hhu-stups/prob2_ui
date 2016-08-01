package de.prob2.ui.preferences;

import java.util.Arrays;
import java.util.Objects;

public class PreferenceType {
	private final String type;
	private final String[] values;
	
	public PreferenceType(final String type) {
		Objects.requireNonNull(type);
		
		if ("[]".equals(type)) {
			throw new IllegalArgumentException("Type \"[]\" is reserved for list types");
		}
		
		this.type = type;
		this.values = null;
	}
	
	public PreferenceType(final String[] values) {
		Objects.requireNonNull(values);
		
		this.type = "[]";
		this.values = values.clone();
	}
	
	public String getType() {
		return this.type;
	}
	
	public String[] getValues() {
		return this.values == null ? null : this.values.clone();
	}
	
	public boolean equals(final Object obj) {
		if (obj == null || !this.getClass().equals(obj.getClass())) {
			return false;
		}
		
		final PreferenceType casted = (PreferenceType)obj;
		return Objects.equals(this.getType(), casted.getType()) && Arrays.equals(this.getValues(), casted.getValues());
	}
	
	public int hashCode() {
		return Objects.hash(this.getType(), Arrays.deepHashCode(this.getValues()));
	}
	
	public String toString() {
		return String.format("PreferenceType(%s)", this.getValues() == null ? this.getType() : Arrays.toString(this.getValues()));
	}
}
