package pt.goncalo.mcptool.task;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for tasks (traffic from the API gateway / frontends).
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
class TaskController {

	private final TaskService taskService;

	/**
	 * Creates a task.
	 *
	 * @param command task data
	 * @return created task
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TaskResponse createTask(@Valid @RequestBody CreateTaskCommand command) {
		return taskService.createTask(command);
	}

	/**
	 * @return all tasks
	 */
	@GetMapping
	List<TaskResponse> listTasks() {
		return taskService.listTasks();
	}

	/**
	 * Deletes a task.
	 *
	 * @param taskId task id
	 */
	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteTask(@PathVariable String taskId) {
		taskService.deleteTask(taskId);
	}

	@ExceptionHandler(TaskNotFoundException.class)
	ProblemDetail handleNotFound(TaskNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

}
