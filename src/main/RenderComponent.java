package main;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class RenderComponent extends JPanel {
    private SimplexGen2D gen;
    private WorleyGen2D gen2;
    private SimplexOctaveGen2D gen3;
    private double zoom;
    private long seed;
    private final double height_cutoff = 0.03;
    private final double ground_cut = 0.38;
    private static final int width = 800;
    private final Vec2i[] neighbors = {new Vec2i(0,1), new Vec2i(0,-1), new Vec2i(-1,0), new Vec2i(1, 0),
            new Vec2i(-1, 1), new Vec2i(1, 1), new Vec2i(-1, -1), new Vec2i(1, -1)};

    enum DIR {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        UPLEFT,
        UPRIGHT,
        DOWNLEFT,
        DOWNRIGHT,
        EMPTY
    }

    private DIR neighborMap(int index) {
        return DIR.values()[index];
    }

    private Vec2d dirToVec(DIR d) {
        return switch (d) {
            case UP -> new Vec2d(0, 1);
            case DOWN -> new Vec2d(0, -1);
            case LEFT -> new Vec2d(-1, 0);
            case RIGHT -> new Vec2d(1, 0);
            case UPLEFT -> new Vec2d(-1, 1);
            case UPRIGHT -> new Vec2d(1, 1);
            case DOWNLEFT -> new Vec2d(-1, -1);
            case DOWNRIGHT -> new Vec2d(1, -1);
            case EMPTY -> new Vec2d(0, 0);
        };
    }

    int[] worley1 = new int[width * width];
    int[] worley2 = new int[width * width];

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

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                double dx = gen3.base(new Vec2d(x, y));
                double dy = gen3.base(new Vec2d(-x, y));
                Vec2d res = gen2.base(new Vec2d((x - 300 + dx) / zoom, (y - 300 + dy) / zoom));
                Vec2d out = gen2.base(res);
                worley1[x + width*y] = (int)out.x();
                worley2[x + width*y] = (int)out.y();
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
        double[] riverim = new double[width*width];
        double[] carvedim = getDepressionFill(secondim, imageinit, fillim, riverim);

        //getRivers(fillim, riverim);
        BufferedImage finalim = new BufferedImage(width, width, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                double height = carvedim[x + width*y];
                double fill = fillim[x + width*y] - height;
                try {
                    if (height >= ground_cut) {
                        if (riverim[x + y * width] >= 11.0) {
                            finalim.setRGB(x, y, new Color(255, 255, 255).getRGB());
                        }
                        else if (fill/2 <= 0.1) {
                            finalim.setRGB(x, y, new Color((int)(255 * sigmoid(height * 0.09 + -0.6)),
                                    (int)(150 * sigmoid(height * 0.065 + 1.2)),
                                    (int)(120 * sigmoid(height * 0.04 - 1.5))).getRGB());
                            //finalim.setRGB(x, y, new Color(0,0,((height % 4 < 0.3) ? 255 : 0)).getRGB());
                        } else {
                            finalim.setRGB(x, y, new Color(0,
                                    0,
                                    (int)(255 - 120 * sigmoid(fill * 0.05 - 1.1))).getRGB());
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

    public static class FillEntry extends AbstractMap.SimpleEntry<AbstractMap.SimpleEntry<Integer, Integer>, Double> {
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
        public Vec2i getVec() {return new Vec2i(this.getX(), this.getY());}
        public static FillEntry[] get(double[] intro) {
            FillEntry[] ret = new FillEntry[width * width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    ret[i + j * width] = new FillEntry(new AbstractMap.SimpleEntry<>(i, j),
                            intro[i + j * width]);
                }
            }
            return ret;
        }
    }

    private boolean isEdge(int x, int y) {
        return x == 0 || y == 0 || x == (width - 1) || y == (width - 1);
    }

    private boolean ooB(Vec2d vec) {
        return vec.x() < 0 || vec.y() < 0 || vec.x() >= width || vec.y() >= width;
    }

    private boolean ooB(Vec2i vec) {
        return vec.x() < 0 || vec.y() < 0 || vec.x() >= width || vec.y() >= width;
    }

    private Vec2i[] depressionPass(double[] image, double[] prekernel, double[] out, double[] riverout) {
        PriorityQueue<FillEntry> open = new PriorityQueue<>(Comparator.comparingDouble(FillEntry::getPrior));
        boolean[] closed = new boolean[width * width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                if (image[x + width*y] >= ground_cut) {
                    boolean edge = false;
                    for (Vec2i change : neighbors) {
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
                        open.add(new FillEntry(x, y, image[x + width * y]));
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
        Vec2i[] receivers = new Vec2i[width * width];
        Vec2i[] order = new Vec2i[width * width];
        int k = 0;
        Arrays.fill(receivers, new Vec2i(-1,-1));
        while (!open.isEmpty() || !pit.isEmpty()) {
            FillEntry entry;
            if (!pit.isEmpty()) {
                entry = pit.remove();
            } else
                entry = open.remove();
            order[k++] = entry.getVec();
            for (Vec2i change : neighbors) {
                int newx = entry.getX() + (int) change.x();
                int newy = entry.getY() + (int) change.y();
                boolean inBounds = !ooB(new Vec2d(newx, newy));
                if (!inBounds || closed[newx + width*newy]) {
                    continue;
                }
                double height = image[newx + width * newy];
                closed[newx + width*newy] = true;
                double epsilon = 0.00001;
                if (height < entry.getPrior() + epsilon) {
                    out[newx + width*newy] = entry.getPrior() + epsilon;
                    pit.add(new FillEntry(newx, newy, entry.getPrior()));
                } else {
                    out[newx + width*newy] = height;
                    open.add(new FillEntry(newx, newy, height));
                    receivers[newx + newy * width] = entry.getVec();
                }
            }
        }

        Arrays.fill(riverout, 1.0);
        for (int l = k - 1; l >= 0; l--) {
            Vec2i inputter = order[l];
            int index = inputter.x() + inputter.y() * width;
            int recindex = receivers[index].x() + receivers[index].y() * width;
            if (recindex >= 0)
                riverout[recindex] += riverout[index];
        }

        return order;
    }

    private double[] getDepressionFill(double[] image, double[] prekernel, double[] out, double[] riverout) {
        double[] imageout = image.clone();
        double[] prekernelc = prekernel.clone();

        Vec2i[] kout;
        for (int j = 0; j < 10; j++) {
            kout = depressionPass(imageout, prekernelc, out, riverout);
            for (int i = 0; i < kout.length; i++) {
                Vec2i inputter = kout[i];
                if (inputter == null) {
                    break;
                }
                int index = inputter.x() + inputter.y() * width;
                imageout[index] -= Math.sqrt(i) * Math.pow(riverout[index], 0.5) * gradient(imageout, inputter).abs() * 0.001;
            }
        }
        depressionPass(imageout, prekernelc, out, riverout);
        return imageout;

    }

    public Vec2d gradient(double[] image, Vec2i loc) {
        double accx = 0;
        double accy = 0;
        double count = 1;
        for (Vec2i neighbor: neighbors) {
            if (!ooB(neighbor.plus(loc))) {
                Vec2i neigh = neighbor.plus(loc);
                accx += image[neigh.x() + width * neigh.y()] * neighbor.x() / neighbor.abs();
                accy += image[neigh.x() + width * neigh.y()] * neighbor.y() / neighbor.abs();
                count += 1/neighbor.abs();
            }
        }
        return new Vec2d(accx/count, accy/count);
    }

    public double getFundamentalHeight(int x, int y, double[] image) {
        return kernel(x, y, image) + geo(x, y, 0) * 0.5 + geo(x, y, 1) * 0.25 + gen3.base(new Vec2d(x, y)) * 0.1;
    }

    public double getBase(int x, int y) {
        double dx = gen3.base(new Vec2d(x, y));
        double dy = gen3.base(new Vec2d(-x, y));
        Vec2d res = gen2.base(new Vec2d((x - 300 + dx) / zoom, (y - 300 + dy) / zoom));
        double dz = gen3.base(new Vec2d(-y/4, x/4)) + 0.75 * gen3.base(new Vec2d(-x, y+3));

        double test = gen.base(new Vec2d(res.x() + dx/4, res.y() + dy/4).times(0.01));
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
                    double change = img[x+i + width * (y+j)];
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
