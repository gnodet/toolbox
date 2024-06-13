/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Collects paths to matched artifact from plugins, if exists.
 */
@Mojo(name = "plugin-tree-find", threadSafe = true)
public class PluginTreeFindMojo extends MPPluginMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * The artifact matcher spec.
     */
    @Parameter(property = "artifactMatcherSpec", required = true)
    private String artifactMatcherSpec;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ResolutionRoot root = pluginAsResolutionRoot(toolboxCommando, false);
        if (root != null) {
            return toolboxCommando.treeFind(
                    ResolutionScope.parse(scope),
                    root,
                    verboseTree,
                    toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec),
                    output);
        } else {
            boolean result = true;
            for (ResolutionRoot resolutionRoot : allPluginsAsResolutionRoots(toolboxCommando)) {
                result = result
                        && toolboxCommando.treeFind(
                                ResolutionScope.parse(scope),
                                resolutionRoot,
                                verboseTree,
                                toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec),
                                output);
                output.normal("");
            }
            return result;
        }
    }
}
