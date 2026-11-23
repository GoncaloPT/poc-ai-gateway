package pt.goncalo.mcptool.task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/**
 * In-memory task store. POC only: data is lost on restart and not shared between replicas.
 */
@Repository
class InMemoryTaskRepository {

	// Insertion order keeps list results stable; access is synchronized because servlet threads run concurrently.
	private final Map<String, TaskResponse> tasks = new LinkedHashMap<>();

	/**
	 * @param task task to store, replaces an existing task with the same id
	 */
	synchronized void save(TaskResponse task) {
		tasks.put(task.id(), task);
	}

	/**
	 * @return snapshot of all tasks in creation order
	 */
	synchronized List<TaskResponse> findAll() {
		return new ArrayList<>(tasks.values());
	}

	/**
	 * @param taskId task id
	 * @return removed task, empty if it did not exist
	 */
	synchronized Optional<TaskResponse> delete(String taskId) {
		return Optional.ofNullable(tasks.remove(taskId));
	}

}
