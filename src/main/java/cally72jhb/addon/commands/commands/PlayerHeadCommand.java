package cally72jhb.addon.commands.commands;

import cally72jhb.addon.commands.arguments.PlayerNameArgumentType;
import cally72jhb.addon.utils.ExecutorTask;
import cally72jhb.addon.utils.ItemUtils;
import cally72jhb.addon.utils.NetworkUtils;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;
import java.util.Random;

public class PlayerHeadCommand extends Command {
    private final static SimpleCommandExceptionType NO_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));

    public PlayerHeadCommand() {
        super("player-head", "Gives you an player-head in creative.", "head", "skull");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerNameArgumentType.player()).executes(context -> {
            giveHead(PlayerNameArgumentType.getPlayer(context, "player"), 1);

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerNameArgumentType.player()).then(argument("amount", IntegerArgumentType.integer(0, 64)).executes(context -> {
            giveHead(PlayerNameArgumentType.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"));

            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            giveHead(mc.player.getGameProfile().getName(), 1);

            return SINGLE_SUCCESS;
        });
    }

    // Utils

    private void giveHead(String player, int amount) throws CommandSyntaxException {
        if (!mc.player.getAbilities().creativeMode) {
            throw NO_CREATIVE.create();
        } else if (player != null) {
            ExecutorTask.execute(() -> {
                try {
                    ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();

                    Random random = new Random(player.hashCode());
                    String id = "[I;" + random.nextInt() + "," + random.nextInt() + "," + random.nextInt() + "," + random.nextInt() + "]";

                    JsonObject object = NetworkUtils.getJsonObject("https://api.mojang.com/users/profiles/minecraft/" + player);

                    if (object != null) {
                        JsonObject array = NetworkUtils.getJsonObject("https://sessionserver.mojang.com/session/minecraft/profile/" + object.get("id").getAsString());

                        if (array != null) {
                            if (amount > 1) stack.setCount(MathHelper.clamp(amount, 1, 64));

                            // Create NbtCompound for the skull owner
                            NbtCompound nbtData = StringNbtReader.parse(
                                    "{Id:" + id + ",Properties:{textures:[{Value:\""
                                            + array.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString()
                                            + "\"}]}}");

                            // Set the NBT data as a component
                            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtData));
                        }
                    }

                    if (!ItemUtils.insertCreativeStack(stack)) {
                        error("Not enough space in your inventory.");
                    }
                } catch (CommandSyntaxException exception) {
                    exception.printStackTrace();
                    error("Failed to set NBT data.");
                }
            });
        }
    }

}
