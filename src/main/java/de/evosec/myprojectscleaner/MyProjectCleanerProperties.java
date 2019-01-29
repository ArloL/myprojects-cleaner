package de.evosec.myprojectscleaner;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("myprojectscleaner")
public class MyProjectCleanerProperties {

	private String eclipseVersionToKeep;

	public String getEclipseVersionToKeep() {
		return eclipseVersionToKeep;
	}

	public void setEclipseVersionToKeep(String eclipseVersionToKeep) {
		this.eclipseVersionToKeep = eclipseVersionToKeep;
	}

}
