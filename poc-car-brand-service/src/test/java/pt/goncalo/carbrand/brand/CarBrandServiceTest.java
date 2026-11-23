package pt.goncalo.carbrand.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CarBrandServiceTest {

	private final CarBrandService carBrandService = new CarBrandService(new InMemoryCarBrandRepository());

	@Test
	void addedBrandsAreListedByNameAndRemovable() {
		var volvo = carBrandService.addBrand(new CarBrand(" Volvo ", "Sweden"));
		var bmw = carBrandService.addBrand(new CarBrand("BMW", "Germany"));

		assertThat(volvo).isEqualTo(new CarBrand("Volvo", "Sweden"));
		assertThat(carBrandService.listBrands()).containsExactly(bmw, volvo);

		assertThat(carBrandService.removeBrand("bmw")).isEqualTo(bmw);
		assertThat(carBrandService.listBrands()).containsExactly(volvo);
	}

	@Test
	void addingDuplicateNameFails() {
		carBrandService.addBrand(new CarBrand("BMW", "Germany"));

		assertThatThrownBy(() -> carBrandService.addBrand(new CarBrand("bmw", "Germany")))
			.isInstanceOf(CarBrandAlreadyExistsException.class);
	}

	@Test
	void removingUnknownBrandFails() {
		assertThatThrownBy(() -> carBrandService.removeBrand("missing"))
			.isInstanceOf(CarBrandNotFoundException.class);
	}

}
