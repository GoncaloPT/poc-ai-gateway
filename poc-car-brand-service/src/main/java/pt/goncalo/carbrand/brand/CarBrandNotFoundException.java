package pt.goncalo.carbrand.brand;

/**
 * Thrown when a car brand does not exist.
 */
class CarBrandNotFoundException extends RuntimeException {

	/**
	 * @param name brand name that was not found
	 */
	CarBrandNotFoundException(String name) {
		super("Car brand %s not found".formatted(name));
	}

}
