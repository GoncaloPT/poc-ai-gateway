package pt.goncalo.carbrand.brand;

/**
 * Thrown when adding a car brand whose name is already in the list.
 */
class CarBrandAlreadyExistsException extends RuntimeException {

	/**
	 * @param name brand name that already exists
	 */
	CarBrandAlreadyExistsException(String name) {
		super("Car brand %s already exists".formatted(name));
	}

}
