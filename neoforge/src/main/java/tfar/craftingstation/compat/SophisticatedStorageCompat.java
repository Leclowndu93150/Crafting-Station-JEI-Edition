package tfar.craftingstation.compat;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;

import java.util.Locale;

public class SophisticatedStorageCompat {
    
    /**
     * Fixes SophisticatedStorage block entity display names that contain untranslated %s%s placeholders.
     * This occurs when wood storage blocks (like Gold Chest) have null wood types, causing the translation
     * to pass empty strings as parameters which results in "%s%s" being displayed literally.
     * <p>
     * Instead, we directly access the block entity to get the wood type and construct the proper component.
     * 
     * @param blockEntity The block entity from the side container
     * @return A properly constructed component with correct wood type translation
     */
    public static Component fixDisplayName(BlockEntity blockEntity) {
        if (blockEntity instanceof WoodStorageBlockEntity woodStorage) {
            String translationKey = Util.makeDescriptionId("block",
                BuiltInRegistries.BLOCK.getKey(woodStorage.getBlockState().getBlock()));

            if (woodStorage.getCustomName() != null) {
                return woodStorage.getCustomName();
            }

            return woodStorage.getWoodType()
                .map(woodType -> {
                    Component woodName = Component.translatable("wood_name.sophisticatedstorage." + woodType.name().toLowerCase(Locale.ROOT));
                    return Component.translatable(translationKey, woodName, " ");
                })
                .orElseGet(() -> Component.translatable(translationKey, "", ""));
        }

        return blockEntity instanceof MenuProvider menuProvider
            ? menuProvider.getDisplayName() 
            : blockEntity.getBlockState().getBlock().getName();
    }
}
