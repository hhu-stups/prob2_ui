package de.prob2.ui.simulation.external;

import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.simulators.Simulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class ExternalSimulatorExecutor {

	private final Simulator simulator;

	private final ProcessBuilder pb;

	private final Path pythonFile;

	private Process process;

	private BufferedReader reader;

	private BufferedWriter writer;

	private final ExecutorService threadService = Executors.newFixedThreadPool(1);

	private boolean done;

	public ExternalSimulatorExecutor(Simulator simulator, Path pythonFile) {
		this.simulator = simulator;
		this.pythonFile = pythonFile;
		this.pb = new ProcessBuilder("python3", pythonFile.getFileName().toString()).directory(pythonFile.getParent().toFile());
		this.done = false;
	}

	public synchronized void reset() {
		this.done = false;
	}

	public void start() {
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
				if(simulator.endingConditionReached(trace)) {
					sendFinish();
					return null;
				} else {
					sendContinue();
				}

				State state = trace.getCurrentState();
				String enabledOperations = state
						.getTransitions().stream()
						.map(Transition::getName)
						.collect(Collectors.joining(","));

				writer.write(enabledOperations);
				writer.newLine();
				writer.flush();

				int j = 0;

				while (j <= 3) {
					String line = reader.readLine();
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

	public void close() {
		try {
			reader.close();
			writer.close();
			process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDone() {
		return done;
	}

	public void sendFinish() {
		if(done) {
			return;
		}
		setDone(true);
		try {
			writer.write(String.valueOf(1));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendContinue() {
		try {
			writer.write(String.valueOf(0));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Path getPythonFile() {
		return pythonFile;
	}
}
