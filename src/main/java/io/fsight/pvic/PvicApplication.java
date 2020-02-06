package io.fsight.pvic;

import io.fsight.pvic.service.InformationCollectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PvicApplication implements CommandLineRunner {

	@Autowired
	private InformationCollectorService informationCollector;

	public static void main(String[] args) {
		SpringApplication.run(PvicApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		informationCollector.runScraping();
	}
}
