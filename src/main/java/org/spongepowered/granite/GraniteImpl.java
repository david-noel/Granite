package org.spongepowered.granite;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import org.spongepowered.common.SpongeImpl;

public class GraniteImpl implements SpongeImpl {

    @Override
    public WorldServer[] getWorlds() {
        return MinecraftServer.getServer().worldServers;
    }

    @Override
    public String getSaveFolder(WorldProvider provider) {
        return provider.getDimensionId() == 0 ? null : "DIM" + provider.getDimensionId();
    }

}
