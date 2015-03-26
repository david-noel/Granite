/*
 * This file is part of Granite, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <http://github.com/SpongePowered>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.granite.launch.server;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.granite.launch.GraniteLaunch;
import org.spongepowered.granite.launch.console.GraniteConsole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class VanillaServerTweaker implements ITweaker {

    private static final Logger logger = LogManager.getLogger();

    private String[] args = ArrayUtils.EMPTY_STRING_ARRAY;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        Path gamePath = gameDir != null ? gameDir.toPath() : Paths.get("");

        boolean jline = true;

        if (args != null && !args.isEmpty()) {
            OptionParser parser = new OptionParser();
            parser.allowsUnrecognizedOptions();
            parser.accepts("nojline"); // TODO: Naming

            OptionSet options = parser.parse(args.toArray(new String[args.size()]));
            jline = !options.has("nojline");

            this.args = options.nonOptionArguments().stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
        }

        GraniteLaunch.initialize(gamePath);

        // Initialize jline
        logger.debug("Initializing console...");
        try {
            GraniteConsole.initialize(jline);
        } catch (Throwable t) {
            logger.error("Failed to initialize console", t);
            try {
                GraniteConsole.initializeFallback();
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize terminal", e);
            }
        }

        GraniteConsole.start();
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader loader) {
        logger.info("Initializing Granite...");

        // We shouldn't load these through Launchwrapper as they use native dependencies
        loader.addClassLoaderExclusion("io.netty.");
        loader.addClassLoaderExclusion("jline.");
        loader.addClassLoaderExclusion("org.fusesource.");

        // Don't allow libraries to be transformed
        loader.addTransformerExclusion("joptsimple.");

        // Minecraft Server libraries
        loader.addTransformerExclusion("com.google.gson.");
        loader.addTransformerExclusion("org.apache.commons.codec.");
        loader.addTransformerExclusion("org.apache.commons.io.");
        loader.addTransformerExclusion("org.apache.commons.lang3.");

        // SpongeAPI
        loader.addTransformerExclusion("com.flowpowered.math.");
        loader.addTransformerExclusion("org.slf4j.");

        // Guice
        loader.addTransformerExclusion("com.google.inject.");
        loader.addTransformerExclusion("org.aopalliance.");

        // Configurate
        loader.addTransformerExclusion("ninja.leaping.configurate.");
        loader.addTransformerExclusion("com.typesafe.config.");

        // Granite Launch
        loader.addClassLoaderExclusion("org.spongepowered.tools.");
        loader.addClassLoaderExclusion("org.spongepowered.granite.launch.");
        loader.addTransformerExclusion("org.spongepowered.granite.mixin.");

        // The server GUI won't work if we don't exclude this: log4j2 wants to have this in the same classloader
        loader.addClassLoaderExclusion("com.mojang.util.QueueLogAppender");

        // Check if we're running in deobfuscated environment already
        logger.info("Applying runtime deobfuscation...");
        if (isObfuscated()) {
            Launch.blackboard.put("granite.deobf-srg", Paths.get("bin", "deobf.srg.gz"));
            loader.registerTransformer("org.spongepowered.granite.launch.transformers.DeobfuscationTransformer");
            logger.info("Runtime deobfuscation is applied.");
        } else {
            logger.info("Runtime deobfuscation was not applied. Granite is being loaded in a deobfuscated environment.");
        }

        logger.info("Applying access transformer...");
        Launch.blackboard.put("granite.at", "granite_at.cfg");
        loader.registerTransformer("org.spongepowered.granite.launch.transformers.AccessTransformer");

        logger.info("Initializing Mixin environment...");
        MixinBootstrap.init();
        MixinEnvironment env = MixinEnvironment.getCurrentEnvironment();
        env.addConfiguration("mixins.granite.json");
        env.setSide(MixinEnvironment.Side.SERVER);
        loader.registerTransformer(MixinBootstrap.TRANSFORMER_CLASS);

        logger.info("Initialization finished. Starting Minecraft server...");
    }

    private static boolean isObfuscated() {
        try {
            return Launch.classLoader.getClassBytes("net.minecraft.world.World") == null;
        } catch (IOException ignored) {
            return true;
        }
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.server.MinecraftServer";
    }

    @Override
    public String[] getLaunchArguments() {
        return this.args;
    }

}
