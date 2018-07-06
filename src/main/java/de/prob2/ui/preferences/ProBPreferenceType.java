package de.prob2.ui.preferences;

import java.util.Arrays;
import java.util.Objects;

import de.prob.animator.domainobjects.ProBPreference;
import de.prob.prolog.term.ListPrologTerm;

public class ProBPreferenceType {
	private final String type;
	private final String[] values;
	
	public ProBPreferenceType(final String type) {
		Objects.requireNonNull(type);
		
		if ("[]".equals(type)) {
			throw new IllegalArgumentException("Type \"[]\" is reserved for list types");
		}
		
		this.type = type;
		this.values = null;
	}
	
	public ProBPreferenceType(final String[] values) {
		Objects.requireNonNull(values);
		
		this.type = "[]";
		this.values = values.clone();
	}
	
	static ProBPreferenceType fromProBPreference(ProBPreference pref) {
		if (pref.type instanceof ListPrologTerm) {
			final ListPrologTerm values = (ListPrologTerm) pref.type;
			final String[] arr = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {
				arr[i] = values.get(i).getFunctor();
			}
			return new ProBPreferenceType(arr);
		} else {
			return new ProBPreferenceType(pref.type.getFunctor());
		}
	}
	
	public String getType() {
		return this.type;
	}
	
	public String[] getValues() {
		return this.values == null ? null : this.values.clone();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !this.getClass().equals(obj.getClass())) {
			return false;
		}
		
		final ProBPreferenceType casted = (ProBPreferenceType)obj;
		return Objects.equals(this.getType(), casted.getType()) && Arrays.equals(this.getValues(), casted.getValues());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.getType(), Arrays.deepHashCode(this.getValues()));
	}
	
	@Override
	public String toString() {
		return String.format("%s(%s)", this.getClass().getSimpleName(), this.getValues() == null ? this.getType() : Arrays.toString(this.getValues()));
	}
}
