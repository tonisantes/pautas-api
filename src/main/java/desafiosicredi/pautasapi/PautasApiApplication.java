package desafiosicredi.pautasapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
// @EnableTransactionManagement
public class PautasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PautasApiApplication.class, args);
	}

}
