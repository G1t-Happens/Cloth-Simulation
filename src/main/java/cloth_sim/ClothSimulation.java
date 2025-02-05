package cloth_sim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A cloth simulation that uses a particle system on a grid with constraints acting as distance-preserving springs.
 *
 * <p>The simulation includes:</p>
 * <ul>
 *   <li>A grid of particles with Verlet integration</li>
 *   <li>Multiple constraint iterations for distance preservation</li>
 *   <li>Gravity and damping</li>
 *   <li>Interactive drag and drop to manipulate individual particles</li>
 * </ul>
 *
 * <p>This class demonstrates how to implement a reasonably complex physics-based simulation in a single Java class.</p>
 */
public class ClothSimulation extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    // ------------------------------------------------------------------------
    // Simulation and Rendering Constants
    // ------------------------------------------------------------------------
    /**
     * The width of the simulation window in pixels.
     */
    private static final int WIDTH = 800;

    /**
     * The height of the simulation window in pixels.
     */
    private static final int HEIGHT = 600;

    /**
     * Time step for the simulation in seconds (approximately 60 FPS).
     */
    private static final double TIME_STEP = 0.016;

    /**
     * Gravity in pixels per second squared (980 is roughly 9.8 * 100).
     */
    private static final double GRAVITY = 980;

    /**
     * Damping factor applied to particle velocities.
     */
    private static final double DAMPING = 0.99;

    /**
     * Number of constraint solving iterations per frame.
     */
    private static final int CONSTRAINT_ITERATIONS = 5;

    // ------------------------------------------------------------------------
    // Cloth (Grid) Configuration
    // ------------------------------------------------------------------------
    /**
     * Number of columns in the cloth grid.
     */
    private static final int NUM_COLS = 30;

    /**
     * Number of rows in the cloth grid.
     */
    private static final int NUM_ROWS = 20;

    /**
     * The spacing in pixels between adjacent particles in the grid.
     */
    private static final double SPACING = 20;

    // ------------------------------------------------------------------------
    // Data Structures
    // ------------------------------------------------------------------------
    /**
     * 2D array of {@link Particle} objects forming the cloth grid.
     */
    private transient Particle[][] particles;

    /**
     * List of {@link Constraint} objects representing the springs between particles.
     */
    private transient List<Constraint> constraints;

    // ------------------------------------------------------------------------
    // Mouse Interaction (Drag & Drop)
    // ------------------------------------------------------------------------
    /**
     * The currently selected {@link Particle} when dragging; null if none is selected.
     */
    private transient Particle selectedParticle = null;

    /**
     * Previous mouse X coordinate during drag operations.
     */
    int prevMouseX;

    /**
     * Previous mouse Y coordinate during drag operations.
     */
    int prevMouseY;

    /**
     * Constructs the cloth simulation panel:
     * <ul>
     *     <li>Sets the preferred size and background color</li>
     *     <li>Adds mouse listeners for user interaction</li>
     *     <li>Initializes the cloth grid</li>
     *     <li>Starts the timer for periodic updates</li>
     * </ul>
     */
    public ClothSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);

        initCloth();

        // ------------------------------------------------------------------------
        // Timer for Periodic Updates
        // ------------------------------------------------------------------------
        Timer timer = new Timer((int) (TIME_STEP * 1000), this);
        timer.start();
    }

    /**
     * Initializes the cloth by creating:
     * <ul>
     *     <li>A 2D array of {@link Particle} instances laid out in a grid</li>
     *     <li>Multiple {@link Constraint} objects to link adjacent particles</li>
     * </ul>
     *
     * <p>All particles in the top row are pinned (fixed in place).</p>
     */
    private void initCloth() {
        particles = new Particle[NUM_ROWS][NUM_COLS];
        constraints = new ArrayList<>();

        // Create the particles
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                // Offset to avoid placing cloth at the very edge
                double x = 100 + col * SPACING;
                double y = 50 + row * SPACING;
                particles[row][col] = new Particle(x, y);

                // Pin the top row particles
                if (row == 0) {
                    particles[row][col].pinned = true;
                }
            }
        }

        // Create constraints (springs) horizontally, vertically, and diagonally
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                // Horizontal
                if (col < NUM_COLS - 1) {
                    constraints.add(new Constraint(particles[row][col], particles[row][col + 1]));
                }
                // Vertical
                if (row < NUM_ROWS - 1) {
                    constraints.add(new Constraint(particles[row][col], particles[row + 1][col]));
                }
                // Diagonal (to improve stability)
                if (row < NUM_ROWS - 1 && col < NUM_COLS - 1) {
                    constraints.add(new Constraint(particles[row][col], particles[row + 1][col + 1]));
                }
                if (row < NUM_ROWS - 1 && col > 0) {
                    constraints.add(new Constraint(particles[row][col], particles[row + 1][col - 1]));
                }
            }
        }
    }

    /**
     * Called by the timer on each tick. Updates the simulation and repaints the panel.
     *
     * @param e the action event triggered by the timer
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        updateSimulation();
        repaint();
    }

    /**
     * Updates the simulation by:
     * <ul>
     *     <li>Performing Verlet integration for each particle</li>
     *     <li>Applying multiple constraint solving iterations</li>
     * </ul>
     */
    private void updateSimulation() {
        // 1. Verlet Integration
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                Particle p = particles[row][col];
                if (!p.pinned) {
                    double vx = (p.x - p.oldX) * DAMPING;
                    double vy = (p.y - p.oldY) * DAMPING;

                    p.oldX = p.x;
                    p.oldY = p.y;

                    p.x += vx;
                    // Incorporate gravity
                    p.y += vy + GRAVITY * TIME_STEP * TIME_STEP;
                }
            }
        }

        // 2. Constraint Solving
        for (int i = 0; i < CONSTRAINT_ITERATIONS; i++) {
            for (Constraint c : constraints) {
                c.solve();
            }
        }
    }

    /**
     * Renders the cloth simulation:
     * <ul>
     *     <li>Draws all constraints (springs) as lines</li>
     *     <li>Draws each particle as a small filled circle</li>
     * </ul>
     *
     * @param g the {@link Graphics} context in which to do the painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Enable anti-aliasing for smooth rendering
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw constraints
        g2d.setColor(Color.DARK_GRAY);
        for (Constraint c : constraints) {
            g2d.drawLine(
                    (int) c.p1.x,
                    (int) c.p1.y,
                    (int) c.p2.x,
                    (int) c.p2.y
            );
        }

        // Draw particles
        g2d.setColor(Color.RED);
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                Particle p = particles[row][col];
                int r = 3; // radius of the circle
                g2d.fillOval(
                        (int) p.x - r,
                        (int) p.y - r,
                        2 * r,
                        2 * r
                );
            }
        }
    }

    // ------------------------------------------------------------------------
    // Inner Classes
    // ------------------------------------------------------------------------

    /**
     * Represents a single particle in the cloth simulation with
     * position data for Verlet integration.
     */
    private static class Particle {
        /**
         * Current X position of the particle.
         */
        double x;

        /**
         * Current Y position of the particle.
         */
        double y;

        /**
         * Previous X position of the particle (for Verlet integration).
         */
        double oldX;

        /**
         * Previous Y position of the particle (for Verlet integration).
         */
        double oldY;

        /**
         * Indicates whether this particle is pinned (fixed in place).
         */
        boolean pinned = false;

        /**
         * Constructs a {@link Particle} at the specified position.
         *
         * @param x the initial X coordinate
         * @param y the initial Y coordinate
         */
        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.oldX = x;
            this.oldY = y;
        }
    }

    /**
     * Represents a constraint (spring) that enforces a rest length between two particles.
     */
    private static class Constraint {
        /**
         * The first particle.
         */
        final Particle p1;

        /**
         * The second particle.
         */
        final Particle p2;

        /**
         * The rest length between p1 and p2.
         */
        final double restLength;

        /**
         * Constructs a {@link Constraint} for two particles and sets the rest length
         * based on their initial distance.
         *
         * @param p1 the first particle
         * @param p2 the second particle
         */
        Constraint(Particle p1, Particle p2) {
            this.p1 = p1;
            this.p2 = p2;
            this.restLength = Math.hypot(p1.x - p2.x, p1.y - p2.y);
        }

        /**
         * Solves the constraint by adjusting the positions of the particles so
         * that the distance between them matches {@code restLength}.
         */
        void solve() {
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double dist = Math.hypot(dx, dy);

            // Prevent division by zero
            if (dist == 0) {
                return;
            }

            double difference = (dist - restLength) / dist;
            double offsetX = dx * 0.5 * difference;
            double offsetY = dy * 0.5 * difference;

            if (!p1.pinned) {
                p1.x += offsetX;
                p1.y += offsetY;
            }
            if (!p2.pinned) {
                p2.x -= offsetX;
                p2.y -= offsetY;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Mouse and Interaction Events
    // ------------------------------------------------------------------------

    /**
     * Called when the mouse is pressed. Attempts to select the nearest particle within a certain radius.
     *
     * @param e the mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        double minDist = Double.MAX_VALUE;
        Particle candidate = null;

        // Search for the closest particle within a certain radius (20 pixels)
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                Particle p = particles[row][col];
                double dx = p.x - x;
                double dy = p.y - y;
                double dist = Math.hypot(dx, dy);

                if (dist < minDist && dist < 20) {
                    minDist = dist;
                    candidate = p;
                }
            }
        }

        // If the found particle is pinned, don't select it; otherwise, select it
        if (candidate != null && candidate.pinned) {
            selectedParticle = null;
        } else {
            selectedParticle = candidate;
        }

        prevMouseX = x;
        prevMouseY = y;
    }

    /**
     * Called when the mouse is dragged. Moves the selected particle to follow the mouse position.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedParticle != null) {
            int x = e.getX();
            int y = e.getY();
            selectedParticle.x = x;
            selectedParticle.y = y;
        }
        prevMouseX = e.getX();
        prevMouseY = e.getY();
    }

    /**
     * Called when the mouse is released. Deselects any currently selected particle.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        selectedParticle = null;
    }

    // Unused MouseListener and MouseMotionListener methods
    @Override
    public void mouseClicked(MouseEvent e) { /* Not used */ }

    @Override
    public void mouseEntered(MouseEvent e) { /* Not used */ }

    @Override
    public void mouseExited(MouseEvent e) { /* Not used */ }

    @Override
    public void mouseMoved(MouseEvent e) { /* Not used */ }

    // ------------------------------------------------------------------------
    // Main Method
    // ------------------------------------------------------------------------

    /**
     * Main entry point for running the cloth simulation as a standalone application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Cloth Simulation");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            ClothSimulation sim = new ClothSimulation();
            frame.add(sim);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
