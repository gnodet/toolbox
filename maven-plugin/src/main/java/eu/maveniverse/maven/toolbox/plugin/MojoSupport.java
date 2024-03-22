/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.Slf4jOutput;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for all Mojos.
 */
public abstract class MojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Output output = new Slf4jOutput(logger);

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            doExecute(ToolboxCommando.create(runtime, context));
        } catch (RuntimeException e) {
            throw new MojoExecutionException(e);
        } catch (Exception e) {
            throw new MojoFailureException(e);
        }
    }

    protected abstract void doExecute(ToolboxCommando toolboxCommando) throws Exception;
}