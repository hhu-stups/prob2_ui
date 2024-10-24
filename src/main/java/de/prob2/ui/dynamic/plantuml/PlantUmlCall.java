package de.prob2.ui.dynamic.plantuml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import de.prob.exception.ProBError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlantUmlCall {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlantUmlCall.class);

	public static final String PNG = "png";
	public static final String SVG = "svg";

	private final String javaCommand;
	private final Path plantUmlJar;
	private byte[] input;
	private String outputFormat;
	private String dotExecutable;

	public PlantUmlCall(String javaCommand, Path plantUmlJar) {
		this.javaCommand = Objects.requireNonNull(javaCommand, "javaCommand");
		this.plantUmlJar = Objects.requireNonNull(plantUmlJar, "plantUmlJar");
	}

	public PlantUmlCall input(byte[] input) {
		if (this.input != null) {
			throw new IllegalStateException("input");
		}
		this.input = Objects.requireNonNull(input, "input");
		return this;
	}

	public PlantUmlCall input(String input) {
		return this.input(input.getBytes(StandardCharsets.UTF_8));
	}

	public PlantUmlCall outputFormat(String outputFormat) {
		if (this.outputFormat != null) {
			throw new IllegalStateException("outputFormat");
		}
		this.outputFormat = Objects.requireNonNull(outputFormat, "outputFormat");
		return this;
	}

	public PlantUmlCall dotExecutable(String dotExecutable) {
		if (this.dotExecutable != null) {
			throw new IllegalStateException("dotExecutable");
		}
		this.dotExecutable = Objects.requireNonNull(dotExecutable, "dotExecutable");
		return this;
	}

	public RunnableFuture<byte[]> getRunnableFuture() {
		if (this.input == null) {
			throw new IllegalStateException("input");
		}

		List<String> command = new ArrayList<>(Arrays.asList(this.javaCommand, "-jar", this.plantUmlJar.toString()));
		command.add("-p"); // -pipe: IO via stdin/stdout
		if (this.outputFormat != null) {
			command.add("-t" + this.outputFormat);
		}
		if (this.dotExecutable != null) {
			command.add("-graphvizdot");
			command.add(this.dotExecutable);
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		return new FutureTask<>(() -> {
			Process p = pb.start();

			Thread stdinWriter = new Thread(() -> {
				try (OutputStream os = p.getOutputStream()) {
					os.write(this.input);
				} catch (Throwable t) {
					LOGGER.warn("could not write input stream of plantuml call", t);
				}
			});
			stdinWriter.setDaemon(true);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Thread stdoutReader = new Thread(() -> {
				try (InputStream is = p.getInputStream()) {
					is.transferTo(outputStream);
				} catch (Throwable t) {
					LOGGER.warn("could not read output stream of plantuml call", t);
				}
			});
			stdoutReader.setDaemon(true);

			StringWriter errorStream = new StringWriter();
			Thread stderrReader = new Thread(() -> {
				try (BufferedReader r = p.errorReader()) {
					r.transferTo(errorStream);
				} catch (Throwable t) {
					LOGGER.warn("could not read error stream of plantuml call", t);
				}
			});
			stderrReader.setDaemon(true);

			stdoutReader.start();
			stderrReader.start();
			stdinWriter.start();

			int exit;
			try {
				exit = p.waitFor();
			} catch (InterruptedException e) {
				stdinWriter.interrupt();
				stdoutReader.interrupt();
				stderrReader.interrupt();
				p.destroy();
				throw e;
			}

			if (exit != 0) {
				stdinWriter.interrupt();
				stdoutReader.interrupt();
				stderrReader.join();
				throw new RuntimeException("error while calling plantuml: " + errorStream);
			}

			stdinWriter.interrupt();
			stderrReader.interrupt();
			stdoutReader.join();
			return outputStream.toByteArray();
		});
	}

	public byte[] call() throws InterruptedException {
		RunnableFuture<byte[]> future = this.getRunnableFuture();
		future.run();

		try {
			return future.get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof InterruptedException) {
				throw (InterruptedException) e.getCause();
			} else {
				throw new ProBError(e.getCause());
			}
		}
	}
}
