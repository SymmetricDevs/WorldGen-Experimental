package main;

public final record Vec2i(int x, int y) {
    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    public int dot(Vec2i other) {
        return x * other.x + y * other.y;
    }

    public double distsq(Vec2i other) {
        return (x - other.x)*(x - other.x) + (y - other.y)*(y - other.y);
    }

    public Vec2i plus(Vec2i other) {
        return new Vec2i(x + other.x, y + other.y);
    }

    public Vec2i times(int other) {
        return new Vec2i(x * other, y * other);
    }

    public Vec2i minus(Vec2i other) {
        return new Vec2i(x - other.x, y - other.y);
    }

    public double abs() {return Math.sqrt(this.distsq(new Vec2i(0,0)));}

    @Override
    public String toString() {
        return ((Integer)x).toString() + " " + ((Integer)y).toString();
    }
}
