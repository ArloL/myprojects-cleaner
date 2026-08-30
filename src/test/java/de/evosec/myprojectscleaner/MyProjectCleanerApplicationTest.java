package de.evosec.myprojectscleaner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MyProjectCleanerApplicationTest {

	@Test
	void keepsTheConfigurationPropertiesItWasConstructedWith() {
		MyProjectCleanerProperties properties = new MyProjectCleanerProperties();
		properties.setEclipseVersionToKeep("4.35");

		MyProjectCleanerApplication application = new MyProjectCleanerApplication(
				properties
		);

		assertThat(application).extracting("properties").isSameAs(properties);
	}

}
