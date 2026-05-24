package vexside;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
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
            try (Reader reader = new FileReader(FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                killAuraEnabled = json.get("killAuraEnabled").getAsBoolean();
                killAuraMode = json.get("killAuraMode").getAsString();
                killAuraRange = json.get("killAuraRange").getAsFloat();
                if (json.has("killAuraReach")) killAuraReach = json.get("killAuraReach").getAsFloat();
                speedEnabled = json.get("speedEnabled").getAsBoolean();
                speedMode = json.get("speedMode").getAsString();
                speedMultiplier = json.get("speedMultiplier").getAsFloat();
                banMode = json.has("banMode") && json.get("banMode").getAsBoolean();
                if (json.has("binds")) {
                    JsonObject bindsJson = json.getAsJsonObject("binds");
                    for (Map.Entry<String, JsonElement> entry : bindsJson.entrySet()) {
                        binds.put(entry.getKey(), entry.getValue().getAsInt());
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        public static void save() {
            JsonObject json = new JsonObject();
            json.addProperty("killAuraEnabled", killAuraEnabled);
            json.addProperty("killAuraMode", killAuraMode);
            json.addProperty("killAuraRange", killAuraRange);
            json.addProperty("killAuraReach", killAuraReach);
            json.addProperty("speedEnabled", speedEnabled);
            json.addProperty("speedMode", speedMode);
            json.addProperty("speedMultiplier", speedMultiplier);
            json.addProperty("banMode", banMode);
            JsonObject bindsJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : binds.entrySet()) {
                bindsJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("binds", bindsJson);
            try (Writer writer = new FileWriter(FILE)) {
                GSON.toJson(json, writer);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static long lastAttack = 0, lastRot = 0;
    private static float rotYaw = 0;

    private static void onKillAura(Minecraft mc) {
        if (Config.banMode || !Config.killAuraEnabled) return;
        if (System.currentTimeMillis() - lastAttack < 100) return;

        Entity target = null;
        double bestDist = Config.killAuraReach;
        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player) continue;
            if (entity instanceof PlayerEntity && ((PlayerEntity)entity).isCreative()) continue;
            double dist = mc.player.getDistance(entity);
            if (dist < bestDist) {
                bestDist = dist;
                target = entity;
            }
        }
        if (target == null) return;

        Vector3d targetPos = target.getPositionVec().add(0, target.getHeight()/2, 0);
        Vector3d playerPos = mc.player.getEyePosition(1.0F);
        Vector3d delta = targetPos.subtract(playerPos);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        if (Config.killAuraMode.equals("FUNTIME")) {
            if (System.currentTimeMillis() - lastRot > 500) {
                rotYaw = (rotYaw + 45.0F) % 360.0F;
                lastRot = System.currentTimeMillis();
            }
            mc.player.rotationYaw = yaw + (float)((Math.random() - 0.5D) * 0.5D) + rotYaw * 0.1F;
            mc.player.rotationPitch = MathHelper.clamp(pitch + (float)((Math.random() - 0.5D) * 0.3D), -90.0F, 90.0F);
        } else {
            mc.player.rotationYaw = yaw;
            mc.player.rotationPitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        }

        if (mc.player.getDistance(target) <= Config.killAuraRange) {
            mc.playerController.attackEntity(mc.player, target);
            mc.player.swingArm(Hand.MAIN_HAND);
            lastAttack = System.currentTimeMillis();
        }
    }

    private static double speedAngle = 0.0D;

    private static void onSpeed(Minecraft mc) {
        if (Config.banMode || !Config.speedEnabled) return;
        double baseSpeed = Config.speedMultiplier * (Config.speedMode.equals("FUNTIME") ? 0.2873D : 0.35D);
        if (Config.speedMode.equals("FUNTIME")) {
            speedAngle += 0.8D;
            double strafe = Math.sin(speedAngle) * 0.1D;
            float yaw = mc.player.rotationYaw;
            Vector3d forward = Vector3d.fromPitchYaw(0.0F, yaw);
            Vector3d right = Vector3d.fromPitchYaw(0.0F, yaw + 90.0F);
            double motionX = forward.x * baseSpeed + right.x * strafe;
            double motionZ = forward.z * baseSpeed + right.z * strafe;
            if (mc.player.ticksExisted % 20 < 5) {
                motionX *= 1.3D;
                motionZ *= 1.3D;
            }
            mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
        } else {
            float yaw = mc.player.rotationYaw;
            Vector3d forward = Vector3d.fromPitchYaw(0.0F, yaw);
            double motionX = forward.x * baseSpeed;
            double motionZ = forward.z * baseSpeed;
            if (mc.player.isOnGround()) {
                mc.player.jump();
                motionX *= 1.4D;
                motionZ *= 1.4D;
            }
            mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
        }
    }

    public static class YouSoftGUI extends Screen {
        public YouSoftGUI() {
            super(new StringTextComponent("YouSoft"));
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int y = this.height / 2 - 60;

            this.addButton(new Button(centerX - 100, y, 200, 20, new StringTextComponent(Config.killAuraEnabled ? "§aKillAura ON" : "§cKillAura OFF"), (button) -> {
                Config.killAuraEnabled = !Config.killAuraEnabled;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 25, 200, 20, new StringTextComponent("Mode: " + Config.killAuraMode), (button) -> {
                Config.killAuraMode = Config.killAuraMode.equals("FUNTIME") ? "HVH" : "FUNTIME";
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 50, 95, 20, new StringTextComponent("Атака: " + Config.killAuraRange), (button) -> {
                Config.killAuraRange = Config.killAuraRange >= 6.9F ? 3.0F : Config.killAuraRange + 0.5F;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX + 5, y + 50, 95, 20, new StringTextComponent("Наводка: " + Config.killAuraReach), (button) -> {
                Config.killAuraReach = Config.killAuraReach >= 6.9F ? 3.0F : Config.killAuraReach + 0.5F;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 85, 200, 20, new StringTextComponent(Config.speedEnabled ? "§aSpeed ON" : "§cSpeed OFF"), (button) -> {
                Config.speedEnabled = !Config.speedEnabled;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 110, 200, 20, new StringTextComponent("Speed Mode: " + Config.speedMode), (button) -> {
                Config.speedMode = Config.speedMode.equals("FUNTIME") ? "HVH" : "FUNTIME";
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 135, 200, 20, new StringTextComponent("Speed x" + Config.speedMultiplier), (button) -> {
                Config.speedMultiplier = Config.speedMultiplier == 1.5F ? 2.0F : 1.5F;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 170, 200, 20, new StringTextComponent(Config.banMode ? "§cBAN MODE ON" : "§aBAN MODE OFF"), (button) -> {
                Config.banMode = !Config.banMode;
                Config.save();
                this.refresh();
            }));
            this.addButton(new Button(centerX - 100, y + 205, 200, 20, new StringTextComponent("Закрыть"), (button) -> {
                this.onClose();
            }));
        }

        private void refresh() {
            this.minecraft.displayGuiScreen(new YouSoftGUI());
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static boolean wasShiftPressed = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isShiftDown = mc.gameSettings.keyBindSneak.isKeyDown();
        if (isShiftDown && !YouSoft.wasShiftPressed && GLFW.glfwGetKey(mc.mainWindow.getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            mc.displayGuiScreen(new YouSoftGUI());
        }
        YouSoft.wasShiftPressed = isShiftDown;

        for (Map.Entry<String, Integer> entry : Config.binds.entrySet()) {
            if (GLFW.glfwGetKey(mc.mainWindow.getHandle(), entry.getValue()) == GLFW.GLFW_PRESS) {
                String key = entry.getKey();
                switch (key) {
                    case "KillAura": Config.killAuraEnabled = !Config.killAuraEnabled; Config.save(); break;
                    case "Speed": Config.speedEnabled = !Config.speedEnabled; Config.save(); break;
                    case "BanMode": Config.banMode = !Config.banMode; Config.save(); break;
                }
            }
        }

        YouSoft.onKillAura(mc);
        YouSoft.onSpeed(mc);
    }
}
