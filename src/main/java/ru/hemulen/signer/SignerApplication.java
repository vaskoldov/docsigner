package ru.hemulen.signer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.hemulen.signer.thread.FileProcessor;

@SpringBootApplication
public class SignerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignerApplication.class, args);
	}

}
