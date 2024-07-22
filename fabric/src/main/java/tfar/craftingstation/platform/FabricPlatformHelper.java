package tfar.craftingstation.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.MixinEnvironment;
import tfar.craftingstation.CraftingStationBlockEntityFabric;
import tfar.craftingstation.SideContainerWrapperFabric;
import tfar.craftingstation.blockentity.CraftingStationBlockEntity;
import tfar.craftingstation.menu.CraftingStationMenu;
import tfar.craftingstation.network.C2SModPacket;
import tfar.craftingstation.network.S2CModPacket;
import tfar.craftingstation.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import tfar.craftingstation.util.Empty;
import tfar.craftingstation.util.SideContainerWrapper;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <MSG extends S2CModPacket> void registerClientPacket(CustomPacketPayload.Type<MSG> type, StreamCodec<RegistryFriendlyByteBuf, MSG> streamCodec) {
        PayloadTypeRegistry.playS2C().register(type,streamCodec);//payload needs to be registered on server/client, packethandler is client only
        if (MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(type,(payload, context) -> context.client().execute(payload::handleClient));
        }
    }

    @Override
    public <MSG extends C2SModPacket> void registerServerPacket(CustomPacketPayload.Type<MSG> type, StreamCodec<RegistryFriendlyByteBuf, MSG> streamCodec) {
        PayloadTypeRegistry.playC2S().register(type,streamCodec);
        ServerPlayNetworking.registerGlobalReceiver(type,(payload, context) -> context.player().server.execute(() -> payload.handleServer(context.player())));
    }

    @Override
    public void sendToClient(CustomPacketPayload msg, ServerPlayer player) {
        ServerPlayNetworking.send(player,msg);
    }

    @Override
    public void sendToServer(CustomPacketPayload msg) {
        ClientPlayNetworking.send(msg);
    }

    @Override
    public void forgeHooks$setCraftingPlayer(Player player) {

    }

    @Override
    public void forgeEventFactory$firePlayerCraftingEvent(Player player, ItemStack stack, CraftingContainer craftingContainer) {

    }

    @Override
    public boolean hasCapability(BlockEntity blockEntity) {
        Storage<ItemVariant> storageViews = ItemStorage.SIDED.find(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
        return storageViews instanceof InventoryStorage;
    }

    @Override
    public MLConfig getConfig() {
        return new MLConfig() {
            @Override
            public boolean showItemsInTable() {
                return true;
            }

            @Override
            public boolean sideContainers() {
                return true;
            }
        };
    }


    @Override
    public MenuType<CraftingStationMenu> customMenu() {
        return new ExtendedScreenHandlerType<>(CraftingStationMenu::new,BlockPos.STREAM_CODEC);
    }

    @Override
    public SideContainerWrapper getWrapper(BlockEntity blockEntity) {
        if (blockEntity == null) return Empty.EMPTY;
        Storage<ItemVariant> storageViews = ItemStorage.SIDED.find(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
        return storageViews instanceof InventoryStorage inventoryStorage ? new SideContainerWrapperFabric(inventoryStorage) : null;
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider menuProvider, BlockPos pos) {
        player.openMenu(menuProvider);
    }

    @Override
    public CraftingStationBlockEntity create(BlockPos pos, BlockState state) {
        return new CraftingStationBlockEntityFabric(pos,state);
    }
}
