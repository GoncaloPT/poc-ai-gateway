package pt.goncalo.mcptool.task;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single business entry point for tasks. Both the REST controller and the MCP tools delegate here,
 * so REST and AI callers share the same logic and side effects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class TaskService {

	private final InMemoryTaskRepository taskRepository;

	/**
	 * Creates and stores a task.
	 *
	 * @param command task data
	 * @return created task with generated id
	 */
	TaskResponse createTask(CreateTaskCommand command) {
		var task = new TaskResponse(UUID.randomUUID().toString(), command.title(), command.description(),
				command.priority());
		taskRepository.save(task);
		log.info("Task created: {}", task);
		return task;
	}

	/**
	 * @return all tasks in creation order
	 */
	List<TaskResponse> listTasks() {
		var tasks = taskRepository.findAll();
		log.info("Tasks listed: {}", tasks.size());
		return tasks;
	}

	/**
	 * Deletes a task.
	 *
	 * @param taskId id of the task
	 * @return deleted task
	 * @throws TaskNotFoundException if the task does not exist
	 */
	TaskResponse deleteTask(String taskId) {
		var task = taskRepository.delete(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
		log.info("Task deleted: {}", task);
		return task;
	}

}
