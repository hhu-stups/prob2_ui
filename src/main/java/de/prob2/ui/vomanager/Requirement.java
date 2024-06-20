package de.prob2.ui.vomanager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.prob2.ui.project.machines.Machine;

public class Requirement {

	private final String name;

	private final String introducedAt; // machine where the requirement is first introduced

	private final String text;

	private final Map<String, ValidationObligation> validationObligationsByMachine;

	private final Set<ValidationObligation> validationObligations;

	private final List<Requirement> previousVersions;

	private final Requirement parent;

	@JsonCreator
	public Requirement(@JsonProperty("name") String name,
			@JsonProperty("introducedAt") String introducedAt,
			@JsonProperty("text") String text,
			@JsonProperty("validationObligations") Set<ValidationObligation> validationObligations
	) {
		this(name, introducedAt, text, validationObligations, Collections.emptyList(), null);
	}

	public Requirement(
		String name,
		String introducedAt,
		String text,
		Set<ValidationObligation> validationObligations,
		List<Requirement> previousVersions,
		Requirement parent
	) {
		this.name = Objects.requireNonNull(name, "name");
		this.introducedAt = Objects.requireNonNull(introducedAt, "introducedAt");
		this.text = Objects.requireNonNull(text, "text");
		this.validationObligationsByMachine = groupVosByMachine(validationObligations);
		// The collection returned by TreeMap.values() doesn't support equals/hashCode properly,
		// so we have to copy it into a Set,
		// which has guaranteed behavior for equals/hashCode.
		// Use LinkedHashSet to maintain the order from the map.
		this.validationObligations = new LinkedHashSet<>(this.validationObligationsByMachine.values());
		assert this.validationObligations.size() == this.validationObligationsByMachine.size();
		this.previousVersions = Objects.requireNonNull(previousVersions, "previousVersions");
		this.parent = parent;
	}

	private static Map<String, ValidationObligation> groupVosByMachine(final Set<ValidationObligation> vos) {
		// Use TreeMap to ensure a stable order (sorted by machine name) when saving to JSON.
		final Map<String, ValidationObligation> vosByMachine = new TreeMap<>();
		for (final ValidationObligation vo : vos) {
			if (vosByMachine.containsKey(vo.getMachine())) {
				throw new IllegalArgumentException("Duplicate validation obligation for machine " + vo.getMachine());
			}
			vosByMachine.put(vo.getMachine(), vo);
		}
		return vosByMachine;
	}

	@JsonIgnore
	public List<Requirement> getPreviousVersions(){
		return previousVersions;
	}

	public String getName() {
		return name;
	}

	public String getIntroducedAt() {
		return introducedAt;
	}

	public String getText() {
		return text;
	}

	@JsonIgnore
	public Map<String, ValidationObligation> getValidationObligationsByMachine() {
		return Collections.unmodifiableMap(this.validationObligationsByMachine);
	}

	public Set<ValidationObligation> getValidationObligations() {
		return Collections.unmodifiableSet(this.validationObligations);
	}

	public Optional<ValidationObligation> getValidationObligation(final String machineName) {
		return Optional.ofNullable(this.getValidationObligationsByMachine().get(machineName));
	}

	public Optional<ValidationObligation> getValidationObligation(final Machine machine) {
		return this.getValidationObligation(machine.getName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Requirement that = (Requirement) o;
		return Objects.equals(name, that.name) && Objects.equals(introducedAt, that.introducedAt) && Objects.equals(text, that.text) && getValidationObligations().equals(that.getValidationObligations());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, introducedAt, text, getValidationObligations());
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "Requirement{name = %s, introducedAt = %s, text = %s, validationObligations = %s}", name, introducedAt, text, getValidationObligations());
	}

	@JsonIgnore //TODO Fix this when making history and refinement saving persistent
	public Requirement getParent(){
		return parent;
	}
}
