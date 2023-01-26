package de.prob2.ui.consoles;

public class ConsoleExecResult {
    private final String consoleOutput;
    private final String result;
    private final ConsoleExecResultType resultType;

    public ConsoleExecResult(String consoleOutput, String result, ConsoleExecResultType resultType) {
        this.consoleOutput = consoleOutput;
        this.result = result;
        this.resultType = resultType;
    }

    public String getConsoleOutput() {
        return this.consoleOutput;
    }

    public String getResult() {
        return this.result;
    }

    public ConsoleExecResultType getResultType() {
        return resultType;
    }

    @Override
    public String toString() {
        return consoleOutput + result;
    }
}
