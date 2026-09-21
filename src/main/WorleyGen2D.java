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

    public Vec2d sample(Vec2d pos, int index) {
        int hashx = Double.hashCode(pos.x() * seed * index + pos.y() / seed) * (int)seed/2;
        int hashy = Double.hashCode(pos.y() * seed + pos.x() / seed) * (int)(3 * seed + 1);
        return new Vec2d((double)hashx/Math.cbrt(hashy * hashy * hashy + 1) % 1,
                (double)hashy/Math.cbrt(hashx * hashx * hashx + 1) % 1).plus(pos);
    }

    public Vec2d base(Vec2d pos) {
        int cellx = (int)fast_floor(pos.x()), celly = (int)fast_floor(pos.y());
        double accu = Double.MAX_VALUE;
        int bestx = cellx;
        int besty = celly;
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
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
