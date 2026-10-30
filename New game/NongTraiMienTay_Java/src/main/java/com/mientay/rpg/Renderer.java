package com.mientay.rpg;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class Renderer {
    public record ClickBtn(double x, double y, double w, double h, Runnable fn) {}

    private final GameState state;
    public final List<ClickBtn> battleBtns = new ArrayList<>();
    private final double[] noise = new double[GameData.COLS * GameData.ROWS];
    private final double[][] particles = new double[45][5];

    public Renderer(GameState state) {
        this.state = state;
        for (int i = 0; i < noise.length; i++) {
            noise[i] = Math.random();
        }
        for (int i = 0; i < particles.length; i++) {
            particles[i][0] = Math.random() * GameData.CW;
            particles[i][1] = Math.random() * GameData.CH;
            particles[i][2] = (Math.random() - 0.5) * 0.7;
            particles[i][3] = (Math.random() - 0.5) * 0.7;
            particles[i][4] = Math.random() * 1.8 + 1.2;
        }
    }

    private final DropShadow spriteShadow = new DropShadow(6, 0, 3, Color.rgb(0, 0, 0, 0.55));

    public boolean drawPetSprite(GraphicsContext gc, String petName, double x, double y, double w, double h, int frameIdx) {
        Image img = GameData.getPetSprite(petName);
        if (img == null || img.isError() || img.getWidth() <= 0) return false;
        double fw = img.getWidth() / 3.0;
        double fh = img.getHeight();
        int col = Math.abs(frameIdx) % 3;
        gc.save();
        gc.setImageSmoothing(true);
        gc.setEffect(spriteShadow);
        gc.drawImage(img, col * fw, 0, fw, fh, x, y, w, h);
        gc.restore();
        return true;
    }

    public void render(GraphicsContext gc, double nowSec) {
        gc.setImageSmoothing(true);
        gc.clearRect(0, 0, GameData.CW, GameData.CH);
        drawTiles(gc, nowSec);
        drawBiomeProps(gc, nowSec);
        drawBiomeParticles(gc, nowSec);
        drawPlots(gc, nowSec);
        drawPortals(gc, nowSec);
        drawWildPets(gc, nowSec);
        drawFollowerPet(gc, nowSec);
        drawPlayer(gc, nowSec);
        drawSkyAndLighting(gc, nowSec);
        drawMinimap(gc);

        if (state.battle.active) {
            drawBattle(gc, nowSec);
        } else {
            battleBtns.clear();
        }

        if (state.notifTimer > 0 && state.notification != null && !state.notification.isEmpty()) {
            drawNotification(gc, state.notification);
        }
    }

    private void drawTiles(GraphicsContext gc, double nowSec) {
        String m = state.currentMap;
        GameData.GameMap map = GameData.MAPS.get(m);
        if (map == null) return;

        Color[] bg = map.bg();
        for (int ty = 0; ty < GameData.ROWS; ty++) {
            for (int tx = 0; tx < GameData.COLS; tx++) {
                double px = tx * GameData.TS;
                double py = ty * GameData.TS;
                double n = noise[ty * GameData.COLS + tx];

                if (tx == 0 || tx == GameData.COLS - 1 || ty == 0 || ty == GameData.ROWS - 1) {
                    gc.setFill("mines".equals(m) ? Color.web("#0f172a") : "mountain".equals(m) ? Color.web("#292524") : "sea".equals(m) ? Color.web("#0369a1") : Color.web("#14532d"));
                    gc.fillRect(px, py, GameData.TS, GameData.TS);

                    gc.setFill("mines".equals(m) ? Color.web("#334155") : "mountain".equals(m) ? Color.web("#44403c") : "sea".equals(m) ? Color.web("#0284c7") : Color.web("#166534"));
                    double r = 13 + Math.sin(tx * 3 + ty) * 2;
                    gc.fillOval(px + 16 - r, py + 16 - r, r * 2, r * 2);
                    continue;
                }

                Color tileCol = bg[n < 0.45 ? 0 : (n < 0.8 ? 1 : 2)];
                gc.setFill(tileCol);
                gc.fillRect(px, py, GameData.TS, GameData.TS);

                if ("farm".equals(m)) {
                    if ((ty == 8 && tx >= 2 && tx <= 27) || (tx == 14 && ty >= 2 && ty <= 14) || (tx >= 7 && tx <= 13 && ty >= 5 && ty <= 9)) {
                        gc.setFill(n < 0.5 ? Color.web("#926239") : Color.web("#855832"));
                        gc.fillRect(px, py, GameData.TS, GameData.TS);
                    } else if (n > 0.75) {
                        gc.setFill(Color.rgb(74, 222, 128, 0.4));
                        gc.fillRect(px + 8, py + 12, 2, 6);
                        gc.fillRect(px + 12, py + 10, 2, 7);
                    }
                } else if ("fields".equals(m) && n > 0.35) {
                    double sway = Math.sin(nowSec * 3 + tx * 0.7 + ty) * 2.5;
                    gc.setStroke(Color.web("#eab308"));
                    gc.setLineWidth(1.5);
                    gc.strokeLine(px + 10, py + 26, px + 11 + sway, py + 9);
                    gc.strokeLine(px + 20, py + 28, px + 21 + sway, py + 11);
                } else if ("lake".equals(m)) {
                    double wave = Math.sin(nowSec * 2 + tx + ty) * 3;
                    gc.setStroke(Color.rgb(125, 211, 252, 0.25));
                    gc.setLineWidth(1.2);
                    gc.strokeLine(px + 6 + wave, py + 16, px + 22 + wave, py + 16);
                    if (n > 0.78) {
                        gc.setFill(Color.web("#16a34a"));
                        gc.fillOval(px + 7, py + 7, 18, 18);
                        if (n > 0.88) {
                            gc.setFill(Color.web("#f472b6"));
                            gc.fillOval(px + 11.5, py + 9.5, 9, 9);
                            gc.setFill(Color.web("#fef08a"));
                            gc.fillOval(px + 14, py + 12, 4, 4);
                        }
                    }
                } else if ("sea".equals(m)) {
                    if (tx <= 4) {
                        gc.setFill(n < 0.5 ? Color.web("#fde68a") : Color.web("#fcd34d"));
                        gc.fillRect(px, py, GameData.TS, GameData.TS);
                        if (tx == 4) {
                            double foam = Math.sin(nowSec * 3 + ty) * 4;
                            gc.setFill(Color.rgb(255, 255, 255, 0.75));
                            gc.fillRect(px + 22 + foam, py, 6, GameData.TS);
                        }
                    } else {
                        double wave = Math.sin(nowSec * 2.5 + tx * 0.8 + ty) * 3;
                        gc.setStroke(Color.rgb(186, 230, 253, 0.28));
                        gc.setLineWidth(1.5);
                        gc.strokeLine(px + 5 + wave, py + 14, px + 24 + wave, py + 14);
                    }
                } else if ("mines".equals(m) && n > 0.82) {
                    double glow = 0.5 + Math.sin(nowSec * 4 + tx * ty) * 0.4;
                    gc.setFill(Color.rgb(56, 189, 248, glow));
                    gc.fillPolygon(new double[]{px + 16, px + 22, px + 16, px + 10}, new double[]{py + 6, py + 18, py + 24, py + 18}, 4);
                } else if ("meadow".equals(m) && n > 0.65) {
                    gc.setFill(n > 0.82 ? Color.web("#f43f5e") : (n > 0.74 ? Color.web("#facc15") : Color.web("#c084fc")));
                    gc.fillOval(px + 11, py + 12, 6, 6);
                    gc.setFill(Color.WHITE);
                    gc.fillOval(px + 13, py + 14, 2.4, 2.4);
                } else if ("mountain".equals(m) && n > 0.8) {
                    gc.setFill(Color.web("#57534e"));
                    gc.fillOval(px + 6, py + 11, 20, 14);
                    if (n > 0.9) {
                        gc.setFill(Color.web("#ef4444"));
                        gc.fillRect(px + 12, py + 16, 8, 2);
                    }
                } else if ("mist_forest".equals(m) && n > 0.78) {
                    gc.setFill(Color.web("#c084fc"));
                    gc.fillOval(px + 11, py + 9, 10, 8);
                    gc.setFill(Color.web("#e2e8f0"));
                    gc.fillRect(px + 14, py + 15, 4, 5);
                }
            }
        }
    }

    private void drawCoconutTree(GraphicsContext gc, double tx, double ty, double nowSec) {
        gc.setFill(Color.rgb(0, 0, 0, 0.28));
        gc.fillOval(tx - 10, ty - 2, 32, 12);

        gc.setStroke(Color.web("#78350f"));
        gc.setLineWidth(6);
        gc.strokeLine(tx, ty, tx + 4, ty - 42);

        gc.setFill(Color.web("#65a30d"));
        gc.fillOval(tx - 3, ty - 45, 8, 8);
        gc.fillOval(tx + 3, ty - 44, 8, 8);

        double sway = Math.sin(nowSec * 2.2 + tx * 0.1) * 0.12;
        double[] angles = {-2.6, -1.8, -0.7, 0.4, 1.3, 2.2};
        gc.setStroke(Color.web("#15803d"));
        gc.setLineWidth(3.2);
        for (int i = 0; i < angles.length; i++) {
            double ang = angles[i] + sway * (i % 2 == 0 ? 1 : -1);
            double ex = tx + 4 + Math.cos(ang) * 24;
            double ey = ty - 42 + Math.sin(ang) * 16 + 5;
            gc.strokeLine(tx + 4, ty - 42, ex, ey);
        }
    }

    private void drawBiomeProps(GraphicsContext gc, double nowSec) {
        String m = state.currentMap;
        if ("farm".equals(m)) {
            // 1. Nhà Tranh Bà Tư (tx=2..5, ty=2..4)
            double hx = 2 * GameData.TS;
            double hy = 2 * GameData.TS;
            gc.setFill(Color.rgb(0, 0, 0, 0.35));
            gc.fillRoundRect(hx + 4, hy + 12, GameData.TS * 4 - 4, GameData.TS * 2.6, 12, 12);

            gc.setFill(Color.web("#78350f"));
            gc.fillRoundRect(hx + 6, hy + 28, GameData.TS * 4 - 12, GameData.TS * 1.8, 8, 8);
            gc.setFill(Color.web("#292524"));
            gc.fillRoundRect(hx + 48, hy + 44, 28, 42, 6, 6);
            gc.setFill(Color.web("#fef08a"));
            gc.fillRoundRect(hx + 18, hy + 44, 18, 18, 6, 6);
            gc.fillRoundRect(hx + 88, hy + 44, 18, 18, 6, 6);

            // Đèn lồng đỏ & Lu nước đất nung
            gc.setFill(Color.web("#dc2626"));
            gc.fillOval(hx + 10, hy + 36, 9, 9);
            gc.fillOval(hx + 110, hy + 36, 9, 9);
            gc.setFill(Color.web("#9a3412"));
            gc.fillOval(hx + 121, hy + 63, 18, 22);

            // Mái lá dừa
            gc.setFill(Color.web("#a16207"));
            gc.fillPolygon(new double[]{hx - 4, hx + GameData.TS * 2, hx + GameData.TS * 4 + 4}, new double[]{hy + 36, hy + 2, hy + 36}, 3);
            gc.setFill(Color.web("#ca8a04"));
            gc.fillPolygon(new double[]{hx + 2, hx + GameData.TS * 2, hx + GameData.TS * 4 - 2}, new double[]{hy + 33, hy + 6, hy + 33}, 3);

            // Khói bếp Bà Tư
            for (int i = 0; i < 3; i++) {
                double sy = hy + 6 - ((nowSec * 18 + i * 14) % 36);
                double sx = hx + GameData.TS * 3 + Math.sin(nowSec * 2 + i) * 6;
                double sa = Math.max(0, 1.0 - (hy + 6 - sy) / 36.0) * 0.45;
                gc.setFill(Color.rgb(226, 232, 240, sa));
                double r = 5 + i * 2;
                gc.fillOval(sx - r, sy - r, r * 2, r * 2);
            }

            gc.setFill(Color.web("#451a03"));
            gc.fillRoundRect(hx + 30, hy + 22, 68, 14, 6, 6);
            gc.setFill(Color.web("#fde047"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 8));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("🏡 NHÀ BÀ TƯ", hx + 64, hy + 31);

            // NPC Bà Tư đứng trước hiên nhà
            double bnx = hx + 62;
            double bny = hy + 86 + Math.sin(nowSec * 2.5) * 1.2;
            gc.setFill(Color.web("#78350f"));
            gc.fillRoundRect(bnx - 7, bny, 14, 12, 4, 4);
            gc.setFill(Color.web("#fed7aa"));
            gc.fillOval(bnx - 6, bny - 11, 12, 12);
            gc.setFill(Color.web("#cbd5e1"));
            gc.fillOval(bnx - 4, bny - 14, 8, 8);
            if (Math.hypot(state.player.px - bnx, state.player.py - bny) < GameData.TS * 3.2) {
                gc.setFill(Color.rgb(15, 23, 42, 0.9));
                gc.fillRoundRect(bnx - 68, bny - 32, 136, 16, 8, 8);
                gc.setFill(Color.web("#fde047"));
                gc.setFont(Font.font("System", FontWeight.BOLD, 9));
                gc.fillText("💬 Bà Tư: \"Ghé bếp nấu ăn nghen!\"", bnx, bny - 21);
            }

            // 2. Ao Cá & Cầu Khỉ (tx=2..6, ty=11..14)
            double px = 2 * GameData.TS;
            double py = 11 * GameData.TS;
            RadialGradient pondGrad = new RadialGradient(0, 0, px + 64, py + 42, 60, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#0e7490")),
                    new Stop(0.7, Color.web("#155e75")),
                    new Stop(1, Color.web("#083344")));
            gc.setFill(pondGrad);
            gc.fillOval(px + 6, py + 8, 116, 68);
            gc.setStroke(Color.web("#164e63"));
            gc.setLineWidth(3);
            gc.strokeOval(px + 6, py + 8, 116, 68);

            // Cá chép Koi bơi trong ao
            for (int i = 0; i < 3; i++) {
                double ang = nowSec * 1.2 + i * 2.1;
                double fx = px + 64 + Math.cos(ang) * 32;
                double fy = py + 42 + Math.sin(ang) * 18;
                gc.setFill(i == 0 ? Color.web("#ea580c") : (i == 1 ? Color.web("#facc15") : Color.web("#f97316")));
                gc.fillOval(fx - 4, fy - 2.5, 8, 5);
            }

            gc.setFill(Color.web("#78350f"));
            gc.fillRect(px + 12, py + 38, 104, 8);
            gc.setFill(Color.web("#a16207"));
            gc.fillRect(px + 12, py + 37, 104, 3);
            for (int i = 0; i < 7; i++) {
                gc.setFill(Color.web("#451a03"));
                gc.fillRect(px + 16 + i * 15, py + 35, 4, 13);
                gc.setFill(Color.web("#b45309"));
                gc.fillRect(px + 17 + i * 15, py + 35, 2, 13);
            }
            gc.setStroke(Color.web("#ca8a04"));
            gc.setLineWidth(2);
            gc.strokeLine(px + 12, py + 30, px + 116, py + 30);

            // 3. Hàng rào tre quanh ruộng & Rặng Dừa Bến Tre
            gc.setStroke(Color.web("#ca8a04"));
            gc.setLineWidth(2);
            gc.strokeRect(7.5 * GameData.TS, 5.3 * GameData.TS, 6 * GameData.TS, 3.4 * GameData.TS);

            drawCoconutTree(gc, 6.3 * GameData.TS, 4.2 * GameData.TS, nowSec);
            drawCoconutTree(gc, 6.2 * GameData.TS, 13.8 * GameData.TS, nowSec);
            drawCoconutTree(gc, 22.5 * GameData.TS, 4.5 * GameData.TS, nowSec);
            drawCoconutTree(gc, 24.5 * GameData.TS, 13.5 * GameData.TS, nowSec);
        } else if ("fields".equals(m)) {
            // Bù Nhìn Rơm đội nón lá
            double sx = 15 * GameData.TS + 16;
            double sy = 7 * GameData.TS + 16;
            gc.setFill(Color.web("#78350f"));
            gc.fillRect(sx - 2, sy - 8, 4, 22);
            gc.fillRect(sx - 14, sy - 2, 28, 3);
            gc.setFill(Color.web("#eab308"));
            gc.fillRoundRect(sx - 7, sy - 4, 14, 12, 4, 4);
            gc.setFill(Color.web("#fef08a"));
            gc.fillPolygon(new double[]{sx, sx - 13, sx + 13}, new double[]{sy - 19, sy - 9, sy - 9}, 3);
            drawCoconutTree(gc, 5 * GameData.TS, 4.5 * GameData.TS, nowSec);
            drawCoconutTree(gc, 24 * GameData.TS, 13 * GameData.TS, nowSec);
        } else if ("mines".equals(m)) {
            gc.setStroke(Color.web("#475569"));
            gc.setLineWidth(3);
            gc.strokeRect(13.3 * GameData.TS, 2 * GameData.TS, 1.4 * GameData.TS, 13 * GameData.TS);
            for (int y = 2 * GameData.TS; y < 15 * GameData.TS; y += 12) {
                gc.setFill(Color.web("#334155"));
                gc.fillRect(13.0 * GameData.TS, y, 2.0 * GameData.TS, 3.5);
            }
            // Xe goòng mỏ quặng vàng
            double cartX = 13.2 * GameData.TS, cartY = 7 * GameData.TS;
            gc.setFill(Color.web("#334155"));
            gc.fillRoundRect(cartX, cartY + 2, 1.6 * GameData.TS, 14, 4, 4);
            gc.setFill(Color.web("#facc15"));
            gc.fillOval(cartX + 8, cartY, 7, 7);
            gc.setFill(Color.web("#38bdf8"));
            gc.fillOval(cartX + 18, cartY - 1, 6, 6);
        } else if ("mountain".equals(m)) {
            double lx = 5 * GameData.TS + 32;
            double ly = 5 * GameData.TS + 32;
            double pulse = Math.sin(nowSec * 3) * 5;
            gc.setFill(Color.web("#1c1917"));
            gc.fillOval(lx - 38, ly - 30, 76, 60);
            RadialGradient lg = new RadialGradient(0, 0, lx, ly, 56 + pulse, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fef08a")),
                    new Stop(0.35, Color.web("#f97316")),
                    new Stop(0.7, Color.web("#dc2626")),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(lg);
            gc.fillOval(lx - (56 + pulse), ly - (56 + pulse), (56 + pulse) * 2, (56 + pulse) * 2);
        } else if ("sea".equals(m)) {
            drawCoconutTree(gc, 2.6 * GameData.TS, 4.5 * GameData.TS, nowSec);
            drawCoconutTree(gc, 2.8 * GameData.TS, 12.8 * GameData.TS, nowSec);
            // Ngọn Hải Đăng
            double lhx = 26 * GameData.TS;
            double lhy = 3 * GameData.TS;
            gc.setFill(Color.web("#334155"));
            gc.fillOval(lhx - 24, lhy + 10, 48, 20);
            gc.setFill(Color.web("#f8fafc"));
            gc.fillRect(lhx - 7, lhy - 22, 14, 38);
            gc.setFill(Color.web("#ef4444"));
            gc.fillRect(lhx - 7, lhy - 12, 14, 8);
            gc.fillRect(lhx - 7, lhy + 2, 14, 8);
            gc.setFill(Color.web("#fef08a"));
            gc.fillOval(lhx - 7, lhy - 28, 14, 14);
            gc.setFill(Color.web("#dc2626"));
            gc.fillPolygon(new double[]{lhx - 9, lhx, lhx + 9}, new double[]{lhy - 28, lhy - 37, lhy - 28}, 3);
        }
    }

    private void drawBiomeParticles(GraphicsContext gc, double nowSec) {
        String m = state.currentMap;
        Color pCol = "mines".equals(m) ? Color.rgb(56, 189, 248, 0.45)
                : "mist_forest".equals(m) ? Color.rgb(216, 180, 254, 0.55)
                : "mountain".equals(m) ? Color.rgb(251, 146, 60, 0.5)
                : Color.rgb(254, 240, 138, 0.55);
        gc.setFill(pCol);
        for (int i = 0; i < particles.length; i++) {
            double[] p = particles[i];
            p[0] = (p[0] + p[2] + GameData.CW) % GameData.CW;
            p[1] = (p[1] + p[3] + GameData.CH) % GameData.CH;
            double r = p[4] * (0.6 + 0.4 * Math.sin(nowSec * 2 + i));
            gc.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
        }
    }

    private void drawSkyAndLighting(GraphicsContext gc, double nowSec) {
        String m = state.currentMap;
        // Đàn cò trắng bay ngang cánh đồng Miền Tây
        if ("farm".equals(m) || "fields".equals(m) || "meadow".equals(m) || "lake".equals(m)) {
            gc.setStroke(Color.rgb(255, 255, 255, 0.78));
            gc.setLineWidth(1.8);
            for (int i = 0; i < 5; i++) {
                double bx = ((nowSec * (32 + i * 5) + i * 190) % (GameData.CW + 60)) - 30;
                double by = 45 + i * 65 + Math.sin(nowSec * 2 + i) * 6;
                double wing = Math.sin(nowSec * 7 + i) * 5;
                gc.strokeLine(bx - 7, by + wing, bx, by);
                gc.strokeLine(bx, by, bx + 7, by + wing);
            }
        }
        double cyc = (nowSec % 120.0) / 120.0;
        boolean isRaining = (cyc > 0.36 && cyc < 0.50) && !"mines".equals(m) && !"mountain".equals(m);
        if (isRaining) {
            gc.setStroke(Color.rgb(186, 230, 253, 0.42));
            gc.setLineWidth(1.2);
            for (int i = 0; i < 45; i++) {
                double rx = ((i * 73 - nowSec * 140) % GameData.CW + GameData.CW) % GameData.CW;
                double ry = ((i * 59 + nowSec * 520) % GameData.CH);
                gc.strokeLine(rx, ry, rx - 3, ry + 13);
            }
        }
        // Tia nắng vàng chiếu xiên (Volumetric Sunbeams / God-Rays)
        if (cyc <= 0.65 && !isRaining && !"mines".equals(m)) {
            for (int i = 0; i < 3; i++) {
                double rx = 140 + i * 290 + Math.sin(nowSec * 0.4 + i) * 35;
                LinearGradient rg = new LinearGradient(rx, 0, rx + 180, GameData.CH, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(254, 249, 195, 0.09)),
                        new Stop(0.6, Color.rgb(253, 224, 71, 0.04)),
                        new Stop(1, Color.TRANSPARENT));
                gc.setFill(rg);
                gc.fillPolygon(new double[]{rx - 45, rx + 55, rx + 240, rx + 110},
                        new double[]{0, 0, GameData.CH, GameData.CH}, 4);
            }
        }
        Color tint = null;
        if (cyc > 0.55 && cyc <= 0.72) {
            double k = Math.sin((cyc - 0.55) / 0.17 * Math.PI);
            tint = Color.rgb(194, 65, 12, k * 0.18);
        } else if (cyc > 0.72 && cyc <= 0.92) {
            double k = Math.sin((cyc - 0.72) / 0.20 * Math.PI);
            tint = Color.rgb(15, 23, 42, k * 0.32);
        }
        if (tint != null) {
            gc.setFill(tint);
            gc.fillRect(0, 0, GameData.CW, GameData.CH);
            RadialGradient lg = new RadialGradient(0, 0, state.player.px + 16, state.player.py + 16, 95, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(254, 240, 138, 0.22)),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(lg);
            gc.fillOval(state.player.px + 16 - 95, state.player.py + 16 - 95, 190, 190);
        }
    }

    private void drawPlots(GraphicsContext gc, double nowSec) {
        if (!"farm".equals(state.currentMap)) return;
        for (GameState.Plot p : state.plots) {
            double px = p.tx * GameData.TS;
            double py = p.ty * GameData.TS;
            gc.setFill(Color.web("#451a03"));
            gc.fillRoundRect(px + 1, py + 1, GameData.TS - 2, GameData.TS - 2, 8, 8);
            gc.setFill(p.crop != null ? Color.web("#4a2c11") : Color.web("#6b421b"));
            gc.fillRoundRect(px + 3, py + 3, GameData.TS - 6, GameData.TS - 6, 6, 6);

            gc.setFill(Color.rgb(0, 0, 0, 0.22));
            gc.fillRect(px + 6, py + 8, GameData.TS - 12, 2);
            gc.fillRect(px + 6, py + 15, GameData.TS - 12, 2);
            gc.fillRect(px + 6, py + 22, GameData.TS - 12, 2);

            if (p.crop != null) {
                double prog = p.max > 0 ? Math.max(0, Math.min(1.0, 1.0 - p.timer / p.max)) : 1.0;
                boolean ready = p.timer <= 0;
                double sway = Math.sin(nowSec * 3.8 + p.tx * 1.3 + p.ty) * (1.2 + prog * 1.5);
                double cx = px + GameData.TS / 2.0;
                double cy = py + GameData.TS / 2.0;

                if (ready) {
                    RadialGradient g = new RadialGradient(0, 0, cx, cy, 18, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.rgb(250, 204, 21, 0.65)),
                            new Stop(1, Color.TRANSPARENT));
                    gc.setFill(g);
                    gc.fillOval(cx - 18, cy - 18, 36, 36);
                }

                if (prog < 0.35) {
                    gc.setStroke(Color.web("#4ade80"));
                    gc.setLineWidth(2);
                    gc.strokeLine(cx, cy + 7, cx - 4 + sway, cy - 2);
                    gc.strokeLine(cx, cy + 7, cx + 4 + sway, cy - 3);
                } else if (prog < 0.99) {
                    gc.setStroke(Color.web("#16a34a"));
                    gc.setLineWidth(2.2);
                    gc.strokeLine(cx - 4, cy + 8, cx - 7 + sway, cy - 6);
                    gc.strokeLine(cx, cy + 8, cx + sway * 1.2, cy - 9);
                    gc.strokeLine(cx + 4, cy + 8, cx + 7 + sway, cy - 6);
                    if (prog > 0.65) {
                        gc.setFill("seed_lotus".equals(p.crop) ? Color.web("#f472b6") : Color.web("#facc15"));
                        gc.fillOval(cx + sway * 1.2 - 2.8, cy - 11.8, 5.6, 5.6);
                    }
                } else {
                    GameData.CropInfo ci = GameData.CROPS.get(p.crop);
                    String icon = (ci != null) ? ci.icon() : "🌾";
                    gc.setFont(Font.font("Serif", 21));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.setTextBaseline(VPos.CENTER);
                    gc.setFill(Color.WHITE);
                    gc.fillText(icon, cx + sway, cy);
                    double bx = cx + Math.cos(nowSec * 3.5 + p.tx) * 10;
                    double by = cy - 8 + Math.sin(nowSec * 5 + p.ty) * 4;
                    gc.setFill(Color.web("#fde047"));
                    gc.fillOval(bx - 3, by - 1.5, 3, 3);
                    gc.fillOval(bx, by - 1.5, 3, 3);
                }

                if (p.timer > 0 && p.max > 0) {
                    gc.setFill(Color.rgb(0, 0, 0, 0.65));
                    gc.fillRoundRect(px + 4, py + GameData.TS - 7, GameData.TS - 8, 4, 3, 3);
                    gc.setFill(prog > 0.66 ? Color.web("#facc15") : Color.web("#22c55e"));
                    gc.fillRoundRect(px + 4, py + GameData.TS - 7, (GameData.TS - 8) * prog, 4, 3, 3);
                }
            }
        }
    }

    private void drawPortals(GraphicsContext gc, double nowSec) {
        GameData.GameMap map = GameData.MAPS.get(state.currentMap);
        if (map == null) return;

        for (GameData.Portal pt : map.portals()) {
            double cx = pt.tx() * GameData.TS + GameData.TS / 2.0;
            double cy = pt.ty() * GameData.TS + GameData.TS / 2.0;
            double pulse = 0.55 + Math.sin(nowSec * 3.5) * 0.25;

            gc.setFill(Color.rgb(99, 102, 241, pulse));
            gc.fillOval(cx - 22, cy - 22, 44, 44);
            gc.setStroke(Color.web("#c7d2fe"));
            gc.setLineWidth(1.8);
            gc.strokeOval(cx - 14, cy - 14, 28, 28);
            gc.setStroke(Color.web("#38bdf8"));
            gc.setLineWidth(1.3);
            gc.strokeOval(cx - 9, cy - 9, 18, 18);

            double tw = pt.label().length() * 6.5 + 16;
            gc.setFill(Color.rgb(15, 23, 42, 0.9));
            gc.fillRoundRect(cx - tw / 2.0, cy - 32, tw, 16, 8, 8);
            gc.setFill(Color.web("#fef08a"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.BASELINE);
            gc.fillText(pt.label(), cx, cy - 21);
        }
    }

    private void drawWildPets(GraphicsContext gc, double nowSec) {
        for (GameState.WildPet wp : state.wildPets) {
            if (!wp.alive) continue;

            double cx = wp.px + GameData.TS / 2.0;
            double cy = wp.py + GameData.TS / 2.0;
            Color col = GameData.getElement(wp.tmpl.el()).col();
            double bob = "walk".equals(wp.aiState) ? Math.sin(wp.walkFrame * 3) * 3 : Math.sin(nowSec * 3 + wp.id) * 1.5;
            boolean isRare = "S".equals(wp.pot) || "A".equals(wp.pot);

            gc.setFill(Color.rgb(0, 0, 0, 0.42));
            gc.fillOval(cx - 12, wp.py + GameData.TS - 1, 24, 8);
            if (isRare) {
                gc.setStroke(Color.web("#fde047"));
                gc.setLineWidth(1.5);
                gc.strokeOval(cx - 15, wp.py + GameData.TS - 2, 30, 11);
            }

            RadialGradient gradient = new RadialGradient(0, 0, cx, cy + bob, isRare ? 32 : 26, false, CycleMethod.NO_CYCLE,
                    new Stop(0, (isRare ? Color.web("#facc15") : col).deriveColor(0, 1, 1, 0.53)),
                    new Stop(1, Color.TRANSPARENT));
            gc.setFill(gradient);
            gc.fillOval(cx - 26, cy + bob - 26, 52, 52);

            for (int i = 0; i < 3; i++) {
                double ang = nowSec * 2.5 + (i * Math.PI * 2 / 3.0) + wp.id;
                double r = 16 + Math.sin(nowSec * 4 + i) * 2;
                gc.setFill(isRare ? Color.web("#fde047") : col);
                gc.fillOval(cx + Math.cos(ang) * r - 1.8, cy + bob + Math.sin(ang) * r - 1.8, 3.6, 3.6);
            }

            int frameCol = "walk".equals(wp.aiState) ? ((int)(nowSec * 5) % 3) : 1;
            if (!drawPetSprite(gc, wp.tmpl.name(), cx - 24, cy + bob - 24, 48, 48, frameCol)) {
                gc.setFont(Font.font("Serif", 26));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.setFill(Color.WHITE);
                gc.fillText(wp.tmpl.icon(), cx, cy + bob);
            }

            gc.setFill(Color.web("#22c55e"));
            gc.fillRect(wp.px + 2, wp.py - 8, (GameData.TS - 4) * ((double) wp.hp / wp.maxHp), 4);

            if (wp.exclAlpha > 0 || isRare) {
                String tag = wp.tmpl.name() + " [" + wp.pot + "]";
                gc.setFill(Color.rgb(15, 23, 42, 0.85));
                gc.fillRoundRect(cx - 44, wp.py - 22, 88, 12, 6, 6);
                gc.setFill(GameData.POTS.getOrDefault(wp.pot, GameData.POTS.get("C")).col());
                gc.setFont(Font.font("System", FontWeight.BOLD, 8.5));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(tag, cx, wp.py - 13);
            }
        }
    }

    // Vẽ Nhân Vật Út (Áo Bà Ba + Khăn Rằn Nam Bộ + Nón Lá)
    private void drawPlayer(GraphicsContext gc, double nowSec) {
        double px = state.player.px;
        double py = state.player.py;
        double bob = state.player.moving ? Math.abs(Math.sin(state.player.walkFrame * 2)) * -2.5 : Math.sin(nowSec * 3) * 0.8;
        double legSwing = state.player.moving ? Math.sin(state.player.walkFrame * 2) * 3.5 : 0;
        double cx = px + GameData.TS / 2.0;
        double cy = py + bob;

        gc.setFill(Color.rgb(0, 0, 0, 0.38));
        gc.fillOval(cx - 10, py + GameData.TS - 3, 20, 8);

        // Quần đen & 2 chân bước đi
        gc.setFill(Color.web("#1e293b"));
        gc.fillRect(cx - 6, py + 22 + legSwing * 0.5, 5, 8);
        gc.fillRect(cx + 1, py + 22 - legSwing * 0.5, 5, 8);

        // Áo Bà Ba xanh
        gc.setFill(Color.web("#2563eb"));
        gc.fillRoundRect(cx - 9, cy + 11, 18, 13, 6, 6);
        gc.setFill(Color.web("#1d4ed8"));
        gc.fillRect(cx - 1, cy + 12, 2, 12);
        gc.setFill(Color.web("#fde047"));
        gc.fillRect(cx - 0.5, cy + 14, 1.5, 1.5);
        gc.fillRect(cx - 0.5, cy + 18, 1.5, 1.5);

        // Tay đung đưa
        gc.setFill(Color.web("#3b82f6"));
        gc.fillRect(cx - 12, cy + 12 - legSwing * 0.4, 4, 9);
        gc.fillRect(cx + 8, cy + 12 + legSwing * 0.4, 4, 9);

        // Khăn Rằn Nam Bộ
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRect(cx - 7, cy + 10, 14, 3);
        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(cx - 5, cy + 10, 2, 3);
        gc.fillRect(cx - 1, cy + 10, 2, 3);
        gc.fillRect(cx + 3, cy + 10, 2, 3);

        // Khuôn mặt
        gc.setFill(Color.web("#fed7aa"));
        gc.fillOval(cx - 7, cy - 2, 14, 14);
        gc.setFill(Color.web("#0f172a"));
        gc.fillRect(cx - 4, cy + 4, 2, 2.5);
        gc.fillRect(cx + 2, cy + 4, 2, 2.5);

        // Nón Lá
        gc.setFill(Color.web("#fef08a"));
        gc.fillPolygon(new double[]{cx, cx - 15, cx + 15}, new double[]{cy - 9, cy + 2, cy + 2}, 3);
        gc.setStroke(Color.web("#ca8a04"));
        gc.setLineWidth(1.2);
        gc.strokePolygon(new double[]{cx, cx - 15, cx + 15}, new double[]{cy - 9, cy + 2, cy + 2}, 3);
    }

    private void drawFollowerPet(GraphicsContext gc, double nowSec) {
        if (state.pets.isEmpty()) return;
        GameState.Pet p = state.pets.get(state.activeIdx);

        double fpx = state.player.px - 26;
        double fpy = state.player.py + 4;
        double bob = state.player.moving ? Math.sin(state.player.walkFrame * 2) * 3 : Math.sin(nowSec * 3) * 1.5;
        int frameCol = state.player.moving ? ((int)(nowSec * 6) % 3) : 1;

        if (!drawPetSprite(gc, p.name, fpx - 8, fpy - 8 + bob, 40, 40, frameCol)) {
            gc.setFont(Font.font("Serif", 22));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(Color.WHITE);
            gc.fillText(p.icon, fpx + 11, fpy + 11 + bob);
        }
    }

    private void hpBar(GraphicsContext gc, double x, double y, double w, double h, int hp, int maxHp) {
        gc.setFill(Color.rgb(15, 23, 42, 0.85));
        gc.fillRoundRect(x, y, w, h, h, h);
        gc.setStroke(Color.rgb(255, 255, 255, 0.15));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, h, h);

        double p = Math.max(0, Math.min(1.0, (double) hp / Math.max(1, maxHp)));
        if (p > 0) {
            Color startCol = p > 0.5 ? Color.web("#15803d") : (p > 0.25 ? Color.web("#b45309") : Color.web("#991b1b"));
            Color endCol = p > 0.5 ? Color.web("#4ade80") : (p > 0.25 ? Color.web("#fde047") : Color.web("#f87171"));
            LinearGradient barG = new LinearGradient(x, y, x + w * p, y, false, CycleMethod.NO_CYCLE,
                    new Stop(0, startCol), new Stop(1, endCol));
            gc.setFill(barG);
            gc.fillRoundRect(x + 1, y + 1, (w - 2) * p, h - 2, h - 2, h - 2);
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(9, h - 2)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(Math.max(0, hp) + " / " + maxHp, x + w / 2.0, y + h / 2.0);
    }

    private void drawBattle(GraphicsContext gc, double nowSec) {
        battleBtns.clear();
        double bx = 55, by = 22, bw = GameData.CW - 110, bh = GameData.CH - 44;

        LinearGradient skyGrad = new LinearGradient(bx, by, bx, by + bh * 0.62, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0f172a")),
                new Stop(0.6, Color.web("#1e1b4b")),
                new Stop(1, Color.web("#312e81")));
        gc.setFill(skyGrad);
        gc.fillRoundRect(bx, by, bw, bh, 24, 24);
        gc.setFill(Color.rgb(254, 249, 195, 0.18));
        gc.fillOval(bx + bw - 144, by + 28, 68, 68);
        gc.setFill(Color.web("#fef08a"));
        gc.fillOval(bx + bw - 130, by + 42, 40, 40);
        gc.setFill(Color.rgb(15, 23, 42, 0.55));
        gc.fillPolygon(new double[]{bx, bx + 180, bx + 380, bx + 610, bx + bw},
                new double[]{by + 195, by + 125, by + 180, by + 115, by + 195}, 5);
        gc.setFill(Color.rgb(7, 12, 24, 0.85));
        gc.fillRoundRect(bx, by + 185, bw, bh - 185, 24, 24);
        gc.setStroke(Color.rgb(129, 140, 248, 0.65));
        gc.setLineWidth(2);
        gc.strokeRoundRect(bx, by, bw, bh, 24, 24);

        gc.setFill(Color.web("#fde047"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BASELINE);
        gc.fillText("⚔️  SÀN ĐẤU LINH THÚ MIỀN TÂY  ⚔️", bx + bw / 2.0, by + 30);

        double pShakeX = state.battle.pShake > 0 ? (Math.random() - 0.5) * 12 : 0;
        double eShakeX = state.battle.eShake > 0 ? (Math.random() - 0.5) * 12 : 0;
        if (state.battle.pShake > 0) state.battle.pShake = Math.max(0, state.battle.pShake - 0.05);
        if (state.battle.eShake > 0) state.battle.eShake = Math.max(0, state.battle.eShake - 0.05);

        // Player Pet Panel
        GameState.Pet pp = state.battle.pet;
        GameData.ElementData pEl = GameData.getElement(pp.el);
        gc.setFill(state.battle.pShake > 0 ? Color.rgb(239, 68, 68, 0.18) : Color.rgb(255, 255, 255, 0.04));
        gc.fillRoundRect(bx + 25, by + 44, 290, 200, 18, 18);
        gc.setStroke(pEl.col().deriveColor(0, 1, 1, 0.45));
        gc.setLineWidth(1.8);
        gc.strokeRoundRect(bx + 25, by + 44, 290, 200, 18, 18);

        gc.setFill(pEl.col().deriveColor(0, 1, 1, 0.22));
        gc.fillOval(bx + 122, by + 128, 96, 28);

        if (!drawPetSprite(gc, pp.name, bx + 120 + pShakeX, by + 50 + Math.sin(nowSec * 4) * 3, 100, 100, (int)(nowSec * 3) % 3)) {
            gc.setFont(Font.font("Serif", 60));
            gc.setFill(Color.WHITE);
            gc.fillText(pp.icon, bx + 170 + pShakeX, by + 125);
        }
        gc.setFill(Color.web("#e2e8f0"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(pp.name + " [Tư chất " + pp.pot + "]", bx + 170, by + 166);

        gc.setFill(pEl.col());
        gc.fillRoundRect(bx + 110, by + 172, 120, 18, 12, 12);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText(pEl.icon() + " " + pp.el + " · Cấp " + pp.level, bx + 170, by + 185);
        hpBar(gc, bx + 80, by + 205, 180, 14, state.battle.playerHP, state.battle.playerMaxHP);

        // Enemy Pet Panel
        GameState.WildPet ep = state.battle.wildRef;
        GameData.ElementData eEl = GameData.getElement(ep.tmpl.el());
        gc.setFill(state.battle.eShake > 0 ? Color.rgb(239, 68, 68, 0.18) : Color.rgb(255, 255, 255, 0.04));
        gc.fillRoundRect(bx + bw - 315, by + 44, 290, 200, 18, 18);
        gc.setStroke(eEl.col().deriveColor(0, 1, 1, 0.45));
        gc.setLineWidth(1.8);
        gc.strokeRoundRect(bx + bw - 315, by + 44, 290, 200, 18, 18);

        gc.setFill(eEl.col().deriveColor(0, 1, 1, 0.22));
        gc.fillOval(bx + bw - 218, by + 128, 96, 28);

        if (!drawPetSprite(gc, ep.tmpl.name(), bx + bw - 220 + eShakeX, by + 50 + Math.cos(nowSec * 4) * 3, 100, 100, ((int)(nowSec * 3) + 1) % 3)) {
            gc.setFont(Font.font("Serif", 60));
            gc.setFill(Color.WHITE);
            gc.fillText(ep.tmpl.icon(), bx + bw - 170 + eShakeX, by + 125);
        }
        gc.setFill(Color.web("#fca5a5"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(ep.tmpl.name() + " [Tư chất " + ep.pot + "]", bx + bw - 170, by + 166);

        gc.setFill(eEl.col());
        gc.fillRoundRect(bx + bw - 230, by + 172, 120, 18, 12, 12);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 10));
        gc.fillText(eEl.icon() + " " + ep.tmpl.el() + " · Hoang Dã", bx + bw - 170, by + 185);
        hpBar(gc, bx + bw - 260, by + 205, 180, 14, state.battle.enemyHP, state.battle.enemyMaxHP);

        // VS & Elemental Counter Hint
        gc.setFill(Color.web("#facc15"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 26));
        gc.fillText("⚡VS⚡", bx + bw / 2.0, by + 125);
        double mult = GameData.getElMult(pp.el, ep.tmpl.el());
        if (mult > 1.1) {
            gc.setFill(Color.web("#4ade80"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText("🔥 Khắc Hệ (+45% ST)", bx + bw / 2.0, by + 150);
        } else if (mult < 0.9) {
            gc.setFill(Color.web("#f87171"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            gc.fillText("🛡️ Bị Khắc (-25% ST)", bx + bw / 2.0, by + 150);
        }

        // Draw Battle VFX
        for (int i = state.battle.vfx.size() - 1; i >= 0; i--) {
            GameState.BattleVFX v = state.battle.vfx.get(i);
            v.t += 0.018;
            double p = Math.min(1.0, v.t / v.dur);
            if ("skill".equals(v.type)) {
                double cx = v.sx + (v.tx - v.sx) * p;
                double cy = v.sy + (v.ty - v.sy) * p - Math.sin(p * Math.PI) * 35;
                gc.setFill(Color.web(v.colHex, 0.55));
                gc.fillOval(cx - 24, cy - 24, 48, 48);
                gc.setFont(Font.font("Serif", 26));
                gc.setFill(Color.WHITE);
                gc.fillText(GameData.getElement(v.el).icon(), cx, cy + 6);
                if (p > 0.65) {
                    double r = (p - 0.65) * 180;
                    gc.setStroke(Color.web(v.colHex));
                    gc.setLineWidth(3);
                    gc.strokeOval(v.tx - r, v.ty - r, r * 2, r * 2);
                }
            } else if ("slash".equals(v.type)) {
                double cx = v.sx + (v.tx - v.sx) * p;
                double cy = v.sy + (v.ty - v.sy) * p;
                gc.setStroke(Color.web("#fef08a"));
                gc.setLineWidth(4);
                gc.strokeLine(cx - 18, cy - 18, cx + 18, cy + 18);
                gc.strokeLine(cx + 18, cy - 18, cx - 18, cy + 18);
            } else if ("catch".equals(v.type)) {
                double cx = v.sx + (v.tx - v.sx) * p;
                double cy = v.sy + (v.ty - v.sy) * p - Math.sin(p * Math.PI) * 70;
                gc.setFont(Font.font("Serif", 32));
                gc.setFill(Color.WHITE);
                gc.fillText("🪤", cx, cy + 10);
            } else if ("popup".equals(v.type)) {
                gc.setFill(Color.web(v.colHex, Math.max(0, 1.0 - p * 0.9)));
                gc.setFont(Font.font("System", FontWeight.BOLD, 17));
                gc.fillText(v.text, v.sx, v.sy - p * 42);
            }
            if (v.t >= v.dur) state.battle.vfx.remove(i);
        }

        // Log Box
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRoundRect(bx + 25, by + 255, bw - 50, 36, 12, 12);
        gc.setFill(Color.web("#fde047"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 13));
        gc.fillText(state.battle.log, bx + bw / 2.0, by + 278);

        // 4 Action Buttons
        String skillLabel = GameData.PET_SKILLS.getOrDefault(pp.name, "✨ Tung Chiêu");
        String[] labels = {"⚔️ Đánh Thường", skillLabel, "🪤 Quăng Nơm", "🏃 Tẩu Thoát"};
        Color[][] btnGrads = {
                {Color.web("#b91c1c"), Color.web("#7f1d1d")},
                {Color.web("#7e22ce"), Color.web("#4c1d95")},
                {Color.web("#b45309"), Color.web("#78350f")},
                {Color.web("#334155"), Color.web("#1e293b")}
        };
        Color[] btnBorders = {Color.web("#f87171"), Color.web("#c084fc"), Color.web("#fbbf24"), Color.web("#94a3b8")};
        Runnable[] actions = {state::doBattleAttack, state::doBattleSkill, state::doBattleCatch, state::doBattleRun};

        for (int i = 0; i < 4; i++) {
            double bx2 = bx + 25 + i * (bw - 50) / 4.0 + 4;
            double bw4 = (bw - 50) / 4.0 - 8;
            double by2 = by + 302;
            LinearGradient bg = new LinearGradient(bx2, by2, bx2, by2 + 44, false, CycleMethod.NO_CYCLE,
                    new Stop(0, btnGrads[i][0]), new Stop(1, btnGrads[i][1]));
            gc.setFill(bg);
            gc.fillRoundRect(bx2, by2, bw4, 44, 14, 14);
            gc.setStroke(btnBorders[i].deriveColor(0, 1, 1, 0.45));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(bx2, by2, bw4, 44, 14, 14);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 12.5));
            gc.fillText(labels[i], bx2 + bw4 / 2.0, by2 + 27);
            battleBtns.add(new ClickBtn(bx2, by2, bw4, 44, actions[i]));
        }
    }

    private void drawMinimap(GraphicsContext gc) {
        double mmX = GameData.CW - 172;
        double mmY = GameData.CH - 124;
        double mmW = 160;
        double mmH = 112;
        double tW = mmW / GameData.COLS;
        double tH = mmH / GameData.ROWS;

        gc.setFill(Color.rgb(10, 16, 30, 0.85));
        gc.fillRoundRect(mmX - 4, mmY - 16, mmW + 8, mmH + 20, 10, 10);
        gc.setFill(Color.web("#9ca3af"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("BẢN ĐỒ", mmX + mmW / 2.0, mmY - 5);

        GameData.GameMap map = GameData.MAPS.get(state.currentMap);
        Color baseBg = (map != null) ? map.bg()[0] : Color.DARKGREEN;
        for (int ty = 0; ty < GameData.ROWS; ty++) {
            for (int tx = 0; tx < GameData.COLS; tx++) {
                gc.setFill((tx == 0 || tx == GameData.COLS - 1 || ty == 0 || ty == GameData.ROWS - 1) ? Color.web("#14532d") : baseBg);
                gc.fillRect(mmX + tx * tW, mmY + ty * tH, tW + 0.5, tH + 0.5);
            }
        }
        if ("farm".equals(state.currentMap)) {
            gc.setFill(Color.web("#ca8a04"));
            gc.fillRect(mmX + 2 * tW, mmY + 2 * tH, 4 * tW, 2.5 * tH);
            gc.setFill(Color.web("#0284c7"));
            gc.fillOval(mmX + 2 * tW, mmY + 11 * tH, 4 * tW, 2.5 * tH);
            for (GameState.Plot p : state.plots) {
                gc.setFill(p.crop != null ? (p.timer <= 0 ? Color.web("#facc15") : Color.web("#4ade80")) : Color.web("#78350f"));
                gc.fillRect(mmX + p.tx * tW, mmY + p.ty * tH, tW - 0.5, tH - 0.5);
            }
        }
        for (GameState.WildPet wp : state.wildPets) {
            if (wp.alive) {
                gc.setFill(GameData.getElement(wp.tmpl.el()).col());
                gc.fillOval(mmX + (wp.px / GameData.TS) * tW, mmY + (wp.py / GameData.TS) * tH, 4, 4);
            }
        }
        gc.setFill(Color.web("#fde047"));
        gc.fillOval(mmX + state.player.tx * tW - 1, mmY + state.player.ty * tH - 1, 6, 6);
    }

    private void drawNotification(GraphicsContext gc, String msg) {
        gc.setFill(Color.rgb(10, 16, 30, 0.95));
        double w = Math.max(240, msg.length() * 8.5 + 40);
        double x = (GameData.CW - w) / 2.0;
        gc.fillRoundRect(x, 54, w, 32, 24, 24);
        gc.setStroke(Color.rgb(250, 204, 21, 0.45));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, 54, w, 32, 24, 24);
        gc.setFill(Color.web("#fde047"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(msg, GameData.CW / 2.0, 70);
    }
}
