package nz.ac.auckland.cer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.transaction.Transactional;

@SpringBootApplication
public class CerApiApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(CerApiApplication.class, args);
    }

    @Override
    @Transactional
    public void run(String... strings) throws Exception {

    }
}
