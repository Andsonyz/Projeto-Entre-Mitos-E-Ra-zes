package br.com.raizes.character;

import java.util.Map;

/** One source of truth for availability, save IDs and animation resources. */
public enum CharacterDefinition {
    MALE("male", "Masculino", true),
    FEMALE("female", "Feminino", false);

    public record Sheet(String path, int columns, int rows) { }
    public record Animation(Sheet sheet, int firstFrame, int frameCount, double secondsPerFrame, boolean loop) { }

    private static final String MALE_PATH = "Image/Personagens/AventureiroMasculino/";
    private static final Sheet IDLE_SHEET = new Sheet(MALE_PATH + "PersonagemParado.png", 4, 4);
    private static final Sheet WALK_SHEET = new Sheet(MALE_PATH + "PersonagemAndando.png", 4, 4);
    private static final Sheet RUN_SHEET = new Sheet(MALE_PATH + "PersonagemCorrendo.png", 4, 4);
    private static final Sheet JUMP_SHEET = new Sheet(MALE_PATH + "PersonagemPulando.png", 4, 2);
    private static final Map<AnimationState, Animation> MALE_ANIMATIONS = Map.of(
        AnimationState.IDLE, new Animation(IDLE_SHEET, 0, 16, 0.180, true),
        AnimationState.WALK, new Animation(WALK_SHEET, 0, 16, 0.110, true),
        AnimationState.RUN, new Animation(RUN_SHEET, 0, 16, 0.070, true),
        AnimationState.JUMP, new Animation(JUMP_SHEET, 0, 4, 0.100, false),
        AnimationState.FALL, new Animation(JUMP_SHEET, 4, 4, 0.100, false));

    private final String id, label;
    private final boolean selectable;

    CharacterDefinition(String id, String label, boolean selectable) {
        this.id = id;
        this.label = label;
        this.selectable = selectable;
    }

    public String id() { return id; }
    public String label() { return label; }
    public boolean selectable() { return selectable; }

    public Map<AnimationState, Animation> animations() {
        return this == MALE ? MALE_ANIMATIONS : Map.of();
    }

    public static CharacterDefinition fromSave(String id) {
        for (CharacterDefinition character : values()) {
            if (character.selectable && character.id.equals(id)) return character;
        }
        return MALE;
    }
}
