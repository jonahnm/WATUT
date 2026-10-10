package com.corosus.watut;

import com.corosus.modconfig.CoroConfigRegistry;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.config.CustomArmCorrections;
import net.minecraft.resources.Identifier;
import net.minecraft.server.players.PlayerList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class WatutMod
{

    // Define mod id in a common place for everything to reference
    public static final String MODID = "watut";

    private static PlayerStatusManagerClient playerStatusManagerClient = null;
    private static PlayerStatusManagerServer playerStatusManagerServer = null;

    public static String configJSONName = "watut-item-arm-adjustments.json";

    private static WatutMod instance;

    public static WatutMod instance() {
        return instance;
    }

    public static PlayerStatusManagerClient getPlayerStatusManagerClient() {
        if (playerStatusManagerClient == null) playerStatusManagerClient = new PlayerStatusManagerClient();
        return playerStatusManagerClient;
    }

    public static PlayerStatusManagerServer getPlayerStatusManagerServer() {
        if (playerStatusManagerServer == null) playerStatusManagerServer = new PlayerStatusManagerServer();
        return playerStatusManagerServer;
    }

    public WatutMod() {
        instance = this;
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigCommon());
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigServerControlledSyncedToClient());
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigClient());

        generateJsonConfigFile(configJSONName);
        CustomArmCorrections.loadJsonConfigs();
    }

    public static void generateJsonConfigFile(String filename) {
        String filePath = "config/" + filename;
        String fileContents = getContentsFromResourceLocation(Identifier.fromNamespaceAndPath(MODID, filePath));
        if (!fileContents.equals("")) {
            File fileOut = new File("./config/" + filename);
            if (!fileOut.exists()) {
                try {
                    FileUtils.writeStringToFile(fileOut, fileContents, StandardCharsets.UTF_8);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static String getContentsFromResourceLocation(Identifier resourceLocation) {
        try {
            //server side compatible way
            String str = "assets/" + resourceLocation.toString().replace(":", "/");
            InputStream in = WatutMod.class.getClassLoader().getResourceAsStream(str);
            String contents = IOUtils.toString(in, StandardCharsets.UTF_8);
            return contents;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public void loadConfigs() {

    }

    public abstract PlayerList getPlayerList();

    public static void dbg(Object obj) {
        //System.out.println("" + obj);
    }

    public abstract boolean isModInstalled(String modID);

    public static net.minecraft.nbt.CompoundTag itemStackToNbt(net.minecraft.world.item.ItemStack itemStack, net.minecraft.core.RegistryAccess registryAccess) {
        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops = registryAccess.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        net.minecraft.nbt.Tag tag = net.minecraft.world.item.ItemStack.CODEC.encodeStart(ops, itemStack)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode ItemStack: " + msg));
        if (tag instanceof net.minecraft.nbt.CompoundTag compoundTag) {
            return compoundTag;
        }
        net.minecraft.nbt.CompoundTag wrapper = new net.minecraft.nbt.CompoundTag();
        wrapper.put("stack", tag);
        return wrapper;
    }

    public static net.minecraft.world.item.ItemStack nbtToItemStack(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.RegistryAccess registryAccess) {
        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops = registryAccess.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        net.minecraft.nbt.Tag actual = tag.contains("stack") ? tag.get("stack") : tag;
        return net.minecraft.world.item.ItemStack.CODEC.parse(ops, actual)
                .resultOrPartial(msg -> dbg("Failed to decode ItemStack: " + msg))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }
}
