package com.mientay.rpg;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameData {
    public static final int TS = 32;
    public static final int COLS = 30;
    public static final int ROWS = 17;
    public static final int CW = 960;
    public static final int CH = 544;

    public record ElementData(Color col, Color glow, String hex, String icon) {}

    public static final Map<String, ElementData> ELEMENTS = Map.of(
            "Lôi",  new ElementData(Color.web("#facc15"), Color.web("rgba(250,204,21,.35)"), "#facc15", "⚡"),
            "Thổ",  new ElementData(Color.web("#fb923c"), Color.web("rgba(251,146,60,.35)"), "#fb923c", "⛰️"),
            "Hỏa",  new ElementData(Color.web("#f87171"), Color.web("rgba(248,113,113,.35)"), "#f87171", "🔥"),
            "Thủy", new ElementData(Color.web("#38bdf8"), Color.web("rgba(56,189,248,.35)"), "#38bdf8", "💧"),
            "Mộc",  new ElementData(Color.web("#4ade80"), Color.web("rgba(74,222,128,.35)"), "#4ade80", "🌿"),
            "ĐB",   new ElementData(Color.web("#e879f9"), Color.web("rgba(232,121,249,.35)"), "#e879f9", "✨")
    );

    public static ElementData getElement(String el) {
        return ELEMENTS.getOrDefault(el, new ElementData(Color.web("#94a3b8"), Color.web("rgba(148,163,184,.25)"), "#94a3b8", "❓"));
    }

    public static final Map<String, String> PET_SKILLS = Map.ofEntries(
            Map.entry("Mèo Mướp Vườn", "🌿 Vuốt Dây Leo"),
            Map.entry("Gà Đá Chọi", "⚡ Cựa Sét Cao Lãnh"),
            Map.entry("Chó Cỏ Giữ Nhà", "⛰️ Địa Chấn Đầu Ngõ"),
            Map.entry("Hổ Vằn Rừng", "🔥 Hống Lửa Sơn Lâm"),
            Map.entry("Dơi Sương Mù", "⚡ Cánh Chớp Đêm Mù"),
            Map.entry("Vượn Hú Đêm", "🌿 Rừng Tràm Cuộn Gió"),
            Map.entry("Ếch Cốm Sen", "💧 Bọt Nước Đồng Sen"),
            Map.entry("Rùa Cổ Hồ", "💧 Mai Thần Dậy Sóng"),
            Map.entry("Quái Đá Nhỏ", "⛰️ Cú Đấm Thạch Anh"),
            Map.entry("Chuột Chũi Đào Đá", "⛰️ Khoan Đất Ngầm"),
            Map.entry("Rồng Thần Cổ", "✨ Long Thần Cửu Long"),
            Map.entry("Trâu Đồng Bắp", "⛰️ Cặp Sừng Phá Đất"),
            Map.entry("Thỏ Nhảy Xanh", "🌿 Phi Cước Lá Xanh"),
            Map.entry("Cua Cát Nhanh", "💧 Càng Kẹp Sóng Biển"),
            Map.entry("Cá Mập Cát", "💧 Xoáy Nước Biển Đông"),
            Map.entry("Bạch Tuộc Sóng", "💧 Vòi Cuộn Thủy Triều")
    );

    public static final Map<String, String> EL_COUNTER = Map.of(
            "Thủy", "Hỏa", "Hỏa", "Mộc", "Mộc", "Thổ", "Thổ", "Lôi", "Lôi", "Thủy"
    );

    public static double getElMult(String atkEl, String defEl) {
        if ("ĐB".equals(atkEl)) return 1.25;
        if (defEl.equals(EL_COUNTER.get(atkEl))) return 1.45;
        if (atkEl.equals(EL_COUNTER.get(defEl))) return 0.75;
        return 1.0;
    }

    // 16 Pet Sprite Map (240x80, 3 frames of 80x80)
    public static final Map<String, String> PET_SPRITE_MAP = Map.ofEntries(
            Map.entry("Mèo Mướp Vườn", "meo_muop_vuon"),
            Map.entry("Gà Đá Chọi", "ga_da_choi"),
            Map.entry("Chó Cỏ Giữ Nhà", "cho_co_giu_nha"),
            Map.entry("Hổ Vằn Rừng", "ho_van_rung"),
            Map.entry("Dơi Sương Mù", "doi_suong_mu"),
            Map.entry("Vượn Hú Đêm", "vuon_hu_dem"),
            Map.entry("Ếch Cốm Sen", "ech_com_sen"),
            Map.entry("Rùa Cổ Hồ", "rua_co_ho"),
            Map.entry("Quái Đá Nhỏ", "quai_da_nho"),
            Map.entry("Chuột Chũi Đào Đá", "chuot_chui_dao_da"),
            Map.entry("Rồng Thần Cổ", "rong_than_co"),
            Map.entry("Trâu Đồng Bắp", "trau_dong_bap"),
            Map.entry("Thỏ Nhảy Xanh", "tho_nhay_xanh"),
            Map.entry("Cua Cát Nhanh", "cua_cat_nhanh"),
            Map.entry("Cá Mập Cát", "ca_map_cat"),
            Map.entry("Bạch Tuộc Sóng", "bach_tuoc_song")
    );

    private static final Map<String, Image> LOADED_SPRITES = new HashMap<>();

    public static Image getPetSprite(String petName) {
        String key = PET_SPRITE_MAP.get(petName);
        if (key == null) return null;
        return LOADED_SPRITES.computeIfAbsent(key, k -> {
            try {
                InputStream is = GameData.class.getResourceAsStream("/assets/pets/" + k + ".png");
                if (is != null) {
                    Image raw = new Image(is);
                    int w = (int) raw.getWidth();
                    int h = (int) raw.getHeight();
                    PixelReader pr = raw.getPixelReader();
                    if (pr == null || w <= 0 || h <= 0) return raw;
                    WritableImage clean = new WritableImage(w, h);
                    PixelWriter pw = clean.getPixelWriter();
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int argb = pr.getArgb(x, y);
                            int a = (argb >> 24) & 0xff;
                            if (a == 0) continue;
                            int r = (argb >> 16) & 0xff;
                            int g = (argb >> 8) & 0xff;
                            int b = argb & 0xff;
                            int lum = (r + g + b) / 3;
                            int sat = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
                            if (lum > 232 && sat < 22) {
                                pw.setArgb(x, y, 0);
                            } else if (lum > 205 && sat < 30) {
                                int na = Math.max(0, Math.min(255, ((232 - lum) * 255) / 27));
                                int nr = Math.max(0, r - 35);
                                int ng = Math.max(0, g - 35);
                                int nb = Math.max(0, b - 35);
                                pw.setArgb(x, y, (Math.min(a, na) << 24) | (nr << 16) | (ng << 8) | nb);
                            } else {
                                int nr = Math.min(255, Math.max(0, (int) ((r - 128) * 1.12 + 128)));
                                int ng = Math.min(255, Math.max(0, (int) ((g - 128) * 1.12 + 128)));
                                int nb = Math.min(255, Math.max(0, (int) ((b - 128) * 1.12 + 128)));
                                pw.setArgb(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                            }
                        }
                    }
                    return clean;
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    public record CropInfo(String id, String name, String icon, String type, int price, double time, String yieldId, int amt, int sellPrice) {}
    public static final Map<String, CropInfo> CROPS = new LinkedHashMap<>();
    static {
        CROPS.put("seed_rice",   new CropInfo("seed_rice",   "Hạt Lúa Vàng",   "🌾", "seed", 15,  6.0,  "rice",   2, 0));
        CROPS.put("seed_corn",   new CropInfo("seed_corn",   "Hạt Bắp Nếp",    "🌽", "seed", 25,  9.0,  "corn",   1, 0));
        CROPS.put("seed_banana", new CropInfo("seed_banana", "Cây Chuối Xiêm", "🍌", "seed", 40, 14.0,  "banana", 1, 0));
        CROPS.put("seed_lotus",  new CropInfo("seed_lotus",  "Củ Sen Hồng",    "🪷", "seed", 60, 20.0,  "lotus",  1, 0));
        CROPS.put("rice",        new CropInfo("rice",        "Bông Lúa",       "🌾", "crop", 0,   0.0,  null,     0, 30));
        CROPS.put("corn",        new CropInfo("corn",        "Trái Bắp",       "🌽", "crop", 0,   0.0,  null,     0, 48));
        CROPS.put("banana",      new CropInfo("banana",      "Nải Chuối",      "🍌", "crop", 0,   0.0,  null,     0, 78));
        CROPS.put("lotus",       new CropInfo("lotus",       "Bông Sen",       "🪷", "crop", 0,   0.0,  null,     0, 115));
    }

    public record FoodInfo(String id, String name, String icon, int heal, Map<String, Integer> recipe, String desc) {}
    public static final Map<String, FoodInfo> FOODS = new LinkedHashMap<>();
    static {
        FOODS.put("com_lua",     new FoodInfo("com_lua",     "Cơm Lúa Trắng",   "🍚", 35,  Map.of("rice", 2),   "Hồi 35 máu"));
        FOODS.put("banh_bap",    new FoodInfo("banh_bap",    "Bánh Bắp Ngọt",   "🌽", 50,  Map.of("corn", 2),   "Hồi 50 máu"));
        FOODS.put("chuoi_nuong", new FoodInfo("chuoi_nuong", "Chuối Nướng Mật", "🍌", 70,  Map.of("banana", 1), "Hồi 70 máu"));
        FOODS.put("che_sen",     new FoodInfo("che_sen",     "Chè Sen Nhung",   "🍵", 100, Map.of("lotus", 2),  "Hồi 100 máu & +10 MaxHP"));
    }

    public record Portal(int tx, int ty, String to, String label) {}
    public record GameMap(String id, String name, Color[] bg, List<Portal> portals) {}

    public static final Map<String, GameMap> MAPS = Map.of(
            "farm", new GameMap("farm", "🏡 Nông Trại", new Color[]{Color.rgb(42,98,50), Color.rgb(36,88,44), Color.rgb(30,78,38)}, List.of(
                    new Portal(14, 1, "fields", "🌾 Đồng Ruộng"), new Portal(28, 8, "mist_forest", "🌫️ Rừng Mù"), new Portal(14, 15, "lake", "🪷 Hồ Sen"), new Portal(1, 8, "mines", "⛏️ Hầm Mỏ")
            )),
            "fields", new GameMap("fields", "🌾 Đồng Ruộng", new Color[]{Color.rgb(72,118,28), Color.rgb(66,110,22), Color.rgb(78,126,34)}, List.of(
                    new Portal(14, 15, "farm", "🏡 Về Nhà"), new Portal(28, 8, "meadow", "🌿 Đồng Cỏ")
            )),
            "mist_forest", new GameMap("mist_forest", "🌫️ Rừng Sương Mù", new Color[]{Color.rgb(44,54,72), Color.rgb(38,48,66), Color.rgb(50,60,80)}, List.of(
                    new Portal(1, 8, "farm", "🏡 Về Nhà")
            )),
            "lake", new GameMap("lake", "🪷 Hồ Sen", new Color[]{Color.rgb(18,98,115), Color.rgb(14,92,108), Color.rgb(22,104,122)}, List.of(
                    new Portal(14, 1, "farm", "🏡 Về Nhà"), new Portal(28, 8, "sea", "🌊 Biển")
            )),
            "mines", new GameMap("mines", "⛏️ Hầm Mỏ", new Color[]{Color.rgb(32,36,44), Color.rgb(27,31,40), Color.rgb(37,41,50)}, List.of(
                    new Portal(28, 8, "farm", "🏡 Về Nhà"), new Portal(14, 1, "mountain", "🏔️ Núi Cao")
            )),
            "mountain", new GameMap("mountain", "🏔️ Núi Cao", new Color[]{Color.rgb(62,48,44), Color.rgb(57,43,39), Color.rgb(67,53,49)}, List.of(
                    new Portal(14, 15, "mines", "⛏️ Xuống Mỏ")
            )),
            "meadow", new GameMap("meadow", "🌿 Đồng Cỏ", new Color[]{Color.rgb(50,120,55), Color.rgb(44,112,48), Color.rgb(56,128,62)}, List.of(
                    new Portal(1, 8, "fields", "🌾 Đồng Ruộng")
            )),
            "sea", new GameMap("sea", "🌊 Biển Khơi", new Color[]{Color.rgb(12,82,165), Color.rgb(9,77,158), Color.rgb(15,87,172)}, List.of(
                    new Portal(1, 8, "lake", "🪷 Về Hồ")
            ))
    );

    public record PetTemplate(String name, String icon, String el, int hp, int atk) {}
    public static final Map<String, List<PetTemplate>> WILD_TEMPLATES = Map.of(
            "farm", List.of(),
            "fields", List.of(new PetTemplate("Gà Đá Chọi", "🐓", "Lôi", 75, 18), new PetTemplate("Chó Cỏ Giữ Nhà", "🐕", "Thổ", 90, 20), new PetTemplate("Mèo Mướp Vườn", "🐱", "Mộc", 80, 17)),
            "mist_forest", List.of(new PetTemplate("Dơi Sương Mù", "🦇", "Lôi", 85, 22), new PetTemplate("Vượn Hú Đêm", "🐒", "Mộc", 105, 24)),
            "lake", List.of(new PetTemplate("Ếch Cốm Sen", "🐸", "Thủy", 85, 18), new PetTemplate("Rùa Cổ Hồ", "🐢", "Thủy", 130, 15)),
            "mines", List.of(new PetTemplate("Quái Đá Nhỏ", "🗿", "Thổ", 150, 16), new PetTemplate("Chuột Chũi Đào Đá", "🐀", "Thổ", 95, 21)),
            "mountain", List.of(new PetTemplate("Hổ Vằn Rừng", "🐅", "Hỏa", 160, 35), new PetTemplate("Rồng Thần Cổ", "🐉", "ĐB", 250, 50)),
            "meadow", List.of(new PetTemplate("Trâu Đồng Bắp", "🐃", "Thổ", 160, 22), new PetTemplate("Thỏ Nhảy Xanh", "🐇", "Mộc", 75, 16)),
            "sea", List.of(new PetTemplate("Cua Cát Nhanh", "🦀", "Thủy", 90, 19), new PetTemplate("Cá Mập Cát", "🦈", "Thủy", 170, 32), new PetTemplate("Bạch Tuộc Sóng", "🐙", "Thủy", 135, 26))
    );

    public record Pot(String col, double rate, String text, double baseMult) {}
    public static final Map<String, Pot> POTS = Map.of(
            "S", new Pot("#facc15", 1.15, "Cực phẩm", 1.25),
            "A", new Pot("#f87171", 1.12, "Hiếm có", 1.15),
            "B", new Pot("#a78bfa", 1.10, "Ưu tú", 1.00),
            "C", new Pot("#60a5fa", 1.08, "Khá giỏi", 0.90),
            "D", new Pot("#4ade80", 1.06, "Bình thường", 0.80),
            "E", new Pot("#94a3b8", 1.05, "Yếu ớt", 0.75),
            "F", new Pot("#64748b", 1.04, "Kém cỏi", 0.70)
    );

    public static String getRandPot() {
        double r = Math.random() * 100;
        if (r < 1) return "S";
        if (r < 5) return "A";
        if (r < 15) return "B";
        if (r < 35) return "C";
        if (r < 60) return "D";
        if (r < 85) return "E";
        return "F";
    }
}
