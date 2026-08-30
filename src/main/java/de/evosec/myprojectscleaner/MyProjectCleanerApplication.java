package de.evosec.myprojectscleaner;

import java.net.ProxySelector;
import java.nio.file.Path;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.github.markusbernhardt.proxy.ProxySearch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableConfigurationProperties(MyProjectCleanerProperties.class)
public class MyProjectCleanerApplication implements CommandLineRunner {

	private final MyProjectCleanerProperties properties;

	@SuppressFBWarnings(
			value = "EI_EXPOSE_REP2",
			justification = "Spring-managed configuration properties bean"
	)
	public MyProjectCleanerApplication(MyProjectCleanerProperties properties) {
		this.properties = properties;
	}

	public static void main(String[] args) {
		SpringApplication.run(MyProjectCleanerApplication.class, args);
	}

	@PostConstruct
	public void setProxySelector() {
		ProxySelector.setDefault(
				ProxySearch.getDefaultProxySearch().getProxySelector()
		);
	}

	@Override
	public void run(String... args) throws Exception {
		Path workingDirectory = Path.of(System.getProperty("user.dir", "."));
		new MyProjectCleaner(
				workingDirectory,
				properties.getEclipseVersionToKeep()
		).clean();
	}

}
