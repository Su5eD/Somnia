package com.kingrunes.somnia.common.util;

import com.kingrunes.somnia.Somnia;
import com.kingrunes.somnia.api.capability.CapabilityFatigue;
import com.kingrunes.somnia.api.capability.IFatigue;
import com.kingrunes.somnia.common.SomniaConfig;
import com.kingrunes.somnia.common.compat.CompatModule;
import com.kingrunes.somnia.server.ServerTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public class SomniaUtil {
    public static String timeStringForWorldTime(long time)
    {
        time += 6000; // Tick -> Time offset

        time = time % 24000;
        int hours = (int) Math.floor(time / (double)1000);
        int minutes = (int) ((time % 1000) / 1000.0d * 60);

        String lsHours = String.valueOf(hours);
        String lsMinutes = String.valueOf(minutes);

        if (lsHours.length() == 1)
            lsHours = "0"+lsHours;
        if (lsMinutes.length() == 1)
            lsMinutes = "0"+lsMinutes;

        return lsHours + ":" + lsMinutes;
    }

    public static boolean doesPlayHaveAnyArmor(EntityPlayer player)
    {
        for (ItemStack stack : player.inventory.armorInventory) {
            if (!stack.isEmpty()) {
                if (Loader.isModLoaded("openblocks") && stack.getItem().getRegistryName().equals(CompatModule.SLEEPING_BAG)) return false;
                return true;
            }
        }
        return false;
    }

    public static long calculateWakeTime(long totalWorldTime, int i)
    {
        long l;
        long timeInDay = totalWorldTime % 24000L;
        l = totalWorldTime - timeInDay + i;
        if (timeInDay > i)
            l += 24000L;
        return l;
    }

    /*
     * These methods are referenced by ASM generated bytecode
     *
     */

    @SuppressWarnings("unused")
    @SideOnly(Side.CLIENT)
    public static void renderWorld(float par1, long par2)
    {
        if (Minecraft.getMinecraft().player.isPlayerSleeping() && SomniaConfig.PERFORMANCE.disableRendering)
        {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            return;
        }
        Minecraft.getMinecraft().entityRenderer.renderWorld(par1, par2);
    }

    @SuppressWarnings("unused")
    public static boolean doMobSpawning(WorldServer par1WorldServer)
    {
        boolean defValue = par1WorldServer.getGameRules().getBoolean("doMobSpawning");
        if (!SomniaConfig.PERFORMANCE.disableCreatureSpawning || !defValue)
            return defValue;

        for (ServerTickHandler serverTickHandler : Somnia.instance.tickHandlers)
        {
            if (serverTickHandler.worldServer == par1WorldServer)
                return serverTickHandler.currentState != SomniaState.ACTIVE;
        }

        throw new IllegalStateException("tickHandlers doesn't contain match for given world server");
    }

    @SuppressWarnings("unused")
    public static void chunkLightCheck(Chunk chunk)
    {
        if (!SomniaConfig.PERFORMANCE.disableMoodSoundAndLightCheck)
            chunk.checkLight();

        for (ServerTickHandler serverTickHandler : Somnia.instance.tickHandlers)
        {
            if (serverTickHandler.worldServer == chunk.getWorld())
            {
                if (serverTickHandler.currentState != SomniaState.ACTIVE)
                    chunk.checkLight();
                return;
            }
        }

        chunk.checkLight();
    }

    public static String translate(String key) {
        return I18n.translateToLocal(key);
    }

    @SuppressWarnings("unused")
    public static boolean checkFatigue(EntityPlayer player) {
        IFatigue fatigue = player.getCapability(CapabilityFatigue.FATIGUE_CAPABILITY, null);
        return player.capabilities.isCreativeMode || fatigue == null || fatigue.getFatigue() >= SomniaConfig.FATIGUE.minimumFatigueToSleep;
    }

    @SuppressWarnings("unused")
    public static boolean shouldResetSpawn(EntityPlayer player) {
        IFatigue props = player.getCapability(CapabilityFatigue.FATIGUE_CAPABILITY, null);
        if (props != null) {
            return props.shouldResetSpawn();
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static void tick()
    {
        synchronized (Somnia.instance.tickHandlers)
        {
            for (ServerTickHandler serverTickHandler : Somnia.instance.tickHandlers)
                serverTickHandler.tickStart();
        }
    }

    public static double calculateFatigueToReplenish(EntityPlayer player)
    {
        long worldTime = player.world.getTotalWorldTime();
        long wakeTime = SomniaUtil.calculateWakeTime(worldTime, 0);
        return SomniaConfig.FATIGUE.fatigueReplenishRate * (wakeTime - worldTime);
    }
}