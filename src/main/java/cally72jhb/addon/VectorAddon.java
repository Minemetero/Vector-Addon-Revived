package cally72jhb.addon;

import cally72jhb.addon.system.Systems;
import cally72jhb.addon.system.players.Player;
import cally72jhb.addon.system.players.Players;
import cally72jhb.addon.utils.VectorUtils;
import cally72jhb.addon.utils.config.VectorConfig;
import cally72jhb.addon.utils.login.Login;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

public class VectorAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Vector", Items.AMETHYST_SHARD.getDefaultStack());
    public static final Logger LOG = LogManager.getLogger();

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public static Screen screen;

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vector Addon");

        MeteorClient.EVENT_BUS.registerLambdaFactory("cally72jhb.addon", (method, klass) -> (MethodHandles.Lookup) method.invoke(null, klass, MethodHandles.lookup()));

        VectorUtils.init();
        Systems.init();

        MeteorClient.EVENT_BUS.subscribe(this);

        VectorUtils.members();
        VectorUtils.changeIcon();

        mc.options.skipMultiplayerWarning = true;
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    
    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        VectorUtils.members();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (screen != null && mc.currentScreen == null) {
            mc.setScreen(screen);
            screen = null;
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        for (Player player : Players.get()) {
            if (!event.getMessage().getString().contains("muted " + player.name) && player.muted && event.getMessage().getString().contains(player.name)) {
                event.cancel();
                System.out.println(event.getMessage().getString());
            }
        }
    }
}
