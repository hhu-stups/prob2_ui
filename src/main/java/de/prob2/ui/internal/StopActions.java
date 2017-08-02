package de.prob2.ui.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class StopActions {
	private final Deque<Runnable> actions;
	
	@Inject
	private StopActions() {
		super();
		
		this.actions = new ArrayDeque<>();
	}
	
	public void add(final Runnable action) {
		this.actions.addFirst(action);
	}
	
	public void run() {
		this.actions.forEach(Runnable::run);
	}
}
