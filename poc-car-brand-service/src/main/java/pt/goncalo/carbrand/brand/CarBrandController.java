package pt.goncalo.carbrand.brand;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for car brands (traffic from the API gateway / frontends).
 */
@RestController
@RequestMapping("/api/car-brands")
@RequiredArgsConstructor
class CarBrandController {

	private final CarBrandService carBrandService;

	/**
	 * Adds a brand.
	 *
	 * @param brand brand data
	 * @return added brand
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CarBrand addBrand(@Valid @RequestBody CarBrand brand) {
		return carBrandService.addBrand(brand);
	}

	/**
	 * @return all brands, ordered by name
	 */
	@GetMapping
	List<CarBrand> listBrands() {
		return carBrandService.listBrands();
	}

	/**
	 * Removes a brand.
	 *
	 * @param name brand name, case-insensitive
	 */
	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeBrand(@PathVariable String name) {
		carBrandService.removeBrand(name);
	}

	@ExceptionHandler(CarBrandNotFoundException.class)
	ProblemDetail handleNotFound(CarBrandNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CarBrandAlreadyExistsException.class)
	ProblemDetail handleConflict(CarBrandAlreadyExistsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

}
