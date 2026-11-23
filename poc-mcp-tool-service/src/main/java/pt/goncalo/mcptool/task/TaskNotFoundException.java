package pt.goncalo.mcptool.task;

/**
 * Thrown when a task id does not exist.
 */
class TaskNotFoundException extends RuntimeException {

	/**
	 * @param taskId id that was not found
	 */
	TaskNotFoundException(String taskId) {
		super("Task %s not found".formatted(taskId));
	}

}
