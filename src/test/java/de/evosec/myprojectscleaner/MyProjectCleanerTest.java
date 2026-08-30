package de.evosec.myprojectscleaner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MyProjectCleanerTest {

	private static final String VERSION_TO_KEEP = "4.35";

	private static final List<String> WORKSPACE_STATE_DIRS = List.of(
			".metadata",
			".recommenders",
			"Servers",
			"RemoteSystemsTempFiles"
	);

	@TempDir
	Path root;

	@Test
	void deletesOutdatedEclipseAndWorkspaceState() throws Exception {
		Path ide = createIde("stale", "4.30");
		Path workspace = ide.resolve("workspace");
		Path project = Files.createDirectories(workspace.resolve("myproject"));
		Files.writeString(project.resolve("pom.xml"), "<project/>");

		new MyProjectCleaner(root, VERSION_TO_KEEP).clean();

		assertThat(ide.resolve("eclipse")).doesNotExist();
		for (String stateDir : WORKSPACE_STATE_DIRS) {
			assertThat(workspace.resolve(stateDir)).doesNotExist();
		}
		assertThat(project.resolve("pom.xml")).exists();
	}

	@Test
	void keepsCurrentEclipseAndWorkspaceState() throws Exception {
		Path ide = createIde("current", VERSION_TO_KEEP);
		Path workspace = ide.resolve("workspace");

		new MyProjectCleaner(root, VERSION_TO_KEEP).clean();

		assertThat(ide.resolve("eclipse").resolve(".eclipseproduct")).exists();
		for (String stateDir : WORKSPACE_STATE_DIRS) {
			assertThat(workspace.resolve(stateDir).resolve("state.txt"))
					.exists();
		}
	}

	@Test
	void deletesEclipseForWorkspaceWithoutMetadata() throws Exception {
		Path ide = createIde("no-metadata", VERSION_TO_KEEP);
		Path workspace = ide.resolve("workspace");
		deleteRecursively(workspace.resolve(".metadata"));

		new MyProjectCleaner(root, VERSION_TO_KEEP).clean();

		assertThat(ide.resolve("eclipse")).doesNotExist();
		assertThat(workspace.resolve("Servers")).doesNotExist();
	}

	@Test
	void cleansIgnoredFilesFromCleanRepositoryWithoutEclipse()
			throws Exception {
		Path ide = createIde("with-repository", "4.30");
		Path project = Files.createDirectories(
				ide.resolve("workspace").resolve("myproject")
		);
		try (Git git = Git.init().setDirectory(project.toFile()).call()) {
			Path info = Files.createDirectories(
					git.getRepository().getDirectory().toPath().resolve("info")
			);
			Files.writeString(info.resolve("exclude"), "build/\n");
		}
		Path ignored = Files.createDirectories(project.resolve("build"));
		Files.writeString(ignored.resolve("output.txt"), "compiled");

		new MyProjectCleaner(root, VERSION_TO_KEEP).clean();

		assertThat(ide.resolve("eclipse")).doesNotExist();
		assertThat(ignored).doesNotExist();
		assertThat(project.resolve(".git")).isDirectory();
	}

	private Path createIde(String name, String eclipseVersion)
			throws IOException {
		Path ide = Files.createDirectories(root.resolve(name));
		Path eclipse = Files.createDirectories(ide.resolve("eclipse"));
		Files.writeString(
				eclipse.resolve(".eclipseproduct"),
				"name=Eclipse Platform\nversion=" + eclipseVersion + "\n"
		);
		Path plugins = Files.createDirectories(eclipse.resolve("plugins"));
		Files.writeString(plugins.resolve("plugin.jar"), "binary");
		Path workspace = Files.createDirectories(ide.resolve("workspace"));
		for (String stateDir : WORKSPACE_STATE_DIRS) {
			Path directory = Files
					.createDirectories(workspace.resolve(stateDir));
			Files.writeString(directory.resolve("state.txt"), "state");
		}
		return ide;
	}

	private void deleteRecursively(Path directory) throws IOException {
		try (var paths = Files.walk(directory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

}
