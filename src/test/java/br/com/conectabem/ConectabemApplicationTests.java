package br.com.conectabem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class ConectabemApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainStartsApplication() {
		assertDoesNotThrow(() -> ConectabemApplication.main(new String[0]));
	}

}
