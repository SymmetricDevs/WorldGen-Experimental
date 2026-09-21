package main;
public class WorleyGen2D {
    private long seed = 0;

    public WorleyGen2D(long seed) {
        this.seed = seed;
    }
    public static int fast_floor(double x) {
        int y = (int) x;
        return (x < y) ? y-1 : y;
    }

    // Fast 64-bit mix function (SplitMix64 variant)
    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    // Hash a long input to a double in [-1.0, 1.0]
    public static double hashToDouble(double input) {
        long mixed = mix64(Double.doubleToLongBits(input));
        // Map top 53 bits to a [0, 1) double using bit manipulation
        long upper53 = mixed >>> 11;
        double d = upper53 * (1.0 / (1L << 53)); // value in [0, 1)
        // Scale and shift from [0, 1) to [-1, 1] roughly
        return d * 2.0 - 1.0;
    }

    public Vec2d sample(Vec2d pos, int index) {
        double hashx = hashToDouble(pos.x() * seed * 1 / (Math.abs(index) + 1) + pos.y() / seed);
        double hashy = hashToDouble(pos.y() * seed + pos.x() / seed);
        return new Vec2d(hashx, hashy).plus(pos);
    }

    public Vec2d base(Vec2d pos) {
        int cellx = (int)fast_floor(pos.x()), celly = (int)fast_floor(pos.y());
        double accu = Double.MAX_VALUE;
        int bestx = cellx;
        int besty = celly;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Vec2d real = new Vec2d(cellx + i, celly + j);
                Vec2d res = sample(real, 0);
                Vec2d res2 = sample(real, 1);
                double distsq = Math.min(pos.distsq(res), pos.distsq(res2));
                if (distsq < accu) {
                    accu = distsq;
                    bestx = fast_floor(real.x());
                    besty = fast_floor(real.y());
                }

            }
        }
        return new Vec2d(bestx, besty);
    }

    public Vec2d edgeDir(Vec2d pos) {
        int cellx = (int)fast_floor(pos.x()), celly = (int)fast_floor(pos.y());
        Vec2d accu = new Vec2d(Double.MAX_VALUE, Double.MAX_VALUE);
        Vec2d accu2 = new Vec2d(Double.MAX_VALUE, Double.MAX_VALUE);
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                Vec2d real = new Vec2d(cellx + i, celly + j);
                Vec2d res = sample(real, 0);
                Vec2d res2 = sample(real, 1);
                double distsq = Math.min(pos.distsq(res), pos.distsq(res2));
                if (distsq < accu.abs()) {
                    accu2 = accu;
                    if (pos.distsq(res) > pos.distsq(res2))
                        accu = res2;
                    else
                        accu = res;
                }
            }
        }

        if (accu2.abs() > 100) {
            return new Vec2d(0,0);
        }

        return accu2.minus(accu);
    }
}