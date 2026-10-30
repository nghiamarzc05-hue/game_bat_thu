package com.mientay.rpg;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Map;

public class MainApp extends Application {

    private GameState state;
    private Label goldLbl;
    private Label zoneLbl;
    private Label petCountLbl;
    private StackPane modalOverlay;
    private VBox modalContentBox;
    private Label modalTitleLbl;

    @Override
    public void start(Stage primaryStage) {
        state = new GameState();
        boolean hasSave = state.loadGame();
        state.spawnWildPetsForMap(state.currentMap);

        InputHandler input = new InputHandler();
        Renderer renderer = new Renderer(state);

        Canvas canvas = new Canvas(GameData.CW, GameData.CH);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);

        canvas.setOnMouseClicked(e -> {
            double mx = e.getX();
            double my = e.getY();
            if (state.battle.active) {
                for (Renderer.ClickBtn b : renderer.battleBtns) {
                    if (mx >= b.x() && mx <= b.x() + b.w() && my >= b.y() && my <= b.y() + b.h()) {
                        b.fn().run();
                        break;
                    }
                }
                return;
            }
            int ttx = (int) Math.floor(mx / GameData.TS);
            int tty = (int) Math.floor(my / GameData.TS);
            if (ttx > 0 && ttx < GameData.COLS - 1 && tty > 0 && tty < GameData.ROWS - 1) {
                state.player.targetPx = (double) (ttx * GameData.TS);
                state.player.targetPy = (double) (tty * GameData.TS);
            }
        });

        // TOP HUD
        HBox topHud = buildTopHud();
        state.onHudUpdate = this::updateHudLabels;
        updateHudLabels();

        // MODAL OVERLAY
        modalOverlay = buildModalOverlay();
        modalOverlay.setVisible(false);

        Pane root = new Pane();
        root.setStyle("-fx-background-color: #0b0e17;");
        topHud.setLayoutX(10);
        topHud.setLayoutY(10);
        topHud.setPrefWidth(GameData.CW - 20);
        modalOverlay.setPrefSize(GameData.CW, GameData.CH);

        root.getChildren().addAll(canvas, topHud, modalOverlay);

        Scene scene = new Scene(root, GameData.CW, GameData.CH);
        scene.setOnKeyPressed(e -> input.setKeyPressed(e.getCode()));
        scene.setOnKeyReleased(e -> input.setKeyReleased(e.getCode()));

        primaryStage.setTitle("🌾 Nông Trại Miền Tây – RPG 2D (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        if (hasSave) {
            state.notifyMsg("💾 Đã tải dữ liệu game đã lưu!");
        } else {
            openModal("guide");
        }

        GameLoop loop = new GameLoop(state, input, renderer, gc);
        loop.start();
    }

    private HBox buildTopHud() {
        HBox hud = new HBox(10);
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPickOnBounds(false);

        goldLbl = createBadge("🪙 500", "#fde047");
        zoneLbl = createBadge("🏡 Nông Trại", "#d8b4fe");
        petCountLbl = createBadge("🐾 Thú bảo: 1", "#86efac");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMouseTransparent(true);

        Button btnGuide = createHudBtn("📖", "#60a5fa", () -> openModal("guide"));
        Button btnSave  = createHudBtn("💾", "#4ade80", () -> state.saveGame(false));
        Button btnReset = createHudBtn("🧹", "#f87171", () -> {
            state.resetSave();
            state = new GameState();
            state.spawnWildPetsForMap(state.currentMap);
            state.onHudUpdate = this::updateHudLabels;
            updateHudLabels();
            state.notifyMsg("🧹 Đã xóa save, làm lại cuộc đời!");
        });
        Button btnBag  = createHudBtn("🎒 Túi", "#fbbf24", () -> openModal("bag"));
        Button btnCook = createHudBtn("🍳 Bếp", "#fb923c", () -> openModal("cook"));
        Button btnPets = createHudBtn("🐾 Thú", "#a78bfa", () -> openModal("pets"));
        Button btnShop = createHudBtn("🏪 Tiệm", "#34d399", () -> openModal("shop"));

        hud.getChildren().addAll(goldLbl, zoneLbl, petCountLbl, spacer, btnGuide, btnSave, btnReset, btnBag, btnCook, btnPets, btnShop);
        return hud;
    }

    private Label createBadge(String text, String colorHex) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color: rgba(10,16,30,0.9); -fx-text-fill: " + colorHex + "; -fx-padding: 6 14 6 14; -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
        return lbl;
    }

    private Button createHudBtn(String text, String colorHex, Runnable action) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: rgba(10,16,30,0.9); -fx-text-fill: " + colorHex + "; -fx-background-radius: 18; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 18; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        b.setOnAction(e -> action.run());
        b.setFocusTraversable(false);
        return b;
    }

    private void updateHudLabels() {
        goldLbl.setText("🪙 " + state.gold);
        GameData.GameMap m = GameData.MAPS.get(state.currentMap);
        zoneLbl.setText(m != null ? m.name() : state.currentMap);
        petCountLbl.setText("🐾 Thú bảo: " + state.pets.size());
    }

    private StackPane buildModalOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.72);");

        VBox box = new VBox(10);
        box.setMaxSize(460, 440);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 16;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        modalTitleLbl = new Label();
        modalTitleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        modalTitleLbl.setTextFill(Color.WHITE);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button closeBtn = new Button("✖");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> overlay.setVisible(false));
        closeBtn.setFocusTraversable(false);

        header.getChildren().addAll(modalTitleLbl, sp, closeBtn);

        modalContentBox = new VBox(8);
        modalContentBox.setPadding(new Insets(4));

        ScrollPane scroll = new ScrollPane(modalContentBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(header, scroll);
        overlay.getChildren().add(box);
        return overlay;
    }

    private void openModal(String type) {
        modalContentBox.getChildren().clear();
        modalOverlay.setVisible(true);
        switch (type) {
            case "bag"   -> renderBagModal();
            case "cook"  -> renderCookModal();
            case "pets"  -> renderPetsModal();
            case "shop"  -> renderShopModal();
            case "guide" -> renderGuideModal();
        }
    }

    private void renderBagModal() {
        modalTitleLbl.setText("🎒 Túi Đồ");
        boolean empty = true;
        for (String k : new String[]{"rice", "corn", "banana", "lotus"}) {
            int cnt = state.inv.getOrDefault(k, 0);
            if (cnt > 0) {
                empty = false;
                GameData.CropInfo ci = GameData.CROPS.get(k);
                HBox row = createItemRow(ci.icon() + " " + ci.name(), "Số lượng: " + cnt, "Bán (+" + ci.sellPrice() + "đ)", "#92400e", () -> {
                    state.inv.put(k, cnt - 1);
                    state.gold += ci.sellPrice();
                    updateHudLabels();
                    state.notifyMsg("💰 Bán nông sản thành công!");
                    renderBagModal();
                });
                modalContentBox.getChildren().add(row);
            }
        }
        for (String k : new String[]{"seed_rice", "seed_corn", "seed_banana", "seed_lotus"}) {
            int cnt = state.inv.getOrDefault(k, 0);
            if (cnt > 0) {
                empty = false;
                GameData.CropInfo ci = GameData.CROPS.get(k);
                modalContentBox.getChildren().add(createItemRow(ci.icon() + " " + ci.name(), "Hạt giống x" + cnt, null, null, null));
            }
        }
        for (Map.Entry<String, GameData.FoodInfo> entry : GameData.FOODS.entrySet()) {
            String k = entry.getKey();
            GameData.FoodInfo f = entry.getValue();
            int cnt = state.inv.getOrDefault(k, 0);
            if (cnt > 0) {
                empty = false;
                modalContentBox.getChildren().add(createItemRow(f.icon() + " " + f.name(), "x" + cnt + " · " + f.desc(), "Cho Ăn", "#15803d", () -> {
                    GameState.Pet pet = state.pets.get(state.activeIdx);
                    if (pet.hp >= pet.maxHp) {
                        state.notifyMsg(pet.icon + " Máu đầy rồi!");
                        return;
                    }
                    state.inv.put(k, cnt - 1);
                    if ("che_sen".equals(k)) pet.maxHp += 10;
                    pet.hp = Math.min(pet.maxHp, pet.hp + f.heal());
                    state.notifyMsg(f.icon() + " " + pet.name + " ăn " + f.name() + "!");
                    renderBagModal();
                }));
            }
        }
        if (empty) {
            Label l = new Label("Túi đồ đang trống!");
            l.setTextFill(Color.GRAY);
            modalContentBox.getChildren().add(l);
        }
    }

    private void renderCookModal() {
        modalTitleLbl.setText("🍳 Bếp Bà Tư");
        modalContentBox.getChildren().clear();
        for (Map.Entry<String, GameData.FoodInfo> entry : GameData.FOODS.entrySet()) {
            String k = entry.getKey();
            GameData.FoodInfo f = entry.getValue();
            boolean canCook = true;
            StringBuilder recStr = new StringBuilder();
            for (Map.Entry<String, Integer> r : f.recipe().entrySet()) {
                if (state.inv.getOrDefault(r.getKey(), 0) < r.getValue()) canCook = false;
                recStr.append(r.getValue()).append("x ").append(GameData.CROPS.get(r.getKey()).name()).append(" ");
            }
            int owned = state.inv.getOrDefault(k, 0);
            modalContentBox.getChildren().add(createItemRow(
                    f.icon() + " " + f.name() + " (" + f.desc() + ")",
                    "Cần: " + recStr + (owned > 0 ? " | Đang có: " + owned : ""),
                    canCook ? "🍳 Nấu" : null,
                    "#c2410c",
                    canCook ? () -> {
                        for (Map.Entry<String, Integer> r : f.recipe().entrySet()) {
                            state.inv.put(r.getKey(), state.inv.get(r.getKey()) - r.getValue());
                        }
                        state.inv.put(k, state.inv.getOrDefault(k, 0) + 1);
                        state.notifyMsg("🍳 Nấu xong " + f.name() + "!");
                        renderCookModal();
                    } : null
            ));
        }
    }

    private void renderPetsModal() {
        modalTitleLbl.setText("🐾 Chuồng Linh Thú (" + state.pets.size() + " con)");
        modalContentBox.getChildren().clear();
        for (int i = 0; i < state.pets.size(); i++) {
            int idx = i;
            GameState.Pet p = state.pets.get(i);
            boolean isAct = (i == state.activeIdx);
            HBox row = createItemRow(
                    p.icon + " " + p.name + " [Tư chất " + p.pot + " - " + p.el + "]",
                    "Cấp " + p.level + " | ATK " + p.atk + " | HP " + p.hp + "/" + p.maxHp,
                    "Xem Thẻ Bài",
                    isAct ? "#15803d" : "#4c1d95",
                    () -> renderPetDetailModal(idx)
            );
            modalContentBox.getChildren().add(row);
        }
    }

    private void renderPetDetailModal(int idx) {
        GameState.Pet p = state.pets.get(idx);
        boolean isActive = (idx == state.activeIdx);
        modalTitleLbl.setText("🐾 Chi Tiết: " + p.name);
        modalContentBox.getChildren().clear();

        Button backBtn = new Button("⬅ Trở lại danh sách");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> renderPetsModal());
        modalContentBox.getChildren().add(backBtn);

        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        String elHex = GameData.getElement(p.el).hex();
        card.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 14; -fx-border-color: " + elHex + "; -fx-border-radius: 14;");

        Image spriteImg = GameData.getPetSprite(p.name);
        if (spriteImg != null) {
            ImageView iv = new ImageView(spriteImg);
            iv.setFitWidth(240);
            iv.setFitHeight(80);
            iv.setPreserveRatio(true);
            card.getChildren().add(iv);
        } else {
            Label ico = new Label(p.icon);
            ico.setFont(Font.font("Serif", 48));
            card.getChildren().add(ico);
        }

        Label nameLbl = new Label(p.name + "  [" + GameData.getElement(p.el).icon() + " Hệ " + p.el + " · Tư chất " + p.pot + "]");
        nameLbl.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLbl.setTextFill(Color.web("#d8b4fe"));

        Label statsLbl = new Label(
                "⭐ Cấp độ: " + p.level + " (EXP: " + p.exp + "/" + p.expNext + ")\n" +
                "⚔️ Lực tấn công (ATK): " + p.atk + "\n" +
                "❤️ Máu (HP): " + p.hp + " / " + p.maxHp
        );
        statsLbl.setTextFill(Color.WHITE);
        statsLbl.setFont(Font.font("System", 13));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        int sellPrice = p.level * 25 + (int) Math.floor(GameData.POTS.getOrDefault(p.pot, GameData.POTS.get("C")).rate() * 60);

        if (!isActive) {
            Button btnSelect = new Button("▶ Xuất trận");
            btnSelect.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            btnSelect.setOnAction(e -> {
                state.activeIdx = idx;
                state.notifyMsg("✅ Đã chọn " + p.name + " xuất trận!");
                state.saveGame(true);
                renderPetDetailModal(idx);
            });
            Button btnSell = new Button("🪙 Bán (" + sellPrice + "đ)");
            btnSell.setStyle("-fx-background-color: #991b1b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            btnSell.setOnAction(e -> {
                state.gold += sellPrice;
                state.pets.remove(idx);
                if (state.activeIdx >= state.pets.size()) state.activeIdx = Math.max(0, state.pets.size() - 1);
                updateHudLabels();
                state.notifyMsg("💰 Đã bán " + p.name + " thu về " + sellPrice + " Tiền!");
                state.saveGame(true);
                renderPetsModal();
            });
            actions.getChildren().addAll(btnSelect, btnSell);
        } else {
            Label activeTag = new Label("✓ Đang xuất trận cùng Út");
            activeTag.setTextFill(Color.web("#4ade80"));
            activeTag.setFont(Font.font("System", FontWeight.BOLD, 13));
            actions.getChildren().add(activeTag);
        }

        card.getChildren().addAll(nameLbl, statsLbl, actions);
        modalContentBox.getChildren().add(card);
    }

    private void renderShopModal() {
        modalTitleLbl.setText("🏪 Tiệm Hạt Giống");
        modalContentBox.getChildren().clear();
        for (String k : new String[]{"seed_rice", "seed_corn", "seed_banana", "seed_lotus"}) {
            GameData.CropInfo ci = GameData.CROPS.get(k);
            modalContentBox.getChildren().add(createItemRow(
                    ci.icon() + " " + ci.name(),
                    ci.price() + " Tiền · Chín sau " + (int) ci.time() + "s",
                    "Mua",
                    "#15803d",
                    () -> {
                        if (state.gold >= ci.price()) {
                            state.gold -= ci.price();
                            state.inv.put(k, state.inv.getOrDefault(k, 0) + 1);
                            updateHudLabels();
                            state.notifyMsg("✅ Mua thành công " + ci.name() + "!");
                            state.saveGame(true);
                        } else {
                            state.notifyMsg("❌ Không đủ Tiền rồi Út ơi!");
                        }
                    }
            ));
        }
    }

    private void renderGuideModal() {
        modalTitleLbl.setText("📖 Cẩm Nang Tân Thủ Miền Tây");
        Label guide = new Label(
                "👋 Chào mừng Út về với Nông Trại Miền Tây!\n\n" +
                "1. DI CHUYỂN & TƯƠNG TÁC:\n" +
                "   • Dùng phím W, A, S, D (hoặc mũi tên / click chuột) để đi lại.\n" +
                "   • Đứng lên ô đất nâu bấm SPACE (hoặc E) để gieo hạt & thu hoạch.\n" +
                "   • Đứng vào vòng tròn xanh bấm SPACE để qua bản đồ mới.\n\n" +
                "2. NẤU ĂN & NUÔI THÚ BẢO:\n" +
                "   • Thu hoạch Lúa, Bắp, Chuối, Sen ➔ Mở Bếp Bà Tư nấu món ngon.\n" +
                "   • Đụng thú hoang ngoài bản đồ để giao đấu & Quăng Nơm bắt về Chuồng!\n" +
                "   • Tư chất Thú trải từ F đến S (Cực phẩm). Đủ bộ 16 Linh Thú có Sprite riêng!"
        );
        guide.setTextFill(Color.web("#e2e8f0"));
        guide.setWrapText(true);
        guide.setFont(Font.font("System", 13));
        modalContentBox.getChildren().add(guide);
    }

    private HBox createItemRow(String title, String sub, String btnText, String btnColor, Runnable onBtn) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;");

        VBox info = new VBox(3);
        Label t = new Label(title);
        t.setTextFill(Color.WHITE);
        t.setFont(Font.font("System", FontWeight.BOLD, 13));
        Label s = new Label(sub);
        s.setTextFill(Color.web("#94a3b8"));
        s.setFont(Font.font("System", 11));
        info.getChildren().addAll(t, s);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(info, spacer);

        if (btnText != null && onBtn != null) {
            Button b = new Button(btnText);
            b.setStyle("-fx-background-color: " + btnColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
            b.setOnAction(e -> onBtn.run());
            b.setFocusTraversable(false);
            row.getChildren().add(b);
        }
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
