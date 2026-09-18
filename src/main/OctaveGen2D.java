package main;

import main.util.IGenBase;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

public class OctaveGen2D<T extends IGenBase> implements IGenBase {
    public T element;
    private double[] scales = new double[]{};
    private double[] amplitudes = new double[]{};

    public void addLayer(double scale, double amplitude) {
        double[] singleton = {scale};
        double[] singleton2 = {amplitude};
        ToDoubleFunction<Object> map = obj -> (Double)obj;
        scales = Stream.concat(Arrays.stream(scales).boxed(), Arrays.stream(singleton).boxed()).mapToDouble(map).toArray();
        amplitudes = Stream.concat(Arrays.stream(amplitudes).boxed(), Arrays.stream(singleton2).boxed()).mapToDouble(map).toArray();
    }

    protected OctaveGen2D(long seed, Class<T> elementClass) {
        this.element = IGenBase.create(seed, elementClass);
    }

    public double base(Vec2d pos) {
        double output = 0;
        for (int i = 0; i < scales.length; i++) {
            output += element.base(pos.times(scales[i])) * amplitudes[i];
        }
        return output;
    }

    public long getSeed() {
        return element.getSeed();
    }
}
