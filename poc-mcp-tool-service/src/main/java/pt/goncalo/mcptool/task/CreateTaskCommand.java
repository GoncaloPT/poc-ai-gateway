package pt.goncalo.mcptool.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input to create a task, shared by REST and MCP.
 *
 * @param title short title
 * @param description details
 * @param priority priority of the task
 */
public record CreateTaskCommand(@NotBlank String title, @NotBlank String description, @NotNull Priority priority) {

	/** Task priority. */
	public enum Priority {

		LOW, MEDIUM, HIGH

	}

}
