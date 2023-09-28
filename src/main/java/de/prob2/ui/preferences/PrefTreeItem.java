package de.prob2.ui.preferences;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import de.prob.animator.domainobjects.ProBPreference;

interface PrefTreeItem {
	final class Category implements PrefTreeItem {
		private final String name;
		
		Category(final String name) {
			this.name = name;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
		
		@Override
		public String getValue() {
			return "";
		}
		
		@Override
		public String getDefaultValue() {
			return "";
		}
		
		@Override
		public String getDescription() {
			return "";
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || this.getClass() != obj.getClass()) {
				return false;
			}
			final PrefTreeItem.Category other = (PrefTreeItem.Category)obj;
			return this.getName().equals(other.getName());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getName());
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("name", this.getName())
				.toString();
		}
	}
	
	final class Preference implements PrefTreeItem {
		private final ProBPreference preferenceInfo;
		private final String value;
		
		public Preference(final ProBPreference preferenceInfo, final String value) {
			this.preferenceInfo = preferenceInfo;
			this.value = value;
		}
		
		public ProBPreference getPreferenceInfo() {
			return this.preferenceInfo;
		}
		
		@Override
		public String getName() {
			return this.getPreferenceInfo().name;
		}
		
		@Override
		public String getValue() {
			return this.value;
		}
		
		@Override
		public String getDefaultValue() {
			return this.getPreferenceInfo().defaultValue;
		}
		
		@Override
		public String getDescription() {
			return this.getPreferenceInfo().description;
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || this.getClass() != obj.getClass()) {
				return false;
			}
			final PrefTreeItem.Preference other = (PrefTreeItem.Preference)obj;
			return this.getPreferenceInfo().equals(other.getPreferenceInfo())
				&& this.getValue().equals(other.getValue());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getPreferenceInfo(), this.getValue());
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("preferenceInfo", this.getPreferenceInfo())
				.add("value", this.getValue())
				.toString();
		}
	}
	
	String getName();
	
	String getValue();
	
	String getDefaultValue();
	
	default boolean isChanged() {
		return !Objects.equals(this.getValue(), this.getDefaultValue());
	}
	
	String getDescription();
	
	@Override
	boolean equals(final Object obj);
	
	@Override
	int hashCode();
	
	@Override
	String toString();
}
