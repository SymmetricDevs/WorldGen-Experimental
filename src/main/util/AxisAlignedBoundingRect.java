package main.util;

import main.Vec2d;
import java.lang.Math;

public class AxisAlignedBoundingRect {
    Vec2d lowerVals = new Vec2d(0,0);
    Vec2d upperVals = new Vec2d(0,0);

    public AxisAlignedBoundingRect(Vec2d one, Vec2d two) {
        lowerVals = new Vec2d(Math.min(one.x, two.x), Math.min(one.y, two.y));
        upperVals = new Vec2d(Math.max(one.x, two.x), Math.max(one.y, two.y));
    }
}
