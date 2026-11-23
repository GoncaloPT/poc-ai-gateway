package pt.goncalo.mcptool.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TaskServiceTest {

	private final TaskService taskService = new TaskService(new InMemoryTaskRepository());

	@Test
	void createdTasksAreListedAndDeletable() {
		var first = taskService.createTask(new CreateTaskCommand("a", "first", CreateTaskCommand.Priority.LOW));
		var second = taskService.createTask(new CreateTaskCommand("b", "second", CreateTaskCommand.Priority.HIGH));

		assertThat(taskService.listTasks()).containsExactly(first, second);

		assertThat(taskService.deleteTask(first.id())).isEqualTo(first);
		assertThat(taskService.listTasks()).containsExactly(second);
	}

	@Test
	void deletingUnknownTaskFails() {
		assertThatThrownBy(() -> taskService.deleteTask("missing")).isInstanceOf(TaskNotFoundException.class);
	}

}
