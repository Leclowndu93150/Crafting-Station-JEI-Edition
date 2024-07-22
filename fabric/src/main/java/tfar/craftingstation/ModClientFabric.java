package tfar.craftingstation;

import net.fabricmc.api.ClientModInitializer;
import tfar.craftingstation.client.ModClient;

public class ModClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModClient.renderers();
    }
}
