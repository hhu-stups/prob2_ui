package de.prob2.ui.simulation.external;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.statespace.State;
import de.prob.statespace.Trace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ExternalSimulatorExecutor {

	private final ProcessBuilder pb;

	private final Path pythonFile;

	private Process process;

	private BufferedReader reader;

	private BufferedWriter writer;

	private final ExecutorService threadService = Executors.newFixedThreadPool(1);

	private boolean done;

	private boolean started;

	private boolean hasShield;

	public ExternalSimulatorExecutor(Path pythonFile, ClassicalBModel model) {
		this.pythonFile = pythonFile;
		this.pb = new ProcessBuilder("python3", pythonFile.toString()).directory(pythonFile.getParent().toFile());
		this.done = false;
		this.started = false;
		this.hasShield = model.getDefinitions().getDefinitionNames().contains("SHIELD_INTERVENTION");
	}

	public void reset() {
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
				State state = trace.getCurrentState();

				writer.write("step");
				writer.newLine();
				writer.flush();
				String line = reader.readLine();

				String intervention = hasShield && state.isInitialised() ? state.eval(String.format("SHIELD_INTERVENTION(%s)", line)).toString() : line;
				writer.write(intervention);
				writer.newLine();
				writer.flush();

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

	public Path getPythonFile() {
		return pythonFile;
	}
}
