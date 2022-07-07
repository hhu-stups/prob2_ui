package de.prob2.ui.sharedviews;

import java.util.Objects;

import de.prob2.ui.internal.Translatable;

public class PredicateBuilderTableItem {

	public enum VariableType implements Translatable {
		INPUT("internal.predicateBuilderView.variableType.input"),
		OUTPUT("internal.predicateBuilderView.variableType.return"),
		VARIABLE("internal.predicateBuilderView.variableType.variable"),
		CONSTANT("internal.predicateBuilderView.variableType.constant");

		private final String bundleKey;

		VariableType(String bundleKey) {
			this.bundleKey = bundleKey;
		}

		@Override
		public String getTranslationKey() {
			return bundleKey;
		}
	}

	private String name;

	private String value;

	private VariableType type;

	public PredicateBuilderTableItem(String name, String value, VariableType type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public VariableType getType() {
		return type;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PredicateBuilderTableItem other = (PredicateBuilderTableItem) o;
		return Objects.equals(name, other.name) &&
				Objects.equals(value, other.value) &&
				type == other.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value, type);
	}
}
