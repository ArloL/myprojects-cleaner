package de.evosec.myprojectscleaner;

import static de.evosec.myprojectscleaner.MyPaths.getParent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class MyProjectCleaner {

	private static final class CollectWorkspacesAndPotentialReposVisitor
			extends SimpleFileVisitor<Path> {

		private final List<Path> workspaces;
		private final List<Path> potentialRepositories;

		private CollectWorkspacesAndPotentialReposVisitor(
				List<Path> workspaces,
				List<Path> potentialRepositories
		) {
			this.workspaces = workspaces;
			this.potentialRepositories = potentialRepositories;
		}

		@Override
		public FileVisitResult preVisitDirectory(
				Path dir,
				BasicFileAttributes attrs
		) throws IOException {
			Objects.requireNonNull(dir);
			Objects.requireNonNull(attrs);
			if (dir.endsWith(".git")) {
				potentialRepositories.add(dir.getParent());
				return FileVisitResult.SKIP_SIBLINGS;
			}
			if (dir.endsWith("eclipse") || dir.endsWith(".metadata")
					|| dir.endsWith(".recommenders")
					|| dir.endsWith("Servers")) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			if (dir.endsWith("workspace")) {
				workspaces.add(dir);
			}
			return FileVisitResult.CONTINUE;
		}

	}

	private static final class RecursiveDeletingFileVisitor
			extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}

	}

	private static final Logger LOG = LoggerFactory
			.getLogger(MyProjectCleaner.class);

	private Path workingDirectory;
	private String eclipseVersionToKeep;

	public MyProjectCleaner(
			Path workingDirectory,
			String eclipseVersionToKeep
	) {
		this.workingDirectory = workingDirectory;
		this.eclipseVersionToKeep = eclipseVersionToKeep;
	}

	public void clean() throws IOException, GitAPIException {
		List<Path> potentialRepositories = new ArrayList<>();
		List<Path> workspaces = new ArrayList<>();
		Files.walkFileTree(
				workingDirectory,
				new CollectWorkspacesAndPotentialReposVisitor(
						workspaces,
						potentialRepositories
				)
		);
		workspaces.parallelStream().forEach(this::checkWorkspace);
		potentialRepositories.parallelStream().forEach(this::checkRepository);
	}

	private void checkWorkspace(Path workspace) {
		Path eclipse = getParent(workspace).resolve("eclipse");
		try {
			if (Files.exists(eclipse)) {
				Files.readAllLines(eclipse.resolve(".eclipseproduct"))
						.stream()
						.filter(s -> s.startsWith("version="))
						.filter(s -> !s.endsWith(eclipseVersionToKeep))
						.findAny()
						.ifPresent(s -> deleteRecursively(eclipse));
				if (!Files.exists(workspace.resolve(".metadata"))) {
					deleteRecursively(eclipse);
				}
			}
			if (!Files.exists(eclipse)) {
				deleteRecursively(workspace.resolve(".metadata"));
				deleteRecursively(workspace.resolve(".recommenders"));
				deleteRecursively(workspace.resolve("Servers"));
				deleteRecursively(workspace.resolve("RemoteSystemsTempFiles"));
			}
		} catch (IOException | UncheckedIOException e) {
			LOG.error("Error checking {}", workspace, e);
		}
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	private void checkRepository(Path potentialRepository) {
		try (Repository repository = new FileRepositoryBuilder()
				.setWorkTree(potentialRepository.toFile())
				.setMustExist(true)
				.build(); Git git = new Git(repository);) {
			Status status = git.status().call();
			if (status.isClean()) {
				Path eclipse = getParent(getParent(potentialRepository))
						.resolve("eclipse");
				if (!Files.exists(eclipse)) {
					LOG.debug("eclipse does not exist: {}", eclipse);
					LOG.debug(
							"{}: cleaning",
							git.getRepository().getWorkTree()
					);
					git.clean()
							.setForce(true)
							.setCleanDirectories(true)
							.setIgnore(false)
							.setDryRun(false)
							.call();
				}
				git.pull().setRebase(true).call();
			} else {
				LOG.info("{}: not clean", git.getRepository().getWorkTree());
			}
		} catch (IOException | GitAPIException e) {
			LOG.error("Error checking {}", potentialRepository, e);
		}
	}

	private void deleteRecursively(Path directory) {
		if (!Files.isDirectory(directory)) {
			return;
		}
		try {
			Files.walkFileTree(directory, new RecursiveDeletingFileVisitor());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
