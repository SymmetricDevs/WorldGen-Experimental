package main.util;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class SmoothScalingPartition extends RoughScalingPartition {
    private double smoothing;
    public SmoothScalingPartition(Set<Double> cuts, List<Double> scales, double pivot, double smoothing) {
        super(cuts, scales, pivot);
        if (smoothing <= 0) {
            throw new IllegalArgumentException("invalid smoothing");
        }
        this.smoothing = smoothing;
    }

    @Override
    public double remap(double lookup) {
        double expansion = 0;
        if (lookup == pivot)
            return lookup;
        if (lookup < pivot) {
            for (int i = pivotPos; i >= 0; i--) {
                double lowerCut = i == 0 ? -Double.MAX_VALUE : cuts.get(i - 1);
                expansion += scales.get(i) * (cuts.get(i) - Math.max(lowerCut, lookup));
                if (lookup >= lowerCut) {
                    break;
                }
            }
            return pivot - expansion;
        } else {
            for (int i = pivotPos; i < cuts.size(); i++) {
                double upperCut = i == cuts.size() - 1 ? Double.MAX_VALUE : cuts.get(i + 1);
                expansion += scales.get(i + 1) * (Math.min(upperCut, lookup) - cuts.get(i));
                if (lookup <= upperCut) {
                    break;
                }
            }
            return pivot + expansion;
        }
    }

}
