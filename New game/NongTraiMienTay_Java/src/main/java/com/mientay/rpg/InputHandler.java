package com.mientay.rpg;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

public class InputHandler {
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public void setKeyPressed(KeyCode code) {
        activeKeys.add(code);
    }

    public void setKeyReleased(KeyCode code) {
        activeKeys.remove(code);
    }

    public boolean isPressed(KeyCode code) {
        return activeKeys.contains(code);
    }
}
