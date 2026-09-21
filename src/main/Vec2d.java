package main;

public final class Vec2d {
    public final double x;
    public final double y;
    public Vec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double dot(Vec2d other) {
        return x * other.x + y * other.y;
    }

    public double distsq(Vec2d other) {
        return (x - other.x)*(x - other.x) + (y - other.y)*(y - other.y);
    }

    public Vec2d plus(Vec2d other) {
        return new Vec2d(x + other.x, y + other.y);
    }

    public Vec2d times(double other) {
        return new Vec2d(x * other, y * other);
    }

    public Vec2d minus(Vec2d other) {
        return new Vec2d(x - other.x, y - other.y);
    }

    public double abs() {return Math.sqrt(this.distsq(new Vec2d(0,0)));}

    @Override
    public String toString() {
        return ((Double)x).toString() + " " + ((Double)y).toString();
    }
}
