package main;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RenderLayout implements LayoutManager {
    private Map<String, Component> componentMap = new HashMap<>();

    @Override
    public void addLayoutComponent(String name, Component comp) {
        componentMap.put(name, comp);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        componentMap.values().remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension(500, 500);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(100, 100);
    }

    @Override
    public void layoutContainer(Container parent) {
        if (componentMap.containsKey("render")) {
            Insets insets = parent.getInsets();
            int maxWidth = parent.getWidth()
                    - (insets.left + insets.right);
            int maxHeight = parent.getHeight()
                    - (insets.top + insets.bottom);

            Component c = componentMap.get("render");
            c.setBounds(0,0, maxWidth, maxHeight);
        }
    }
}
