package io.github.r4t2.nilum.paper.skript.types;

/** Which edge(s) of a Nilum keybind's press/release cycle a script wants to trigger on. */
public enum NilumKeyState {
    PRESS,
    RELEASE,
    BOTH;

    public boolean matches(boolean pressed) {
        return switch (this) {
            case PRESS -> pressed;
            case RELEASE -> !pressed;
            case BOTH -> true;
        };
    }
}
