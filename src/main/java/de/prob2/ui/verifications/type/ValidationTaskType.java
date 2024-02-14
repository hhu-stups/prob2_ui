package de.prob2.ui.verifications.type;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import de.prob2.ui.vomanager.IValidationTask;

public class ValidationTaskType {

	private static final Map<String, ValidationTaskType> REGISTRY = new HashMap<>();

	public static synchronized <T extends ValidationTaskType> T register(T taskType) {
		Objects.requireNonNull(taskType, "taskType");
		if (REGISTRY.putIfAbsent(taskType.getKey(), taskType) != null) {
			throw new IllegalArgumentException("task type already registered: " + taskType.getKey());
		}
		return taskType;
	}

	public static synchronized ValidationTaskType get(String key) {
		Objects.requireNonNull(key, "key");
		ValidationTaskType taskType = REGISTRY.get(key);
		if (taskType == null) {
			throw new NoSuchElementException("task type not found: " + key);
		}
		return taskType;
	}

	private final String key;
	private final Class<? extends IValidationTask> taskClass;

	public ValidationTaskType(String key, Class<? extends IValidationTask> taskClass) {
		this.key = Objects.requireNonNull(key, "key");
		this.taskClass = Objects.requireNonNull(taskClass, "taskClass");
	}

	public String getKey() {
		return this.key;
	}

	public Class<? extends IValidationTask> getTaskClass() {
		return taskClass;
	}
}
