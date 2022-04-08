package de.prob2.ui.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public final class MultiKeyCombination extends KeyCombination {

	private final KeyCombination representation;
	private final Set<KeyCombination> combinations;

	public MultiKeyCombination(KeyCombination representation, KeyCombination... combinations) {
		this(representation, Arrays.asList(combinations));
	}

	public MultiKeyCombination(KeyCombination representation, Collection<? extends KeyCombination> combinations) {
		super(representation.getShift(), representation.getControl(), representation.getAlt(), representation.getMeta(), representation.getShortcut());
		this.representation = representation;
		this.combinations = new HashSet<>(combinations);
		this.combinations.add(representation);
	}

	@Override
	public boolean match(KeyEvent event) {
		return combinations.stream().anyMatch(kc -> kc.match(event));
	}

	@Override
	public String getName() {
		return representation.getName();
	}

	@Override
	public String getDisplayText() {
		return representation.getDisplayText();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof MultiKeyCombination)) {
			return false;
		}

		MultiKeyCombination that = (MultiKeyCombination) o;
		return combinations.equals(that.combinations);
	}

	@Override
	public int hashCode() {
		return combinations.hashCode();
	}
}
