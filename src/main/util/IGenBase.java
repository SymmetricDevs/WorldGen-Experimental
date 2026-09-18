package main.util;

import main.Vec2d;

import java.lang.reflect.Constructor;

public interface IGenBase {
    double base(Vec2d pos);
    long getSeed();

    public static <T extends IGenBase> T create(long seed, Class<?> clazz) {
        try {
            Constructor<?> cons = clazz.getConstructor(long.class);
            return (T)cons.newInstance(seed);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
