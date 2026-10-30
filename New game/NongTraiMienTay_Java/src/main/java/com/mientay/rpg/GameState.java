package com.mientay.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameState {

    public static class Pet {
        public String uid;
        public String name;
        public String icon;
        public String el;
        public String pot;
        public int level;
        public int exp;
        public int expNext;
        public int hp;
        public int maxHp;
        public int atk;

        public Pet() {}

        public Pet(String uid, String name, String icon, String el, String pot, int level, int exp, int expNext, int hp, int maxHp, int atk) {
            this.uid = uid;
            this.name = name;
            this.icon = icon;
            this.el = el;
            this.pot = pot;
            this.level = level;
            this.exp = exp;
            this.expNext = expNext;
            this.hp = hp;
            this.maxHp = maxHp;
            this.atk = atk;
        }
    }

    public static class Plot {
        public int tx, ty;
        public String crop;
        public double timer;
        public double max;

        public Plot() {}

        public Plot(int tx, int ty) {
            this.tx = tx;
            this.ty = ty;
        }
    }

    public static class WildPet {
        public int id;
        public GameData.PetTemplate tmpl;
        public String pot;
        public int maxHp;
        public int hp;
        public int atk;
        public double px, py, vx, vy, spd;
        public double aiTimer;
        public double aiInterval;
        public String aiState; // "idle" or "walk"
        public double walkFrame;
        public boolean alive;
        public double exclAlpha;

        public WildPet(int id, GameData.PetTemplate tmpl, String pot, int maxHp, int hp, int atk, double px, double py, double spd) {
            this.id = id;
            this.tmpl = tmpl;
            this.pot = pot;
            this.maxHp = maxHp;
            this.hp = hp;
            this.atk = atk;
            this.px = px;
            this.py = py;
            this.spd = spd;
            this.aiTimer = 0;
            this.aiInterval = 2;
            this.aiState = "idle";
            this.walkFrame = 0;
            this.alive = true;
            this.exclAlpha = 0;
        }
    }

    public static class Player {
        public int tx = 14;
        public int ty = 8;
        public double px = 14 * GameData.TS;
        public double py = 8 * GameData.TS;
        public double spd = 2.8;
        public String dir = "down";
        public boolean moving = false;
        public double walkFrame = 0;
        public Double targetPx = null;
        public Double targetPy = null;
    }

    public static class BattleVFX {
        public String type; // "skill", "slash", "catch", "popup"
        public String el;
        public double sx, sy, tx, ty;
        public String text;
        public String colHex;
        public double t = 0;
        public double dur;

        public BattleVFX(String type, String el, double sx, double sy, double tx, double ty, String text, String colHex) {
            this.type = type;
            this.el = el;
            this.sx = sx;
            this.sy = sy;
            this.tx = tx;
            this.ty = ty;
            this.text = text;
            this.colHex = colHex;
            this.dur = "popup".equals(type) ? 0.9 : 0.55;
        }
    }

    public static class Battle {
        public boolean active = false;
        public Pet pet = null;
        public WildPet wildRef = null;
        public String log = "";
        public int playerHP = 0, playerMaxHP = 0;
        public int enemyHP = 0, enemyMaxHP = 0;
        public double pShake = 0, eShake = 0;
        public List<BattleVFX> vfx = new ArrayList<>();
    }

    public int gold = 500;
    public String currentMap = "farm";
    public Map<String, Integer> inv = new LinkedHashMap<>();
    public List<Pet> pets = new ArrayList<>();
    public int activeIdx = 0;
    public List<Plot> plots = new ArrayList<>();

    public transient List<WildPet> wildPets = new ArrayList<>();
    public transient int wpIdCount = 0;
    public Player player = new Player();
    public transient Battle battle = new Battle();

    public transient String notification = "";
    public transient double notifTimer = 0;
    public transient Runnable onHudUpdate = () -> {};

    private static final String SAVE_FILE = "savegame.json";

    public GameState() {
        inv.put("seed_rice", 5);
        inv.put("seed_corn", 3);
        inv.put("seed_banana", 1);
        inv.put("seed_lotus", 1);
        inv.put("com_lua", 2);

        pets.add(new Pet("p0", "Mèo Mướp Vườn", "🐱", "Mộc", "B", 1, 0, 60, 80, 80, 17));

        for (int i = 0; i < 6; i++) {
            plots.add(new Plot(8 + (i % 3) * 2, 6 + (i / 3) * 2));
        }
    }

    public void notifyMsg(String msg) {
        this.notification = msg;
        this.notifTimer = 2.6;
        if (onHudUpdate != null) onHudUpdate.run();
    }

    public void spawnWildPetsForMap(String mapId) {
        wildPets.clear();
        List<GameData.PetTemplate> tmpls = GameData.WILD_TEMPLATES.getOrDefault(mapId, List.of());
        for (GameData.PetTemplate tmpl : tmpls) {
            int count = 2 + (int)(Math.random() * 2);
            for (int i = 0; i < count; i++) {
                double px, py;
                int att = 0;
                do {
                    px = (2 + Math.floor(Math.random() * (GameData.COLS - 4))) * GameData.TS;
                    py = (2 + Math.floor(Math.random() * (GameData.ROWS - 4))) * GameData.TS;
                    att++;
                } while (att < 50 && isBlockedPx(px, py));

                String pot = GameData.getRandPot();
                double bMult = GameData.POTS.get(pot).baseMult();
                int baseHp = Math.max(1, (int) Math.round(tmpl.hp() * bMult));
                int baseAtk = Math.max(1, (int) Math.round(tmpl.atk() * bMult));

                wildPets.add(new WildPet(wpIdCount++, tmpl, pot, baseHp, baseHp, baseAtk, px, py, 0.6 + Math.random() * 0.5));
            }
        }
    }

    public boolean isBlockedPx(double px, double py) {
        int tx = (int) Math.round(px / GameData.TS);
        int ty = (int) Math.round(py / GameData.TS);
        if (tx < 1 || tx >= GameData.COLS - 1 || ty < 1 || ty >= GameData.ROWS - 1) return true;
        return false;
    }

    public void startBattle(WildPet wp) {
        if (pets.isEmpty()) return;
        Pet ap = pets.get(activeIdx);
        battle.active = true;
        battle.wildRef = wp;
        battle.pet = ap;
        battle.playerHP = ap.hp;
        battle.playerMaxHP = ap.maxHp;
        battle.enemyHP = wp.hp;
        battle.enemyMaxHP = wp.maxHp;
        battle.log = "⚠️ Đụng " + wp.tmpl.name() + " hoang dã!";
        player.targetPx = null;
        player.targetPy = null;
    }

    public void doBattleAttack() {
        if (!battle.active) return;
        double mult = GameData.getElMult(battle.pet.el, battle.wildRef.tmpl.el());
        int dmg = Math.max(1, (int) Math.round((battle.pet.atk + Math.random() * 6) * mult));
        battle.enemyHP = Math.max(0, battle.enemyHP - dmg);
        battle.eShake = 0.45;
        battle.vfx.add(new BattleVFX("slash", battle.pet.el, 225, 135, GameData.CW - 225, 135, "", "#fde047"));
        battle.vfx.add(new BattleVFX("popup", battle.pet.el, GameData.CW - 225, 95, GameData.CW - 225, 50, "-" + dmg + " ⚔️", "#fde047"));
        battle.log = "⚔️ Đánh thường gây " + dmg + " sát thương!";
        if (battle.enemyHP <= 0) {
            endBattle("win");
        } else {
            enemyHit();
        }
    }

    public void doBattleSkill() {
        if (!battle.active) return;
        double mult = GameData.getElMult(battle.pet.el, battle.wildRef.tmpl.el());
        String sName = GameData.PET_SKILLS.getOrDefault(battle.pet.name, "✨ Tuyệt Chiêu");
        int dmg = Math.max(1, (int) Math.round(battle.pet.atk * 2.2 * mult));
        battle.enemyHP = Math.max(0, battle.enemyHP - dmg);
        battle.eShake = 0.7;
        String elHex = GameData.getElement(battle.pet.el).hex();
        String tag = mult > 1.1 ? " 💥KHẮC HỆ!" : (mult < 0.9 ? " 🛡️Kháng" : "");
        battle.vfx.add(new BattleVFX("skill", battle.pet.el, 225, 135, GameData.CW - 225, 135, "", elHex));
        battle.vfx.add(new BattleVFX("popup", battle.pet.el, GameData.CW - 225, 90, GameData.CW - 225, 45, "-" + dmg + tag, mult > 1.1 ? "#4ade80" : elHex));
        battle.log = sName + "! Gây " + dmg + " sát thương!" + tag;
        if (battle.enemyHP <= 0) {
            endBattle("win");
        } else {
            enemyHit();
        }
    }

    public void doBattleCatch() {
        if (!battle.active) return;
        battle.vfx.add(new BattleVFX("catch", "Thổ", 225, 135, GameData.CW - 225, 135, "", "#fb923c"));
        double chance = 0.3 + (1.0 - (double) battle.enemyHP / battle.enemyMaxHP) * 0.6;
        if (Math.random() < chance) {
            endBattle("caught");
        } else {
            battle.vfx.add(new BattleVFX("popup", "Hỏa", GameData.CW - 225, 90, GameData.CW - 225, 45, "💨 Sẩy rồi!", "#f87171"));
            battle.log = "😤 Quăng nơm hụt rồi!";
            enemyHit();
        }
    }

    public void doBattleRun() {
        battle.active = false;
        battle.vfx.clear();
        notifyMsg("🏃 Tẩu thoát an toàn!");
    }

    private void enemyHit() {
        double mult = GameData.getElMult(battle.wildRef.tmpl.el(), battle.pet.el);
        int dmg = Math.max(1, (int) Math.round((battle.wildRef.atk + Math.random() * 4) * mult));
        battle.playerHP = Math.max(0, battle.playerHP - dmg);
        battle.pet.hp = battle.playerHP;
        battle.pShake = 0.5;
        battle.vfx.add(new BattleVFX("popup", battle.wildRef.tmpl.el(), 225, 95, 225, 50, "-" + dmg + " 🔥", "#f87171"));
        battle.log += " | 🔥 Địch phản đòn " + dmg + " HP!";
        if (battle.playerHP <= 0) {
            endBattle("lose");
        }
    }

    public void endBattle(String res) {
        battle.active = false;
        Pet ap = pets.get(activeIdx);
        WildPet wp = battle.wildRef;
        if ("win".equals(res)) {
            gold += 50;
            wp.alive = false;
            ap.hp = battle.playerHP;
            gainExp(ap, 25);
            notifyMsg("🏆 Thắng trận! +50 Tiền");
            saveGame(true);
        } else if ("caught".equals(res)) {
            String pot = wp.pot != null ? wp.pot : GameData.getRandPot();
            ap.hp = battle.playerHP;
            pets.add(new Pet("p" + System.currentTimeMillis(), wp.tmpl.name(), wp.tmpl.icon(), wp.tmpl.el(), pot, 1, 0, 60, battle.enemyMaxHP, battle.enemyMaxHP, wp.atk));
            wp.alive = false;
            gainExp(ap, 40);
            notifyMsg("🎉 Bắt được " + wp.tmpl.name() + " [Tư chất " + pot + "]!");
            saveGame(true);
        } else if ("lose".equals(res)) {
            ap.hp = Math.max(1, (int) Math.floor(ap.maxHp * 0.25));
            notifyMsg("💀 Thú bảo kiệt sức, rút lui!");
            saveGame(true);
        }
        if (onHudUpdate != null) onHudUpdate.run();
    }

    public void gainExp(Pet pet, int amount) {
        pet.exp += amount;
        while (pet.exp >= pet.expNext) {
            pet.exp -= pet.expNext;
            pet.level++;
            pet.expNext = (int) Math.floor(60 * Math.pow(1.45, pet.level - 1));
            double rate = GameData.POTS.getOrDefault(pet.pot, GameData.POTS.get("C")).rate();
            pet.maxHp = (int) Math.round(pet.maxHp * rate);
            pet.hp = pet.maxHp;
            pet.atk = (int) Math.round(pet.atk * rate);
            notifyMsg("🎆 " + pet.name + " lên Cấp " + pet.level + "!");
        }
    }

    public void saveGame(boolean isAuto) {
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
            if (!isAuto) notifyMsg("💾 Đã lưu tiến trình game thành công!");
        } catch (Exception e) {
            if (!isAuto) notifyMsg("❌ Lỗi lưu game!");
        }
    }

    public boolean loadGame() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return false;
        try (FileReader reader = new FileReader(f)) {
            Gson gson = new Gson();
            GameState loaded = gson.fromJson(reader, GameState.class);
            if (loaded != null && loaded.pets != null && !loaded.pets.isEmpty()) {
                this.gold = loaded.gold;
                this.currentMap = loaded.currentMap != null ? loaded.currentMap : "farm";
                if (loaded.inv != null) this.inv = loaded.inv;
                this.pets = loaded.pets;
                this.activeIdx = Math.min(loaded.activeIdx, this.pets.size() - 1);
                if (loaded.plots != null && !loaded.plots.isEmpty()) this.plots = loaded.plots;
                if (loaded.player != null) this.player = loaded.player;
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void resetSave() {
        File f = new File(SAVE_FILE);
        if (f.exists()) f.delete();
    }
}
