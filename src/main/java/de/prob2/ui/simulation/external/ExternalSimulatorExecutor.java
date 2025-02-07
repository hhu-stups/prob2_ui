package de.prob2.ui.simulation.external;

import de.prob.animator.domainobjects.EvalResult;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.simulation.simulators.Simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public final class ExternalSimulatorExecutor {

	private final ObjectMapper objectMapper;
	private final Simulator simulator;
	private final Path pythonFile;
	private final ExecutorService threadService = Executors.newFixedThreadPool(1);
	private final ExecutorService errorThreadService = Executors.newFixedThreadPool(1);

	private ServerSocket serverSocket;
	private Socket clientSocket;
	private Process process;
	private BufferedReader reader;
	private BufferedReader errorReader;
	private BufferedWriter writer;
	private boolean done;
	private FutureTask<Void> startTask;

	public ExternalSimulatorExecutor(ObjectMapper objectMapper, Simulator simulator, Path pythonFile) {
		this.objectMapper = objectMapper;
		this.objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
		this.simulator = simulator;
		this.pythonFile = pythonFile;
		this.done = false;
	}

	public void reset() {
		this.done = false;
	}

	public void start() {
		try {
			this.serverSocket = new ServerSocket(0);
			int simBPort = serverSocket.getLocalPort();
			System.out.println("SimB Port: " + simBPort);
			startTask = new FutureTask<>(() -> {
				clientSocket = serverSocket.accept();
				this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				this.writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				return null;
			});

			threadService.execute(startTask);
			ProcessBuilder pb = new ProcessBuilder("python3", pythonFile.getFileName().toString(), String.valueOf(simBPort)).directory(pythonFile.getParent().toFile());
			this.process = pb.start();
			this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			processErrorMessages();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processErrorMessages() {
		FutureTask<Void> task = new FutureTask<>(() -> {
			StringBuilder stringBuilder = new StringBuilder();
			try {
				String line;
				while ((line = errorReader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			String message = stringBuilder.toString();
			if(!message.isEmpty()) {
				Platform.runLater(() -> {
					throw new ExternalSimulationException("Error in External Simulation: " + message);
				});
			}
			return null;
		});
		errorThreadService.execute(task);
	}

	public ExternalSimulationStep execute(Trace trace) {
		try {
			startTask.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		ExternalSimulationStep step = null;
		try {
			processErrorMessages();
			if(simulator.endingConditionReached(trace)) {
				sendFinish();
				return null;
			}

			State state = trace.getCurrentState();

			String enabledOperations = String.join(",", state.getStateSpace().getLoadedMachine().getOperationNames()
					.stream()
					.filter(op -> state.eval(String.format("GET_GUARD_STATUS(\"%s\")", op)) instanceof EvalResult res && "TRUE".equals(res.getValue()))
					.collect(Collectors.toSet()));
			sendContinue(enabledOperations);

			step = this.objectMapper.readValue(reader, ExternalSimulationStep.class);
		} catch(Exception e){
			e.printStackTrace();
		}
		if(step != null) {
			setDone(step.isDone());
		}
		return step;
	}

	public void close() {
		try {
			if(clientSocket != null && !clientSocket.isClosed()) {
				writer.close();
				writer = null;
				reader.close();
				reader = null;
				clientSocket.close();
				clientSocket = null;
			}
			process.getOutputStream().close();
			serverSocket.close();
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
			this.objectMapper.writeValue(writer, new ExternalSimulationRequest(1, ""));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendContinue(String enabledOperations) {
		try {
			this.objectMapper.writeValue(writer, new ExternalSimulationRequest(0, enabledOperations));
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
