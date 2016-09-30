package de.prob2.ui.operations;

import java.util.List;

import de.prob.unicode.UnicodeTranslator;

public class Operation {
	public final String name;
	public final List<String> params;
	public final String id;
	public final String enablement;
	public final List<String> returnValues;
	final boolean explored;
	final boolean errored;

	public Operation(final String id, final String name, final List<String> params, List<String> returnValues,
			final boolean isEnabled,
			final boolean hasTimeout, final boolean explored, final boolean errored) {
		this.id = id;
		this.name = name;
		this.params = params;
		this.returnValues = returnValues;
		enablement = isEnabled ? "enabled" : hasTimeout ? "timeout" : "notEnabled";
		this.explored = explored;
		this.errored = errored;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!returnValues.isEmpty()) {
			sb.append(String.join(", ", returnValues));
			sb.append(UnicodeTranslator.toUnicode(" <-- "));
		}
		sb.append(name);
		if (!params.isEmpty()) {
			sb.append("(");
			sb.append(String.join(", ", params));
			sb.append(")");
		}
		return sb.toString();
	}

	public boolean isEnabled() {
		return "enabled".equals(enablement);
	}
}
