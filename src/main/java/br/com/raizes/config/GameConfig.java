package br.com.raizes.config;

import java.awt.Color;

public final class GameConfig {
    private GameConfig() { }

    public static final int W = 1280, H = 720, WORLD_W = 3200;
    public static final int GROUND_Y = 600, GROUND_OVERDRAW = 12, GROUND_ART_SURFACE_OFFSET = 77, QUESTIONS_PER_ROUND = 5;
    public static final int PLAYER_WIDTH = 42, PLAYER_HEIGHT = 58, PLATFORM_SURFACE_OFFSET = 9;
    public static final int PLAYER_DRAW_WIDTH = 58, PLAYER_DRAW_HEIGHT = 78;
    public static final double ENEMY_SPEED = 1.4;
public static final Color[] PHASE_FILTERS = {
    new Color(42, 72, 35, 28), new Color(40, 62, 112, 38),
    new Color(116, 45, 28, 78), new Color(70, 74, 63, 58)
};
}
