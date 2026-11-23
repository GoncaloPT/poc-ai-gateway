package pt.goncalo.carbrand.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CarBrandServiceTest {

	private final CarBrandService carBrandService = new CarBrandService(new InMemoryCarBrandRepository());

	@Test
	void addedBrandsAreListedByNameAndRemovable() {
		var volvo = carBrandService.addBrand(new CarBrand(" Volvo ", "Sweden"));
		var toyota = carBrandService.addBrand(new CarBrand("Toyota", "Japan"));

		assertThat(volvo).isEqualTo(new CarBrand("Volvo", "Sweden"));
		assertThat(carBrandService.listBrands()).containsExactly(toyota, volvo);

		assertThat(carBrandService.removeBrand("toyota")).isEqualTo(toyota);
		assertThat(carBrandService.listBrands()).containsExactly(volvo);
	}

	@Test
	void addingDuplicateNameFails() {
		carBrandService.addBrand(new CarBrand("Toyota", "Japan"));

		assertThatThrownBy(() -> carBrandService.addBrand(new CarBrand("toyota", "Japan")))
			.isInstanceOf(CarBrandAlreadyExistsException.class);
	}

	@Test
	void removingUnknownBrandFails() {
		assertThatThrownBy(() -> carBrandService.removeBrand("missing"))
			.isInstanceOf(CarBrandNotFoundException.class);
	}

}
