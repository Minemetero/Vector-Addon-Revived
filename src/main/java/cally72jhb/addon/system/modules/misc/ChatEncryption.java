package cally72jhb.addon.system.modules.misc;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.events.SendRawMessageEvent;
import cally72jhb.addon.utils.encryption.EncryptUtils;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class ChatEncryption extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> key = sgGeneral.add(new StringSetting.Builder()
        .name("key")
        .description("What key is used to encrypt and decrypt messages.")
        .defaultValue("vector")
        .build()
    );

    private final Setting<String> secretKey = sgGeneral.add(new StringSetting.Builder()
        .name("secret-key")
        .description("A secondary key to ensure safety.")
        .defaultValue(":>")
        .build()
    );

    private final Setting<String> encryptionPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("encryption-prefix")
        .description("What is used as prefix to encrypt messages.")
        .defaultValue("!enc:")
        .build()
    );

    private final Setting<String> decryptionPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("encryption-prefix")
        .description("What is used as prefix to decrypt messages.")
        .defaultValue("!dnc:")
        .build()
    );

    private final Setting<Boolean> alwaysEncrypt = sgGeneral.add(new BoolSetting.Builder()
        .name("always-encrypt")
        .description("Always encrypts your messages automatically.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> wrapLength = sgGeneral.add(new IntSetting.Builder()
        .name("wrap-length")
        .description("How many characters to fit in one line.")
        .defaultValue(50)
        .sliderMin(40)
        .sliderMax(75)
        .min(1)
        .max(256)
        .noSlider()
        .build()
    );

    public ChatEncryption() {
        super(Categories.Misc, "chat-encryption", "Encrypts your chat messages to make them unreadable to other people.");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        event.message = encrypt(alwaysEncrypt.get() ? encryptionPrefix.get() + event.message + ";" : event.message);
    }

    @EventHandler
    private void onSendRawMessage(SendRawMessageEvent event) {
        event.message = encrypt(event.message);
    }

    private String encrypt(String string) {
        if (string.contains(encryptionPrefix.get()) && string.contains(";") && string.indexOf(encryptionPrefix.get()) < string.indexOf(";", string.indexOf(encryptionPrefix.get()))) {
            try {
                int index;

                while ((index = string.indexOf(encryptionPrefix.get())) != -1) {
                    String toEncrypt = string.substring(index + encryptionPrefix.get().length(), string.indexOf(";", index));
                    String encrypted;

                    try {
                        encrypted = EncryptUtils.encrypt(toEncrypt.concat(secretKey.get()), key.get());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        warning("Encryption failed: " + exception + " Exception.");

                        return string;
                    }

                    string = string.substring(0, index).concat(string.substring(index).replaceFirst(encryptionPrefix.get() + toEncrypt + ";", decryptionPrefix.get() + encrypted + ";"));
                }

                if (string.length() > 256) {
                    warning("The encrypted string is above 256 characters. Unable to encrypt.");

                    return string;
                }

                return string;
            } catch (Exception exception) {
                exception.printStackTrace();
                warning("Encryption failed: Invalid format caused a " + exception + " Exception.");

                return string;
            }
        }

        return string;
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        StringBuilder builder = new StringBuilder();

        event.getMessage().asOrderedText().accept((i, style, codePoint) -> {
            builder.append(new String(Character.toChars(codePoint)));
            return true;
        });

        String string = builder.toString();
        String original = builder.toString();

        if (string.contains(decryptionPrefix.get()) && string.contains(";") && string.indexOf(decryptionPrefix.get()) < string.indexOf(";", string.indexOf(decryptionPrefix.get()))) {
            try{
                int index;

                while ((index = string.indexOf(decryptionPrefix.get())) != -1) {
                    String toDecrypt = string.substring(string.indexOf(decryptionPrefix.get(), index) + decryptionPrefix.get().length(), string.indexOf(";", index));
                    String decrypted;

                    try {
                        decrypted = EncryptUtils.decrypt(toDecrypt, key.get());

                        if (!decrypted.endsWith(secretKey.get())) return;

                        decrypted = decrypted.substring(0, decrypted.length() - secretKey.get().length());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        warning("Decryption failed: " + exception + " Exception.");

                        return;
                    }

                    string = string.substring(0, index).concat(string.substring(index).replace(decryptionPrefix.get() + toDecrypt + ";", decrypted));
                }

                StringBuilder wrap = new StringBuilder();

                int i = 0;

                if (!original.isEmpty()) {
                    for (char c : original.toCharArray()) {
                        i++;

                        if (i > wrapLength.get()) {
                            wrap.append("\n");
                            i = 0;
                        }

                        wrap.append(c);
                    }
                }

                event.setMessage(new LiteralText(string + " ").append(new LiteralText("[Decrypted]")
                    .setStyle(Style.EMPTY.withColor(Formatting.GOLD)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(wrap.toString())
                            .setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE))
                        ))
                    )
                ));
            } catch (Exception exception) {
                exception.printStackTrace();
                warning("Decryption failed: Invalid format caused a " + exception + " Exception");
            }
        }
    }
}