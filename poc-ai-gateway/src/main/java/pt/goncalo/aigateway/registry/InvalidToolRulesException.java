package pt.goncalo.aigateway.registry;

/**
 * Thrown when a tool update breaks a registry rule, e.g. an enabled write tool without an explicit client grant or an
 * invalid public name.
 */
public class InvalidToolRulesException extends RuntimeException {

	/**
	 * @param message rule that was broken, safe to show to admins
	 */
	public InvalidToolRulesException(String message) {
		super(message);
	}

}
