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

    public Vec2d sample(Vec2d pos) {
        int hashx = Double.hashCode(pos.x * seed + pos.y / seed) * (int)seed/2;
        int hashy = Double.hashCode(pos.y * seed + pos.x / seed) * (int)(3 * seed + 1);
        return new Vec2d((double)hashx/Math.pow(Math.pow(hashy, 3) + 1, 0.33) % 1,
                (double)hashy/Math.pow(Math.pow(hashx, 3) + 1, 0.33) % 1);

    }

    public Vec2d base(Vec2d pos) {
        int cellx = (int)fast_floor(pos.x), celly = (int)fast_floor(pos.y);
        double accu = Double.MAX_VALUE;
        int bestx = cellx;
        int besty = celly;
        for (int i = -4; i < 5; i++) {
            for (int j = -4; j < 5; j++) {
                Vec2d real = new Vec2d(cellx + i, celly + j);
                Vec2d res = sample(real);
                double distsq = pos.distsq(real.plus(res));
                if (distsq < accu) {
                    accu = distsq;
                    bestx = fast_floor(real.x);
                    besty = fast_floor(real.y);
                }

            }
        }
        return new Vec2d(bestx, besty);
    }

}
