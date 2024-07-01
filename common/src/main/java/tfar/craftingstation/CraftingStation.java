package tfar.craftingstation;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.craftingstation.network.PacketHandler;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class CraftingStation {

    public static final String MOD_ID = "craftingstation";
    public static final String MOD_NAME = "CraftingStation";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final TagKey<BlockEntityType<?>> blacklisted
            = TagKey.create(Registries.BLOCK_ENTITY_TYPE, id("blacklisted"));

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {
        PacketHandler.registerPackets();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID,path);
    }
}