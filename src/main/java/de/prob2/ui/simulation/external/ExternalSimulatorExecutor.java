package de.prob2.ui.simulation.external;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class ExternalSimulatorExecutor {

	private final ProcessBuilder pb;

	private Process process;

	private final CurrentTrace currentTrace;

	private BufferedReader reader;

	private BufferedWriter writer;

	private final ExecutorService threadService = Executors.newFixedThreadPool(1);

	private boolean done;

	private boolean started;

	public ExternalSimulatorExecutor(Path pythonFile, CurrentTrace currentTrace) {
		this.pb = new ProcessBuilder("python3", pythonFile.toString()).directory(pythonFile.getParent().toFile());
		this.currentTrace = currentTrace;
		this.done = false;
		this.started = false;
	}

	public void start() {
		started = true;
		pb.redirectErrorStream(true);
		try {
			this.process = pb.start();
			this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FutureTask<ExternalSimulationStep> execute(Trace trace) {
		FutureTask<ExternalSimulationStep> task = new FutureTask<>(() -> {
			String operation = "";
			String delta = "";
			String predicate = "";
			boolean done = false;
			try {
				List<String> operations = trace.getCurrentState()
						.getTransitions().stream()
						.map(Transition::getName)
						.collect(Collectors.toList());
				String message = String.join(",", operations);
				writer.write(message);
				writer.newLine();
				writer.flush();
				String line;
				int j = 0;


				while (j <= 3) {
					line = reader.readLine();
					if (j == 0) {
						operation = line;
					} else if (j == 1) {
						delta = line;
					} else if (j == 2) {
						predicate = line;
					} else if (j == 3) {
						done = Boolean.parseBoolean(line);
					}
					j++;
				}

			} catch(Exception e){
				e.printStackTrace();
			}
			setDone(done);
			return new ExternalSimulationStep(operation, predicate, delta, done);
		});
		threadService.execute(task);
		return task;
	}

	public void close() throws IOException {
		reader.close();
		writer.close();
		process.getOutputStream().close();
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isStarted() {
		return started;
	}
}
