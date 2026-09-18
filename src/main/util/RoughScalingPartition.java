package main.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class RoughScalingPartition {
    protected List<Double> cuts;
    protected List<Double> scales;
    protected double pivot;
    protected int pivotPos;

    public RoughScalingPartition(Set<Double> cuts, List<Double> scales, double pivot) {
        if (cuts.size() + 1 != scales.size()) {
            throw new IllegalArgumentException("bad sizes");
        }

        Set<Double> cutsp = new HashSet<Double>(cuts);
        boolean changed = false;
        if (!cutsp.contains(pivot)) {
            cutsp.add(pivot);
            changed = true;
        }

        this.cuts = cutsp.stream().sorted().toList();
        this.scales = new ArrayList<Double>(scales);
        if (changed) {
            int index = this.cuts.indexOf(pivot);
            this.scales.add(index + 1, this.scales.get(index));
        }

        for (double scale : this.scales) {
            if (scale < 0) {
                throw new IllegalArgumentException("scale found to be negative");
            }
        }

        this.pivot = pivot;
        this.pivotPos = this.cuts.indexOf(pivot);
    }

    public double findScale(double lookup) {
        if (cuts.isEmpty() || lookup < cuts.getFirst()) {
            return scales.getFirst();
        }
        if (lookup > cuts.getLast()) {
            return scales.getLast();
        }
        // begin binary search
        int lower = 0;
        int upper = cuts.size()-1;

        while (upper-lower > 1) {
            int mid = (upper + lower)/2;
            if (cuts.get(mid) <= lookup) {
                lower = mid;
            } else {
                upper = mid;
            }
        }
        if (cuts.get(upper) == lookup) {
            return scales.get(upper);
        }
        return scales.get(lower);
    }

    public double remap(double lookup) {
        double expansion = 0;
        if (lookup == pivot) {
            return lookup;
        }
        else if (lookup < pivot) {
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

    public static <T extends Object> Function<T, Double> redistrib(Function<T, Double> func, RoughScalingPartition pt) {
        Function<T, Double> internal = inp -> {
            return pt.remap(func.apply(inp));
        };
        return internal;
    }
}
