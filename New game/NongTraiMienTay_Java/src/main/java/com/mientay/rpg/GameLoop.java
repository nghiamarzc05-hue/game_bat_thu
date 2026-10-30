package com.mientay.rpg;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

public class GameLoop extends AnimationTimer {
    private final GameState state;
    private final InputHandler input;
    private final Renderer renderer;
    private final GraphicsContext gc;
    private long lastTime = 0;
    private double regenAccum = 0;
    private double autoSaveAccum = 0;

    public GameLoop(GameState state, InputHandler input, Renderer renderer, GraphicsContext gc) {
        this.state = state;
        this.input = input;
        this.renderer = renderer;
        this.gc = gc;
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) {
            lastTime = now;
            return;
        }

        double dt = (now - lastTime) / 1_000_000_000.0;
        if (dt > 0.05) dt = 0.05;
        lastTime = now;
        double nowSec = now / 1_000_000_000.0;

        update(dt);
        renderer.render(gc, nowSec);
    }

    private void update(double dt) {
        if (state.notifTimer > 0) {
            state.notifTimer -= dt;
        }

        for (GameState.Plot p : state.plots) {
            if (p.crop != null && p.timer > 0) {
                p.timer = Math.max(0, p.timer - dt);
            }
        }

        regenAccum += dt;
        if (regenAccum >= 1.0) {
            regenAccum = 0;
            if (!state.battle.active) {
                for (GameState.Pet p : state.pets) {
                    if (p.hp < p.maxHp) {
                        p.hp = Math.min(p.maxHp, p.hp + Math.max(1, (int) Math.ceil(p.maxHp * 0.02)));
                    }
                }
            }
        }

        autoSaveAccum += dt;
        if (autoSaveAccum >= 30.0) {
            autoSaveAccum = 0;
            state.saveGame(true);
        }

        if (state.battle.active) return;

        updateWildPets(dt);
        updatePlayer(dt);
    }

    private void updateWildPets(double dt) {
        for (GameState.WildPet wp : state.wildPets) {
            if (!wp.alive) continue;
            wp.aiTimer += dt;
            if (wp.aiTimer >= wp.aiInterval) {
                wp.aiTimer = 0;
                wp.aiState = Math.random() < 0.4 ? "idle" : "walk";
                double a = Math.random() * Math.PI * 2;
                wp.vx = Math.cos(a) * wp.spd;
                wp.vy = Math.sin(a) * wp.spd;
            }
            if ("walk".equals(wp.aiState)) {
                double npx = wp.px + wp.vx;
                double npy = wp.py + wp.vy;
                if (!state.isBlockedPx(npx, npy)) {
                    wp.px = npx;
                    wp.py = npy;
                    wp.walkFrame += dt * 4;
                } else {
                    wp.aiState = "idle";
                }
            }
            double dist = Math.hypot(wp.px - state.player.px, wp.py - state.player.py);
            wp.exclAlpha = dist < GameData.TS * 2.5 ? 1 : 0;
            if (dist < GameData.TS * 1.1) {
                state.startBattle(wp);
                break;
            }
        }
    }

    private void updatePlayer(double dt) {
        int dx = 0;
        int dy = 0;

        if (input.isPressed(KeyCode.W) || input.isPressed(KeyCode.UP)) dy = -1;
        else if (input.isPressed(KeyCode.S) || input.isPressed(KeyCode.DOWN)) dy = 1;
        else if (input.isPressed(KeyCode.A) || input.isPressed(KeyCode.LEFT)) dx = -1;
        else if (input.isPressed(KeyCode.D) || input.isPressed(KeyCode.RIGHT)) dx = 1;

        if (dx != 0 || dy != 0) {
            state.player.targetPx = null;
            state.player.targetPy = null;
            double npx = state.player.px + dx * state.player.spd;
            double npy = state.player.py + dy * state.player.spd;
            if (!state.isBlockedPx(npx, npy)) {
                state.player.px = npx;
                state.player.py = npy;
                state.player.tx = (int) Math.floor((state.player.px + GameData.TS / 2.0) / GameData.TS);
                state.player.ty = (int) Math.floor((state.player.py + GameData.TS / 2.0) / GameData.TS);
            }
            state.player.moving = true;
            state.player.walkFrame += 0.18;
        } else if (state.player.targetPx != null && state.player.targetPy != null) {
            double dX = state.player.targetPx - state.player.px;
            double dY = state.player.targetPy - state.player.py;
            double dist = Math.hypot(dX, dY);
            if (dist < state.player.spd + 1) {
                state.player.px = state.player.targetPx;
                state.player.py = state.player.targetPy;
                state.player.tx = (int) Math.floor((state.player.px + GameData.TS / 2.0) / GameData.TS);
                state.player.ty = (int) Math.floor((state.player.py + GameData.TS / 2.0) / GameData.TS);
                state.player.targetPx = null;
                state.player.targetPy = null;
                state.player.moving = false;
                interact();
            } else {
                state.player.px += (dX / dist) * state.player.spd;
                state.player.py += (dY / dist) * state.player.spd;
                state.player.tx = (int) Math.floor((state.player.px + GameData.TS / 2.0) / GameData.TS);
                state.player.ty = (int) Math.floor((state.player.py + GameData.TS / 2.0) / GameData.TS);
                state.player.moving = true;
                state.player.walkFrame += 0.14;
            }
        } else {
            state.player.moving = false;
        }

        if (input.isPressed(KeyCode.SPACE) || input.isPressed(KeyCode.E)) {
            interact();
            input.setKeyReleased(KeyCode.SPACE);
            input.setKeyReleased(KeyCode.E);
        }
    }

    public void interact() {
        GameData.GameMap map = GameData.MAPS.get(state.currentMap);
        if (map != null) {
            for (GameData.Portal pt : map.portals()) {
                if (pt.tx() == state.player.tx && pt.ty() == state.player.ty) {
                    state.currentMap = pt.to();
                    state.player.tx = 14;
                    state.player.ty = 8;
                    state.player.px = 14 * GameData.TS;
                    state.player.py = 8 * GameData.TS;
                    state.spawnWildPetsForMap(state.currentMap);
                    state.notifyMsg("🚪 Bước vào " + GameData.MAPS.get(state.currentMap).name());
                    return;
                }
            }
        }

        if ("farm".equals(state.currentMap)) {
            for (GameState.Plot plot : state.plots) {
                if (plot.tx == state.player.tx && plot.ty == state.player.ty) {
                    if (plot.crop == null) {
                        String seedKey = null;
                        for (String k : new String[]{"seed_rice", "seed_corn", "seed_banana", "seed_lotus"}) {
                            if (state.inv.getOrDefault(k, 0) > 0) {
                                seedKey = k;
                                break;
                            }
                        }
                        if (seedKey != null) {
                            state.inv.put(seedKey, state.inv.get(seedKey) - 1);
                            GameData.CropInfo ci = GameData.CROPS.get(seedKey);
                            plot.crop = seedKey;
                            plot.timer = ci.time();
                            plot.max = ci.time();
                            state.notifyMsg("🌱 Gieo " + ci.name() + "!");
                        } else {
                            state.notifyMsg("❌ Hết hạt giống, mở Tiệm mua nha!");
                        }
                    } else if (plot.timer <= 0) {
                        GameData.CropInfo seedInfo = GameData.CROPS.get(plot.crop);
                        String yieldId = seedInfo.yieldId();
                        int amt = seedInfo.amt();
                        state.inv.put(yieldId, state.inv.getOrDefault(yieldId, 0) + amt);
                        GameData.CropInfo cropInfo = GameData.CROPS.get(yieldId);
                        plot.crop = null;
                        plot.timer = 0;
                        state.notifyMsg("✅ Thu hoạch " + amt + "x " + cropInfo.name() + "!");
                    }
                    break;
                }
            }
        }
    }
}
