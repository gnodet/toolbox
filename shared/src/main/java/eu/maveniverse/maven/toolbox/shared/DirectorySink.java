/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactNameMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, for example like a filesystem directory.
 */
public final class DirectorySink implements ArtifactSink {
    /**
     * Creates plain "flat" directory sink that accepts all artifacts and writes them out having filenames as
     * "A[-C]-V.E" and prevents overwrite (what you usually want).
     * <p>
     * This means that if your set of artifacts have artifacts with different groupIDs but same artifactIDs, this sink
     * will fail while accepting them, to prevent overwrite. Duplicated artifacts are filtered out.
     */
    public static DirectorySink flat(Output output, Path path) throws IOException {
        return new DirectorySink(output, path, ArtifactMatcher.unique(), a -> a, ArtifactNameMapper.ACVE(), false);
    }

    private final Output output;
    private final Path directory;
    private final boolean directoryCreated;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, String> artifactNameMapper;
    private final boolean allowOverwrite;
    private final HashSet<Path> writtenPaths;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param directory The directory, if not existing, will be created.
     * @param artifactMatcher The matcher, that decides is this sink accepting artifact or not.
     * @param artifactMapper The artifact mapper, that may re-map artifact.
     * @param artifactNameMapper The artifact name mapper, that decides what file name will be of the artifact.
     * @param allowOverwrite Does sink allow overwrites. Tip: you usually do not want to allow, as that means you have
     *                       some mismatch in name mapping or alike.
     * @throws IOException In case of IO problem.
     */
    private DirectorySink(
            Output output,
            Path directory,
            ArtifactMatcher artifactMatcher,
            ArtifactMapper artifactMapper,
            ArtifactNameMapper artifactNameMapper,
            boolean allowOverwrite)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.directory = requireNonNull(directory, "directory").toAbsolutePath();
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("directory must not exists, or must be a directory");
        }
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            this.directoryCreated = true;
        } else {
            this.directoryCreated = false;
        }

        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        this.artifactNameMapper = requireNonNull(artifactNameMapper, "artifactNameMapper");
        this.allowOverwrite = allowOverwrite;
        this.writtenPaths = new HashSet<>();
    }

    @Override
    public void accept(Collection<Artifact> artifacts) throws IOException {
        requireNonNull(artifacts, "artifacts");
        output.verbose("Copying {} artifacts to directory {}", artifacts.size(), directory);
        try {
            for (Artifact artifact : artifacts) {
                accept(artifact);
            }
        } catch (IOException e) {
            cleanup();
            throw e;
        }
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        output.verbose("Accept artifact {}", artifact);
        if (artifactMatcher.test(artifact)) {
            output.verbose("  matched");
            String name = artifactNameMapper.apply(artifactMapper.apply(artifact));
            output.verbose("  mapped to name {}", name);
            Path target = directory.resolve(name).toAbsolutePath();
            if (!target.startsWith(directory)) {
                throw new IOException("Path escape prevented; check mappings");
            }
            if (!writtenPaths.add(target) && !allowOverwrite) {
                throw new IOException("Overwrite prevented; check mappings");
            }
            output.verbose("  copied to file {}", target);
            Files.copy(
                    artifact.getFile().toPath(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            output.verbose("  not matched");
        }
    }

    private void cleanup() {
        output.error("Cleaning up: {}", directory);
        writtenPaths.forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ex) {
                // ignore
            }
        });
        if (directoryCreated) {
            try {
                Files.deleteIfExists(directory);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
