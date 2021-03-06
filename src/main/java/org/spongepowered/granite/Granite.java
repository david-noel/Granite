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
package org.spongepowered.granite;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.block.BlockChangeEvent;
import org.spongepowered.api.event.state.ConstructionEvent;
import org.spongepowered.api.event.state.InitializationEvent;
import org.spongepowered.api.event.state.LoadCompleteEvent;
import org.spongepowered.api.event.state.PostInitializationEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.event.state.StateEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.command.CommandService;
import org.spongepowered.api.service.command.SimpleCommandService;
import org.spongepowered.api.util.event.Subscribe;
import org.spongepowered.granite.event.GraniteEventFactory;
import org.spongepowered.granite.guice.GraniteGuiceModule;
import org.spongepowered.granite.launch.GraniteLaunch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Granite implements PluginContainer {

    public static final Granite instance = new Granite();

    private static final Injector injector = Guice.createInjector(new GraniteGuiceModule());
    private final Logger logger = LogManager.getLogger();
    private final Path gameDir;
    private final Path pluginsDir;
    private final Path configDir;
    private GraniteGame game;

    private Granite() {
        this.gameDir = GraniteLaunch.getGameDirectory();
        this.pluginsDir = this.gameDir.resolve("plugins");
        this.configDir = this.gameDir.resolve("config");
    }

    public static Injector getInjector() {
        return injector;
    }

    public Game getGame() {
        return this.game;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Path getGameDirectory() {
        return this.gameDir;
    }

    public Path getPluginsDirectory() {
        return this.pluginsDir;
    }

    public Path getConfigDirectory() {
        return this.configDir;
    }

    public void load() {
        try {
            this.logger.info("Loading Granite...");

            this.game = injector.getInstance(GraniteGame.class);

            try {
                SimpleCommandService commandService = new SimpleCommandService(this.game.getPluginManager());
                this.game.getServiceManager().setProvider(this, CommandService.class, commandService);
                this.game.getEventManager().register(this, commandService);
            } catch (ProviderExistsException e) {
                this.logger.warn("An unknown CommandService was already registered", e);
            }

            if (Files.notExists(this.gameDir) || Files.notExists(this.pluginsDir)) {
                Files.createDirectories(this.pluginsDir);
            }

            getLogger().info("Loading plugins...");
            this.game.getPluginManager().loadPlugins();
            postState(ConstructionEvent.class);
            getLogger().info("Initializing plugins...");
            postState(PreInitializationEvent.class);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void initialize() {
        postState(InitializationEvent.class);
        postState(PostInitializationEvent.class);
        getLogger().info("Successfully loaded and initialized plugins.");

        postState(LoadCompleteEvent.class);
    }

    public void postState(Class<? extends StateEvent> type) {
        this.game.getEventManager().post(GraniteEventFactory.createStateEvent(type, this.game));
    }

    @Override
    public String getId() {
        return "granite";
    }

    @Override
    public String getName() {
        return "Granite";
    }

    @Override
    public String getVersion() {
        return this.game.getImplementationVersion();
    }

    @Override
    public Object getInstance() {
        return this;
    }
}
