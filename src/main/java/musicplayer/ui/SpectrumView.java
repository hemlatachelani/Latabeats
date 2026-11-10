package musicplayer.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Music visualizer. The player feeds it the live audio spectrum (setLevels) and it draws smooth,
 * animated bars. Three looks: Bars, Mirror, Dots. When the music stops the bars fall back down.
 */
public class SpectrumView extends Region {

    public static final int BANDS = 40;
    private static final String[] STYLES = {"Bars", "Mirror", "Dots"};

    private final Canvas canvas = new Canvas();
    private volatile float[] target = new float[BANDS];     // newest levels from the player, 0..1
    private double[] shown = new double[BANDS];             // smoothed levels that are drawn
    private double[] peak = new double[BANDS];              // falling "peak caps"
    private volatile boolean active;
    private Color color = Color.web("#7dd3fc");
    private int style = 0;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            step();
            draw();
        }
    };

    public SpectrumView() {
        getStyleClass().add("spectrum-view");
        getChildren().add(canvas);
        setMinHeight(120);
        setPrefHeight(180);
        sceneProperty().addListener((o, a, b) -> {
            if (b == null) timer.stop();
            else timer.start();
        });
    }

    /** Levels between 0 and 1, bass first. Safe to call from any thread. */
    public void setLevels(float[] levels) {
        target = levels;
    }

    public void setActive(boolean on) {
        active = on;
    }

    public void setColor(Color c) {
        color = c;
    }

    public void nextStyle() {
        style = (style + 1) % STYLES.length;
    }

    public String styleName() {
        return STYLES[style];
    }

    @Override
    protected void layoutChildren() {
        canvas.setLayoutX(0);
        canvas.setLayoutY(0);
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    // ---- animation ----
    private void step() {
        float[] t = target;
        int n = t.length;
        if (shown.length != n) {
            shown = new double[n];
            peak = new double[n];
        }
        boolean on = active;
        for (int i = 0; i < n; i++) {
            double goal = on ? t[i] : 0;
            double k = goal > shown[i] ? 0.55 : 0.12;           // rise fast, fall slowly
            shown[i] += (goal - shown[i]) * k;
            if (shown[i] < 0.002) shown[i] = 0;
            peak[i] = Math.max(shown[i], peak[i] - 0.012);
        }
    }

    private void draw() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 8 || h < 8) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        int n = shown.length;
        double gap = Math.max(2, w / n * 0.28);
        double bw = (w - gap * (n - 1)) / n;
        Color top = color.interpolate(Color.WHITE, 0.45);
        Color bottom = color.deriveColor(0, 1, 0.55, 0.95);
        LinearGradient grad = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, bottom), new Stop(1, top));

        for (int i = 0; i < n; i++) {
            double x = i * (bw + gap);
            double v = shown[i];
            if (style == 0) {                                   // Bars with falling caps
                double bh = Math.max(3, v * h * 0.95);
                g.setFill(grad);
                g.fillRoundRect(x, h - bh, bw, bh, bw, bw);
                if (peak[i] > 0.03) {
                    g.setFill(Color.web("#ffffff", 0.85));
                    g.fillRoundRect(x, h - peak[i] * h * 0.95 - 6, bw, 3, 3, 3);
                }
            } else if (style == 1) {                            // Mirror from the middle line
                double half = Math.max(2, v * h * 0.48);
                g.setFill(grad);
                g.fillRoundRect(x, h / 2 - half, bw, half * 2, bw, bw);
            } else {                                            // Dots stacked like an LED meter
                double d = Math.max(3, bw);
                double stepY = d + gap;
                int rows = Math.max(1, (int) (h / stepY));
                int lit = (int) Math.round(v * rows);
                for (int r = 0; r < rows; r++) {
                    double y = h - (r + 1) * stepY + gap;
                    if (r < lit) g.setFill(bottom.interpolate(top, rows <= 1 ? 1 : (double) r / (rows - 1)));
                    else g.setFill(Color.web("#ffffff", 0.07));
                    g.fillOval(x, y, d, d);
                }
            }
        }
    }
}
