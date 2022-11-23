package de.prob2.ui.verifications.po;

import de.prob.model.eventb.ProofObligation;
import de.prob.util.Tuple2;
import de.prob2.ui.verifications.Checked;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ProofObligationItem {

	private String id;
	private final String name;
	private final String description;
	private final String sourceName;
	private final boolean discharged;
	private final Checked checked;
	private final List<Tuple2<String, String>> content;

	public ProofObligationItem(final String name, final String description, final String sourceName, boolean discharged, List<Tuple2<String, String>> content) {
		this.id = null;
		this.name = name;
		this.description = description;
		this.sourceName = sourceName;
		this.discharged = discharged;
		this.checked = discharged ? Checked.SUCCESS : Checked.UNKNOWN;
		this.content = content;
	}

	public ProofObligationItem(ProofObligation proofObligation) {
		this.id = null;
		this.name = proofObligation.getName();
		this.description = proofObligation.getDescription();
		this.sourceName = proofObligation.getSourceName();
		this.discharged = proofObligation.isDischarged();
		this.checked = proofObligation.isDischarged() ? Checked.SUCCESS : Checked.UNKNOWN;
		this.content = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getSourceName() {
		return sourceName;
	}

	public boolean isDischarged() {
		return discharged;
	}

	public String getDescription() {
		return description;
	}

	public Checked getChecked() {
		return checked;
	}

	public List<Tuple2<String, String>> getContent() {
		return content;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ProofObligationItem.class.getSimpleName() + "[", "]")
				.add("name='" + name + "'")
				.add("description='" + description + "'")
				.add("sourceName='" + sourceName + "'")
				.add("discharged=" + discharged)
				.add("content=" + content)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProofObligationItem that = (ProofObligationItem) o;
		return discharged == that.discharged && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(sourceName, that.sourceName) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, sourceName, discharged, content);
	}
}
