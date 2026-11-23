package pt.goncalo.mcptool.task;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP adapter for tasks. Thin layer next to the REST controller: it only maps tool parameters and delegates to
 * {@link TaskService}. Tools are discovered automatically by the Spring AI MCP annotation scanner.
 * <p>
 * These tools are internal. Whether an external AI client can see them is decided by the AI gateway.
 */
@Component
@RequiredArgsConstructor
class TaskMcpTools {

	private final TaskService taskService;

	/**
	 * Creates a task on behalf of the calling user.
	 */
	@McpTool(name = "create_task", description = "Create a task. Returns the created task with its id.",
			annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false))
	public TaskResponse createTask(@McpToolParam(description = "Short title of the task") String title,
			@McpToolParam(description = "Details of the task") String description,
			@McpToolParam(description = "Priority: LOW, MEDIUM or HIGH") CreateTaskCommand.Priority priority) {
		return taskService.createTask(new CreateTaskCommand(title, description, priority));
	}

	/**
	 * Lists all tasks.
	 */
	@McpTool(name = "list_tasks", description = "List all tasks with id, title, description and priority.",
			annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
					openWorldHint = false))
	public List<TaskResponse> listTasks() {
		return taskService.listTasks();
	}

	/**
	 * Deletes a task. Intentionally not exposed by the AI gateway registry (deny by default demo).
	 */
	@McpTool(name = "delete_task", description = "Delete a task by id. Returns the deleted task.",
			annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true))
	public TaskResponse deleteTask(@McpToolParam(description = "Id of the task to delete") String taskId) {
		return taskService.deleteTask(taskId);
	}

}
