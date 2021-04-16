package de.prob2.ui.internal;

import javafx.scene.web.WebEngine;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JavascriptFunctionInvoker {

	public static void invokeFunction(WebEngine engine, String function, Object... params) {
		engine.executeScript(buildInvocation(function, params));
	}

	public static String buildInvocation(String function, Object... params) {
		String parametersAsString = Arrays.stream(params).map(Object::toString).collect(Collectors.joining(", "));
		return function + "(" + parametersAsString + ");";
	}

	public static String wrapAsString(String str) {
		return "\"" + str + "\"";
	}

}
