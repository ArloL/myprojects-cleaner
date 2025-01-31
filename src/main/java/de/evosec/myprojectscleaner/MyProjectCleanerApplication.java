package de.evosec.myprojectscleaner;

import java.net.ProxySelector;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.github.markusbernhardt.proxy.ProxySearch;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableConfigurationProperties(MyProjectCleanerProperties.class)
public class MyProjectCleanerApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(MyProjectCleanerApplication.class, args);
	}

	@PostConstruct
	public void setProxySelector() {
		ProxySelector.setDefault(
				ProxySearch.getDefaultProxySearch().getProxySelector()
		);
	}

	@Autowired
	MyProjectCleanerProperties properties;

	@Override
	public void run(String... args) throws Exception {
		Path workingDirectory = Path.of(System.getProperty("user.dir", "."));
		new MyProjectCleaner(
				workingDirectory,
				properties.getEclipseVersionToKeep()
		).clean();
	}

}
