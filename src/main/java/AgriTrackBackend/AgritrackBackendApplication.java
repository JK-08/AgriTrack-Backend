package AgriTrackBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling

@SpringBootApplication
public class AgritrackBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgritrackBackendApplication.class, args);
	}

}
