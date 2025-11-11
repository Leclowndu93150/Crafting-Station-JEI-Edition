package tfar.craftingstation.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tfar.craftingstation.Configs;
import tfar.craftingstation.compat.SophisticatedStorageCompat;
import tfar.craftingstation.menu.CraftingStationMenu;
import tfar.craftingstation.network.C2SModPacket;
import tfar.craftingstation.network.PacketHandlerNeoForge;
import tfar.craftingstation.network.S2CModPacket;
import tfar.craftingstation.platform.services.IPlatformHelper;
import tfar.craftingstation.util.Empty;
import tfar.craftingstation.util.SideContainerNeoForge;
import tfar.craftingstation.util.SideContainerWrapper;

import java.util.function.Function;

public class NeoForgePlatformHelper implements IPlatformHelper {

    final MLConfig config = new Configs();

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    public static PayloadRegistrar registrar;
    @Override
    public <MSG extends S2CModPacket> void registerClientPacket(CustomPacketPayload.Type<MSG> type, StreamCodec<RegistryFriendlyByteBuf,MSG> streamCodec) {
        registrar.playToClient(type, streamCodec, (p, t) -> p.handleClient());
    }

    @Override
    public <MSG extends C2SModPacket> void registerServerPacket(CustomPacketPayload.Type<MSG> type, StreamCodec<RegistryFriendlyByteBuf, MSG> streamCodec) {
        registrar.playToServer(type, streamCodec, (p, t) -> p.handleServer((ServerPlayer) t.player()));
    }

    @Override
    public void sendToClient(CustomPacketPayload msg, ServerPlayer player) {
        PacketHandlerNeoForge.sendToClient(msg,player);
    }

    @Override
    public void sendToServer(CustomPacketPayload msg) {
        PacketHandlerNeoForge.sendToServer(msg);
    }

    @Override
    public void forgeHooks$setCraftingPlayer(Player player) {
        CommonHooks.setCraftingPlayer(player);
    }

    @Override
    public void forgeEventFactory$firePlayerCraftingEvent(Player player, ItemStack stack, CraftingContainer craftingContainer) {
        EventHooks.firePlayerCraftingEvent(player,stack,craftingContainer);
    }

    @Override
    public boolean hasCapability(BlockEntity blockEntity) {
        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),blockEntity,null);
        return handler instanceof IItemHandlerModifiable;
    }

    @Override
    public MLConfig getConfig() {
        return config;
    }

    @Override
    public MenuType<CraftingStationMenu> customMenu() {
        return IMenuTypeExtension.create((windowId, inv, data) -> new CraftingStationMenu(windowId,inv,data.readBlockPos()));
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider menuProvider, BlockPos pos) {
        player.openMenu(menuProvider, pos);
    }

    @Override
    public SideContainerWrapper getWrapper(BlockEntity blockEntity) {
        if (blockEntity == null) return Empty.EMPTY;

        IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),blockEntity,null);
        if (handler instanceof IItemHandlerModifiable iItemHandlerModifiable) {
            return new SideContainerNeoForge(iItemHandlerModifiable);
        }
        return Empty.EMPTY;
    }

    @Override
    public ItemStack createSideDisplayStack(Level level, BlockPos pos, BlockState state, Player player) {
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack stack = state.getCloneItemStack(hitResult, level, pos, player);
        if (stack.isEmpty()) {
            stack = state.getBlock().getCloneItemStack(level, pos, state);
        }
        return stack.isEmpty() ? new ItemStack(state.getBlock()) : stack;
    }

    @Override
    public Component fixSophisticatedStorageDisplayName(BlockEntity blockEntity) {
        if (isModLoaded("sophisticatedstorage")) {
            return SophisticatedStorageCompat.fixDisplayName(blockEntity);
        }
        return blockEntity instanceof MenuProvider menuProvider 
            ? menuProvider.getDisplayName() 
            : blockEntity.getBlockState().getBlock().getName();
    }
}
