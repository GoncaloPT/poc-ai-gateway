package pt.goncalo.carbrand.brand;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single business entry point for car brands. Both the REST controller and the MCP tools delegate here,
 * so REST and AI callers share the same logic and side effects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class CarBrandService {

	private final InMemoryCarBrandRepository carBrandRepository;

	/**
	 * Adds a brand to the list.
	 *
	 * @param brand brand to add
	 * @return added brand, with trimmed values
	 * @throws CarBrandAlreadyExistsException if a brand with the same name exists (case-insensitive)
	 */
	CarBrand addBrand(CarBrand brand) {
		var normalized = new CarBrand(brand.name().trim(), brand.country().trim());
		if (!carBrandRepository.saveIfAbsent(normalized)) {
			throw new CarBrandAlreadyExistsException(normalized.name());
		}
		log.info("Car brand added: {}", normalized);
		return normalized;
	}

	/**
	 * @return all brands, ordered by name
	 */
	List<CarBrand> listBrands() {
		var brands = carBrandRepository.findAll();
		log.info("Car brands listed: {}", brands.size());
		return brands;
	}

	/**
	 * Removes a brand from the list.
	 *
	 * @param name brand name, case-insensitive
	 * @return removed brand
	 * @throws CarBrandNotFoundException if the brand does not exist
	 */
	CarBrand removeBrand(String name) {
		var brand = carBrandRepository.delete(name).orElseThrow(() -> new CarBrandNotFoundException(name));
		log.info("Car brand removed: {}", brand);
		return brand;
	}

}
