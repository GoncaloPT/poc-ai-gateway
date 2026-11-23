package pt.goncalo.carbrand.brand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Repository;

/**
 * In-memory car brand store. POC only: data is lost on restart and not shared between replicas.
 */
@Repository
class InMemoryCarBrandRepository {

	// Keyed by lower-case name: alphabetical listing and case-insensitive uniqueness.
	private final Map<String, CarBrand> brands = new TreeMap<>();

	/**
	 * @param brand brand to store
	 * @return false if a brand with the same name already exists (nothing stored)
	 */
	synchronized boolean saveIfAbsent(CarBrand brand) {
		return brands.putIfAbsent(key(brand.name()), brand) == null;
	}

	/**
	 * @return snapshot of all brands, ordered by name
	 */
	synchronized List<CarBrand> findAll() {
		return new ArrayList<>(brands.values());
	}

	/**
	 * @param name brand name, case-insensitive
	 * @return removed brand, empty if it did not exist
	 */
	synchronized Optional<CarBrand> delete(String name) {
		return Optional.ofNullable(brands.remove(key(name)));
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

}
