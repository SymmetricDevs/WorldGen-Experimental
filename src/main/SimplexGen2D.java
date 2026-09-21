package main;

import main.util.IGenBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class SimplexGen2D implements IGenBase {
    private long seed = 0;
    public final double F = (Math.sqrt(3)-1.0)/2.0;
    public final double G = (3.0-Math.sqrt(3))/6.0;
    public char[] perm = {};
    public SimplexGen2D(long seed) {
        this.seed = seed;
        this.perm = this.getShuffle();
    }

    public static int fast_floor(double x) {
        int y = (int) x;
        return (x < y) ? y-1 : y;
    }

    public double grad(int hash, Vec2d pos) {
        int h = hash & 7;
        double u = h<4 ? pos.x() : pos.y();
        double v = h<4 ? pos.y() : pos.x();
        return (((h&1) == 1)? -u : u) + (((h&2) == 2)? -2.0 *v  : 2.0 * v);
    }

    public static double pow(double x, int times) {
        double ret = 1;
        for (int i = 0; i < times; i++) {
            ret *= x;
        }
        return ret;
    }

    public double base(Vec2d pos) {
        double x = pos.x();
        double y = pos.y();
        Vec2d transformed = transformCoord(new Vec2d(x, y));

        double x_local = transformed.x()-fast_floor(transformed.x()),
                y_local = transformed.y()-fast_floor(transformed.y());

        Vec2d home_base = new Vec2d(fast_floor(transformed.x()), fast_floor(transformed.y()));

        Vec2d p2_1, p3_1;
        int i_1, j_1;
        if (x_local > y_local) {
            p2_1 = new Vec2d(1,0).plus(home_base);
            p3_1 = new Vec2d(1,1).plus(home_base);
            i_1 = 1;
            j_1 = 0;
        } else {
            p2_1 = new Vec2d(0,1).plus(home_base);
            p3_1 = new Vec2d(1,1).plus(home_base);
            i_1 = 0;
            j_1 = 1;
        }

        Vec2d p1, p2, p3;
        p1 = transformRevCoord(home_base);
        p2 = transformRevCoord(p2_1);
        p3 = transformRevCoord(p3_1);

        int ii = fast_floor(transformed.x()) & 0xff;

        int jj = fast_floor(transformed.y()) & 0xff;

        double c1 = pow(Math.max(0,0.5 - p1.distsq(pos)), 4) * grad(perm[(ii+perm[jj]) & 0xff], p1.minus(pos));
        double c2 = pow(Math.max(0,0.5 - p2.distsq(pos)), 4) * grad(perm[(ii+i_1+perm[(jj+j_1) & 0xff]) & 0xff], p2.minus(pos));
        double c3 = pow(Math.max(0,0.5 - p3.distsq(pos)), 4) * grad(perm[(ii+1+perm[(jj+1) & 0xff]) & 0xff], p3.minus(pos));

        return 20.0*(c1+c2+c3);
    }

    // Finds the local gradient
    public Vec2d gradient(Vec2d input) {
        return this.gradient(input, 0.001);
    }

    public Vec2d gradient(Vec2d input, double epsilon) {
        Vec2d one = input.plus(new Vec2d(epsilon, 0));
        Vec2d two = input.plus(new Vec2d(0, 0));
        Vec2d three = input.plus(new Vec2d(0, epsilon));
        Vec2d four = input.plus(new Vec2d(0, 0));
        return new Vec2d((base(one) - base(two))/epsilon, (base(three)-base(four))/epsilon);
    }

    public Vec2d laplacian(Vec2d input) {
        return this.laplacian(input, 1e-3);
    }

    public Vec2d laplacian(Vec2d input, double epsilon) {
        Vec2d one = input.plus(new Vec2d(epsilon, 0));
        Vec2d three = input.plus(new Vec2d(-epsilon, 0));
        Vec2d four = input.plus(new Vec2d(0, epsilon));
        Vec2d six = input.plus(new Vec2d(0, -epsilon));
        return new Vec2d((base(one)+base(three)-base(input)*2)/(epsilon * epsilon),
                (base(four)+base(six)-base(input)*2)/(epsilon * epsilon));
    }

    private Vec2d transformCoord(Vec2d input) {
        return new Vec2d(input.x() + (input.x() + input.y()) * F,input.y() + (input.x() + input.y()) * F);
    }
    private Vec2d transformRevCoord(Vec2d input) {
        return new Vec2d(input.x() - (input.x() + input.y()) * G, input.y() - (input.x() + input.y()) * G);
    }

    private char[] getShuffle() {
        ArrayList<Character> init = new ArrayList<>(256);
        for (int i = 0; i < 256; i++) {
            init.add((char)i);
        }
        Collections.shuffle(init, new Random(seed));
        char[] next = new char[256];
        for (int i = 0; i < 256; i++) {
            next[i] = init.get(i);
        }
        return next;
    }

    @Override
    public long getSeed() {
        return seed;
    }
}
