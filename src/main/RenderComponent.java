package main;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class RenderComponent extends JPanel {
    private SimplexGen2D gen;
    private WorleyGen2D gen2;
    private SimplexOctaveGen2D gen3;
    private double zoom;
    private long seed;
    private final double height_cutoff = 0.03;
    private final double ground_cut = 0.38;
    private final int width = 600;
    private final Vec2d[] neighbors = {new Vec2d(1,0), new Vec2d(1,1), new Vec2d(0,1), new Vec2d(0, -1),
            new Vec2d(-1, -1), new Vec2d(-1, 0), new Vec2d(-1, 1), new Vec2d(1, -1)};
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
        return new Vec2d(gen.base(new Vec2d(continent.x() * 2 + stage, continent.y() * 2 + stage)),
                gen.base(new Vec2d(continent.y() * 2 + stage, -continent.x() * 2 + stage))).times(2)
                .plus(new Vec2d(gen.base(new Vec2d(x * 3 + continent.x() + stage, y * 3 + continent.y() + stage)),
                        gen.base(new Vec2d(-x * 3 + continent.x() + stage, y * 3 + continent.y() + stage))).times(2));
    }

    public static double sigmoid(double x) {
        return 1/(1+Math.exp(-x));
    }

    public void paintComponent(Graphics g) {
        double[] imageinit = new double[width * width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                double height = getBase(x,y);
                imageinit[x + width*y] = height;
            }
        }
        double[] secondim = new double[width*width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                double height = getFundamentalHeight(x, y, imageinit);
                secondim[x + width*y] = height;
            }
        }
        double[] fillim = new double[width*width];
        getDepressionFill(secondim, imageinit, fillim);
        BufferedImage finalim = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                double height = secondim[x + width*y];
                double fill = fillim[x + width*y] - height;
                try {
                    if (height >= ground_cut) {
                        if (fill <= 0.1) {
                            //finalim.setRGB(x, y, new Color((int)(255 * sigmoid(height * 0.09 + -0.6)),
                            //        (int)(240 * sigmoid(height * 0.065 + 1.2)),
                            //        (int)(200 * sigmoid(height * 0.04 - 1.5))).getRGB());
                            finalim.setRGB(x, y, new Color(0,0,((height % 4 < 0.3) ? 255 : 0)).getRGB());
                        } else {
                            finalim.setRGB(x, y, new Color(0, 255, 255).getRGB());
                        }
                    } else {
                        finalim.setRGB(x, y, new Color(40, 130 + (int) (height * 3.0), 180 + (int) (height * 2.5)).getRGB());
                    }
                } catch (Exception e) {
                    System.out.println(height);
                }
            }
        }
        g.drawImage(finalim, 0, 0, width, width, this);
    }

    public class FillEntry extends AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<Integer, Integer>, Double> {
        public FillEntry(AbstractMap.SimpleEntry<Integer, Integer> key, Double value) {
            super(key, value);
        }
        public FillEntry(int key1, int key2, double value) {
            super(new AbstractMap.SimpleEntry<>(key1, key2), value);
        }
        public int getX() {
            return this.getKey().getKey();
        }
        public int getY() {
            return this.getKey().getValue();
        }
        public double getPrior() {
            return this.getValue();
        }
    }

    private void getDepressionFill(double[] image, double[] prekernel, double[] out) {
        PriorityQueue<FillEntry> open = new PriorityQueue<>(Comparator.comparingDouble(FillEntry::getPrior));
        boolean[] closed = new boolean[width * width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                if (image[x + width*y] >= ground_cut) {
                    boolean edge = false;
                    for (Vec2d change : neighbors) {
                        int newx = x + (int) change.x();
                        int newy = y + (int) change.y();
                        double height;
                        if (newx >= 0 && newy >= 0 && newx < width && newy < width) {
                            height = image[newx + width*newy];
                        } else {
                            height = getFundamentalHeight(newx, newy, prekernel);
                        }
                        if (height < ground_cut) {
                            edge = true;
                            break;
                        }
                    }
                    if (edge) {
                        open.add(new FillEntry(x, y, image[x + width*y]));
                        closed[x + width*y] = true;
                    } else {
                        closed[x + width*y] = false;
                    }
                } else {
                    closed[x + width*y] = true;
                }
            }
        }
        ArrayDeque<FillEntry> pit = new ArrayDeque<>();
        while (!open.isEmpty() || !pit.isEmpty()) {
            FillEntry entry;
            if (!pit.isEmpty()) {
                entry = pit.remove();
            } else
                entry = open.remove();
            for (Vec2d change : neighbors) {
                int newx = entry.getX() + (int) change.x();
                int newy = entry.getY() + (int) change.y();
                boolean inBounds = newx >= 0 && newy >= 0 && newx < width && newy < width;
                if (!inBounds || closed[newx + 600*newy]) {
                    continue;
                }
                double height = getFundamentalHeight(newx, newy, prekernel);
                closed[newx + 600*newy] = true;
                if (height < entry.getPrior()) {
                    out[newx + width*newy] = entry.getPrior();
                    pit.add(new FillEntry(newx, newy, entry.getPrior()));
                } else {
                    open.add(new FillEntry(newx, newy, height));
                }
            }
        }
    }

    public double getFundamentalHeight(int x, int y, double[] image) {
        return kernel(x, y, image) + geo(x, y, 0) * 0.5 + geo(x, y, 1) * 0.25 + gen3.base(new Vec2d(x, y)) * 0.1;
    }

    public double getBase(int x, int y) {
        double dx = gen3.base(new Vec2d(x, y));
        double dy = gen3.base(new Vec2d(-x, y));
        Vec2d res = gen2.base(new Vec2d((x - 300 + dx) / zoom, (y - 300 + dy) / zoom));
        double dz = gen3.base(new Vec2d(-y/4, x/4)) + 0.75 * gen3.base(new Vec2d(-x, y+3));
        //int hash = highEntropyHash(Math.pow(res.x() + 1, 5)/(Math.abs(res.y()*res.x() + res.x() - res.y()) + 1));

        double test = gen.base(new Vec2d(res.x() + dx/4, res.y() + dy/4).times(0.01));
        //System.out.println(Integer.remainderUnsigned(hash, 11));
        if (test >= height_cutoff) {
            return dz * 1.2 + 2.0;
        } else {
            return dz * 0.75 - 9.5;
        }
    }

    public double geo(int x, int y, int stage) {
        double accu = 0;
        double divis = 0;
        for (int i = -(int)(150.0/zoom); i < (int)(300.0/zoom); i++) {
            for (int j = -(int)(150.0/zoom); j < (int)(300.0/zoom); j++) {
                int newx = x + i;
                int newy = y + j;
                double dx = gen3.base(new Vec2d(newx, newy)) * 0.5;
                double dy = gen3.base(new Vec2d(-newx, newy)) * 0.5;
                Vec2d core = new Vec2d((newx - 300 + dx) / zoom, (newy - 300 + dy) / zoom);
                Vec2d res = gen2.base(core);
                Vec2d velocity = this.getPlateVelocity(res, core.x(), core.y(), stage);

                accu -= velocity.dot(new Vec2d(i, j)) * 1/Math.sqrt((double)(i*i + j*j) + 1.8) + 0.13;
                divis += 1/Math.sqrt((double)(i*i + j*j) + 1.8);
            }
        }

        double dx = gen3.base(new Vec2d(x, y));
        double dy = gen3.base(new Vec2d(-x, y));
        Vec2d core = new Vec2d((x - 300 + dx) / zoom, (y - 300 + dy) / zoom);
        Vec2d res = gen2.base(core);
        Vec2d coreContinent = gen2.sample(core, 0);
        Vec2d coreContinentVelocity = this.getPlateVelocity(res, coreContinent.x(), coreContinent.y(), stage);
        double test = gen.base(new Vec2d(coreContinent.x(), coreContinent.y()).times(0.01));
        if (test >= height_cutoff) {
            // problem: how to make this dependent on the other continent's velocity without wasting time?
            double subduction = Math.max(0, gen2.edgeDir(core).dot(coreContinentVelocity) * 0.05 / zoom);
            return accu/divis + 0.05 + subduction;
        }
        return accu/divis + 0.1;
    }

    public double kernel(int x, int y, double[] img) {
        double accu = 0;
        double divis = 0;
        for (int i = -(int)(200.0/zoom); i < (int)(300.0/zoom); i++) {
            for (int j = -(int)(200.0/zoom); j < (int)(300.0/zoom); j++) {
                double distFudge = 1/Math.sqrt(i * i + j * j + 1);
                if (x+i >= 0 && y+j >= 0 && x+i < width && y+j < width) {
                    double change = img[x+i + 600 * (y+j)];
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
