package main;

public class SimplexOctaveGen2D extends OctaveGen2D<SimplexGen2D> {
    public SimplexOctaveGen2D(Long seed) {
        super(seed, SimplexGen2D.class);
    }
}
