package main;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class RenderComponent extends JPanel {
    private SimplexGen2D gen;
    private WorleyGen2D gen2;
    private SimplexOctaveGen2D gen3;
    private double zoom;
    private long seed;
    public RenderComponent(long seed) {
        this.seed = seed;
        zoom = 100;
        gen = new SimplexGen2D(seed);
        gen2 = new WorleyGen2D(seed);
        gen3 = new SimplexOctaveGen2D(seed);
        gen3.addLayer(0.01, 27);
        gen3.addLayer(0.04, 7);
        gen3.addLayer(0.19, 3);
    }

    public int highEntropyHash(double value) {
        // 1. Convert the double into its raw IEEE 754 64-bit representation.
        // Use doubleToLongBits to collapse all variations of NaN into a single state.
        long bits = Double.doubleToLongBits(value);

        // 2. Apply a MurmurHash3 / Mix13 style bit avalanche mixer
        // to spread the entropy of the 64 bits thoroughly.
        bits ^= bits >>> 33;
        bits *= 0xff51afd7ed558ccdL;
        bits ^= bits >>> 33;
        bits *= 0xc4ceb9fe1a85ec53L;
        bits ^= bits >>> 33;
        bits *= (this.seed + 0x38cdf9ee2a05e7b3L);
        bits ^= bits >>> 33;

        // 3. Downcast to 32-bit int. The entropy is now uniformly distributed.
        return (int) bits;
    }

    public Vec2d getPlateVelocity(Vec2d continent, double x, double y, int stage) {
        return new Vec2d(gen.base(new Vec2d(continent.x * 2 + stage, continent.y * 2 + stage)),
                gen.base(new Vec2d(continent.y * 2 + stage, -continent.x * 2 + stage))).times(2)
                .plus(new Vec2d(gen.base(new Vec2d(x * 3 + continent.x + stage, y * 3 + continent.y + stage)),
                        gen.base(new Vec2d(-x * 3 + continent.x + stage, y * 3 + continent.y + stage))).times(2));
    }

    public void paintComponent(Graphics g) {
        BufferedImage imageinit = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) {
                double height = getBase(x,y);
                imageinit.setRGB(x, y, (int) height*2048);
            }
        }
        BufferedImage finalim = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) {
                double height = kernel(x, y, imageinit) + geo(x, y, 0) + geo(x, y, 1) * 0.25;
                if (height >= 0.31) {
                    finalim.setRGB(x, y, new Color(140+(int)(height * 5), 200, 60).getRGB());
                } else {
                    finalim.setRGB(x, y, new Color(40, 150, 210).getRGB());
                }
            }
        }
        System.out.println("done");
        g.drawImage(finalim, 0, 0, 600, 600, this);
    }

    public double getBase(int x, int y) {
        double dx = gen3.base(new Vec2d(x, y));
        double dy = gen3.base(new Vec2d(-x, y));
        Vec2d res = gen2.base(new Vec2d((x - 300 + dx) / zoom, (y - 300 + dy) / zoom));
        int dz = (int)gen3.base(new Vec2d(-y, x));
        double test = gen.base(new Vec2d(res.x + res.y, res.x - res.y).times(0.15));
        if (test >= 0.06) {
            return dz+0.10;
        } else {
            return -0.39;
        }
    }

    public double geo(int x, int y, int stage) {
        double accu = 0;
        double divis = 0;
        for (int i = -(int)(150.0/zoom); i < (int)(300.0/zoom); i++) {
            for (int j = -(int)(150.0/zoom); j < (int)(300.0/zoom); j++) {
                int newx = x + i;
                int newy = y + j;
                double dx = gen3.base(new Vec2d(newx, newy));
                double dy = gen3.base(new Vec2d(-newx, newy));
                Vec2d continent = gen2.base(new Vec2d((newx - 300 + dx) / zoom, (newy - 300 + dy) / zoom));
                Vec2d velocity = this.getPlateVelocity(continent, x, y, stage);

                accu -= velocity.dot(new Vec2d(i, j)) * 1/Math.sqrt((double)(i*i + j*j) + 1.8) + 0.13;
                divis += 1/Math.sqrt((double)(i*i + j*j) + 1.8);
            }
        }
        double dx = gen3.base(new Vec2d(x, y));
        double dy = gen3.base(new Vec2d(-x, y));
        Vec2d res = gen2.base(new Vec2d((x - 300 + dx) / zoom, (x - 300 + dy) / zoom));
        double test = gen.base(new Vec2d(res.x + res.y, res.x - res.y).times(0.15));
        if (test >= 0.06) {
            return accu/divis - 0.32;
        } else {
            return accu/divis-0.40;
        }
    }

    public double kernel(int x, int y, BufferedImage img) {
        double accu = 0;
        double divis = 0;
        for (int i = -(int)(200.0/zoom); i < (int)(300.0/zoom); i++) {
            for (int j = -(int)(200.0/zoom); j < (int)(300.0/zoom); j++) {
                double distFudge = 1/Math.sqrt(i * i + j * j + 1);
                if (x+i >= 0 && y+j >= 0 && x+i < img.getWidth() && y+j < img.getHeight()) {
                    double change = (double)img.getRGB(x+i,y+j)/2048.0;
                    accu += change * distFudge;
                } else {
                    accu += (getBase(x+i,y+j)) * distFudge;
                }
                divis += distFudge;
            }
        }
        return accu/divis;
    }
}
