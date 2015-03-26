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
package org.spongepowered.granite.mixin.server;

import jline.console.ConsoleReader;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.granite.Granite;
import org.spongepowered.granite.console.ConsoleCommandCompleter;
import org.spongepowered.granite.launch.console.GraniteConsole;

import java.io.IOException;

@Mixin(targets = "net/minecraft/server/dedicated/DedicatedServer$2")
public abstract class MixinConsoleHandler extends Thread {

    @Shadow // Change to "this$0" for dev environment and "field_72428_a" for production
    private DedicatedServer field_72428_a; // TODO: How do we access this properly at dev time and in production

    @Override
    @Overwrite
    public void run() {
        final ConsoleReader reader = GraniteConsole.getReader();
        reader.addCompleter(new ConsoleCommandCompleter(this.field_72428_a));

        try {
            String line;

            while (!this.field_72428_a.isServerStopped() && this.field_72428_a.isServerRunning()) {
                line = reader.readLine("> ");

                if (line != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        this.field_72428_a.addPendingCommand(line, this.field_72428_a);
                    }
                }
            }
        } catch (IOException e) {
            Granite.instance.getLogger().error("Failed to handle console input", e);
        }
    }

}
