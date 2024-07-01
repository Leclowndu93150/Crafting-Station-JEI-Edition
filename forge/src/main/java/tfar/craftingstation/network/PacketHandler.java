package tfar.craftingstation.network;

import tfar.craftingstation.CraftingStation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


public class PacketHandler {

  public static SimpleChannel INSTANCE;

  public static void registerMessages(String channelName) {
    INSTANCE = NetworkRegistry.newSimpleChannel(CraftingStation.id(channelName), () -> "1.0", s -> true, s -> true);
    INSTANCE.registerMessage(0, S2CLastRecipePacket.class,
            S2CLastRecipePacket::encode,
            S2CLastRecipePacket::new,
            S2CLastRecipePacket::handle);
  }
}