package vexside;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.google.gson.*;
import java.io.*;
import java.util.*;

@Mod("yousoft")
public class YouSoft {
    
    public YouSoft() {
        MinecraftForge.EVENT_BUS.register(this);
        Config.load();
    }
    
    public static class Config {
        private static final File FILE = new File("config/yousoft.json");
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        
        public static boolean killAuraEnabled = false;
        public static String killAuraMode = "FUNTIME";
        public static float killAuraRange = 4.2f;
        public static float killAuraReach = 4.5f;
        
        public static boolean speedEnabled = false;
        public static String speedMode = "FUNTIME";
        public static float speedMultiplier = 1.5f;
        
        public static boolean banMode = false;
        public static Map<String, Integer> binds = new HashMap<>();
        
        public static void load() {
            if (!FILE.exists()) { save(); return; }
            try (Reader r = new FileReader(FILE)) {
                JsonObject o = GSON.fromJson(r, JsonObject.class);
                killAuraEnabled = o.get("killAuraEnabled").getAsBoolean();
                killAuraMode = o.get("killAuraMode").getAsString();
                killAuraRange = o.get("killAuraRange").getAsFloat();
                if (o.has("killAuraReach")) killAuraReach = o.get("killAuraReach").getAsFloat();
                speedEnabled = o.get("speedEnabled").getAsBoolean();
                speedMode = o.get("speedMode").getAsString();
                speedMultiplier = o.get("speedMultiplier").getAsFloat();
                banMode = o.has("banMode") && o.get("banMode").getAsBoolean();
                if (o.has("binds")) {
                    JsonObject b = o.getAsJsonObject("binds");
                    for (Map.Entry<String, JsonElement> entry : b.entrySet()) {
                        binds.put(entry.getKey(), entry.getValue().getAsInt());
                    }
                }
            } catch (Exception e) {}
        }
        
        public static void save() {
            JsonObject o = new JsonObject();
            o.addProperty("killAuraEnabled", killAuraEnabled);
            o.addProperty("killAuraMode", killAuraMode);
            o.addProperty("killAuraRange", killAuraRange);
            o.addProperty("killAuraReach", killAuraReach);
            o.addProperty("speedEnabled", speedEnabled);
            o.addProperty("speedMode", speedMode);
            o.addProperty("speedMultiplier", speedMultiplier);
            o.addProperty("banMode", banMode);
            JsonObject b = new JsonObject();
            for (Map.Entry<String, Integer> e : binds.entrySet()) b.addProperty(e.getKey(), e.getValue());
            o.add("binds", b);
            try (Writer w = new FileWriter(FILE)) { GSON.toJson(o, w); } catch (Exception e) {}
        }
    }
    
    private static long lastAttack = 0, lastRot = 0;
    private static float rotYaw = 0;
    
    private static void onKillAura(Minecraft mc) {
        if (Config.banMode || !Config.killAuraEnabled) return;
        if (System.currentTimeMillis() - lastAttack < 100) return;
        
        Entity target = null;
        double best = Config.killAuraReach;
        for (Entity e : mc.world.getAllEntities()) {
            if (!(e instanceof LivingEntity) || e == mc.player) continue;
            if (e instanceof PlayerEntity && ((PlayerEntity)e).isCreative()) continue;
            double d = mc.player.getDistance(e);
            if (d < best) { best = d; target = e; }
        }
        if (target == null) return;
        
        double deltaX = target.getPosX() - mc.player.getPosX();
        double deltaZ = target.getPosZ() - mc.player.getPosZ();
        double deltaY = target.getPosY() + target.getHeight()/2 - (mc.player.getPosY() + mc.player.getEyeHeight());
        
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX*deltaX + deltaZ*deltaZ)));
        
        if (Config.killAuraMode.equals("FUNTIME")) {
            if (System.currentTimeMillis() - lastRot > 500) { rotYaw = (rotYaw + 45) % 360; lastRot = System.currentTimeMillis(); }
            mc.player.rotationYaw = yaw + (float)((Math.random()-0.5)*0.5) + rotYaw*0.1f;
            mc.player.rotationPitch = MathHelper.clamp(pitch + (float)((Math.random()-0.5)*0.3f), -90, 90);
        } else {
            mc.player.rotationYaw = yaw;
            mc.player.rotationPitch = MathHelper.clamp(pitch, -90, 90);
        }
        
        if (mc.player.getDistance(target) <= Config.killAuraRange) {
            mc.playerController.attackEntity(mc.player, target);
            mc.player.swingArm(Hand.MAIN_HAND);
            lastAttack = System.currentTimeMillis();
        }
    }
    
    private static double speedAngle = 0;
    
    private static void onSpeed(Minecraft mc) {
        if (Config.banMode || !Config.speedEnabled) return;
        double base = Config.speedMultiplier * (Config.speedMode.equals("FUNTIME") ? 0.2873 : 0.35);
        if (Config.speedMode.equals("FUNTIME")) {
            speedAngle += 0.8;
            double strafe = Math.sin(speedAngle) * 0.1;
            float yaw = mc.player.rotationYaw;
            double motionX = -Math.sin(Math.toRadians(yaw)) * base + strafe;
            double motionZ = Math.cos(Math.toRadians(yaw)) * base + strafe;
            if (mc.player.ticksExisted % 20 < 5) { motionX *= 1.3; motionZ *= 1.3; }
            mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
        } else {
            float yaw = mc.player.rotationYaw;
            double motionX = -Math.sin(Math.toRadians(yaw)) * base;
            double motionZ = Math.cos(Math.toRadians(yaw)) * base;
            if (mc.player.isOnGround()) { mc.player.jump(); motionX *= 1.4; motionZ *= 1.4; }
            mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
        }
    }
    
    public static class YouSoftGUI extends Screen {
        public YouSoftGUI() { super(new StringTextComponent("YouSoft")); }
        
        @Override
        protected void init() {
            int cx = width/2, y = height/2 - 60;
            addButton(new Button(cx-100, y, 200, 20, new StringTextComponent(
                Config.killAuraEnabled ? "§aKillAura ON" : "§cKillAura OFF"),
                b -> { Config.killAuraEnabled = !Config.killAuraEnabled; Config.save(); refresh(); }));
            addButton(new Button(cx-100, y+25, 200, 20, new StringTextComponent(
                "Mode: " + Config.killAuraMode),
                b -> { Config.killAuraMode = Config.killAuraMode.equals("FUNTIME") ? "HVH" : "FUNTIME"; Config.save(); refresh(); }));
            addButton(new Button(cx-100, y+50, 95, 20, new StringTextComponent("Атака: " + Config.killAuraRange),
                b -> { Config.killAuraRange = Config.killAuraRange >= 6.9f ? 3f : Config.killAuraRange + 0.5f; Config.save(); refresh(); }));
            addButton(new Button(cx+5, y+50, 95, 20, new StringTextComponent("Наводка: " + Config.killAuraReach),
                b -> { Config.killAuraReach = Config.killAuraReach >= 6.9f ? 3f : Config.killAuraReach + 0.5f; Config.save(); refresh(); }));
            
            addButton(new Button(cx-100, y+85, 200, 20, new StringTextComponent(
                Config.speedEnabled ? "§aSpeed ON" : "§cSpeed OFF"),
                b -> { Config.speedEnabled = !Config.speedEnabled; Config.save(); refresh(); }));
            addButton(new Button(cx-100, y+110, 200, 20, new StringTextComponent(
                "Speed Mode: " + Config.speedMode),
                b -> { Config.speedMode = Config.speedMode.equals("FUNTIME") ? "HVH" : "FUNTIME"; Config.save(); refresh(); }));
            addButton(new Button(cx-100, y+135, 200, 20, new StringTextComponent(
                "Speed x" + Config.speedMultiplier),
                b -> { Config.speedMultiplier = Config.speedMultiplier == 1.5f ? 2.0f : 1.5f; Config.save(); refresh(); }));
            
            addButton(new Button(cx-100, y+170, 200, 20, new StringTextComponent(
                Config.banMode ? "§cBAN MODE ON" : "§aBAN MODE OFF"),
                b -> { Config.banMode = !Config.banMode; Config.save(); refresh(); }));
            
            addButton(new Button(cx-100, y+205, 200, 20, new StringTextComponent("Закрыть"),
                b -> { onClose(); }));
        }
        
        void refresh() { minecraft.displayGuiScreen(new YouSoftGUI()); }
        
        @Override
        public void render(int mx, int my, float pt) {
            this.renderBackground();
            super.render(mx, my, pt);
        }
        
        @Override
        public boolean isPauseScreen() { return false; }
    }
    
    private static boolean wasShift = false;
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        boolean shift = mc.gameSettings.keyBindSneak.isKeyDown();
        if (shift && !wasShift && GLFW.glfwGetKey(mc.mainWindow.getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            mc.displayGuiScreen(new YouSoftGUI());
        }
        wasShift = shift;
        
        for (Map.Entry<String, Integer> entry : Config.binds.entrySet()) {
            if (GLFW.glfwGetKey(mc.mainWindow.getHandle(), entry.getValue()) == GLFW.GLFW_PRESS) {
                switch (entry.getKey()) {
                    case "KillAura": Config.killAuraEnabled = !Config.killAuraEnabled; Config.save(); break;
                    case "Speed": Config.speedEnabled = !Config.speedEnabled; Config.save(); break;
                    case "BanMode": Config.banMode = !Config.banMode; Config.save(); break;
                }
            }
        }
        
        onKillAura(mc);
        onSpeed(mc);
    }
}
