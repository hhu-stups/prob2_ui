package de.prob2.ui.consoles;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncExecutable {

	CompletableFuture<ConsoleExecResult> exec(String instruction);

}
