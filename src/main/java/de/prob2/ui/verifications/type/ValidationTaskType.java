package de.prob2.ui.verifications.type;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import de.prob2.ui.verifications.IValidationTask;

public class ValidationTaskType<T extends IValidationTask> implements Comparable<ValidationTaskType<?>> {
	private static final Map<String, ValidationTaskType<?>> REGISTRY = new HashMap<>();

	public static synchronized <T extends IValidationTask> ValidationTaskType<T> register(ValidationTaskType<T> taskType) {
		Objects.requireNonNull(taskType, "taskType");
		if (REGISTRY.putIfAbsent(taskType.getKey(), taskType) != null) {
			throw new IllegalArgumentException("task type already registered: " + taskType.getKey());
		}
		return taskType;
	}

	public static synchronized ValidationTaskType<?> get(String key) {
		Objects.requireNonNull(key, "key");
		ValidationTaskType<?> taskType = REGISTRY.get(key);
		if (taskType == null) {
			throw new NoSuchElementException("task type not found: " + key);
		}
		return taskType;
	}

	private final String key;
	private final Class<T> taskClass;

	public ValidationTaskType(String key, Class<T> taskClass) {
		this.key = Objects.requireNonNull(key, "key");
		this.taskClass = Objects.requireNonNull(taskClass, "taskClass");
	}

	public String getKey() {
		return this.key;
	}

	public Class<T> getTaskClass() {
		return this.taskClass;
	}

	@Override
	public int compareTo(ValidationTaskType<?> o) {
		return this.getKey().compareTo(o.getKey());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof ValidationTaskType<?> that)) {
			return false;
		} else {
			return Objects.equals(this.getKey(), that.getKey());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getKey());
	}
}
