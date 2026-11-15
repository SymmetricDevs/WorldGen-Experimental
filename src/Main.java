import main.RenderComponent;
import main.util.RoughScalingPartition;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Main {
    public static void main() throws InterruptedException {
        RoughScalingPartition x = new RoughScalingPartition(Set.of(0.0,5.0), List.of(1.0,2.0,3.0), 50);
        Random random = new Random();
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        loader.setDefaultAssertionStatus(true);
        RenderComponent comp = new RenderComponent(123);
        JFrame window = new JFrame("render");


        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        String emptyLabel = "render";
        window.getContentPane().add(comp, BorderLayout.CENTER);

        window.setSize(800, 800);

        window.setVisible(true);

        while (window.isActive()) {
            Thread.sleep(30);
        }
    }
}
