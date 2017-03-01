package de.prob2.ui.states;

import java.util.Objects;

public class StateItem<T> {
	private final T contents;
	private final boolean errored;
	
	public StateItem(final T contents, final boolean errored) {
		super();
		
		Objects.requireNonNull(contents);
		
		this.contents = contents;
		this.errored = errored;
	}
	
	public T getContents() {
		return this.contents;
	}
	
	public boolean isErrored() {
		return this.errored;
	}
}
