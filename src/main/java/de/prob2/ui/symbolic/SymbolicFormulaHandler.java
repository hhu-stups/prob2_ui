package de.prob2.ui.symbolic;

import java.util.concurrent.CompletableFuture;

public interface SymbolicFormulaHandler<T extends SymbolicItem<?>> {
	public CompletableFuture<T> handleItem(T item, boolean checkAll);
}
