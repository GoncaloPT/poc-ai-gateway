package pt.goncalo.mcptool.task;

/**
 * Task as returned to REST and MCP callers.
 *
 * @param id generated task id
 * @param title short title
 * @param description details
 * @param priority priority of the task
 */
public record TaskResponse(String id, String title, String description, CreateTaskCommand.Priority priority) {
}
