package pt.goncalo.aigateway.registry;

/**
 * Thrown when a public tool name is already used by another tool.
 */
public class PublicNameTakenException extends RuntimeException {

	/**
	 * @param publicName name that is already taken
	 * @param ownerId registry id of the tool that owns the name
	 */
	public PublicNameTakenException(String publicName, String ownerId) {
		super("Public name '%s' is already used by %s".formatted(publicName, ownerId));
	}

}
