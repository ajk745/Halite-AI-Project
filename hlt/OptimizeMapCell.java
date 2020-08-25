package hlt;

public class OptimizeMapCell {
    public int width;
    public int cornerX;
    public int cornerY;
    public int centerX;
    public int centerY;
    public int avgHalite;

    public OptimizeMapCell(int cornerX, int cornerY, int width, GameMap gm) {
        this.cornerX = cornerX;
        this.cornerY = cornerY;
        this.centerX = this.cornerX + (width / 2);
        this.centerY = this.cornerY + (width / 2);
        this.width = width;
        this.avgHalite = findAvgHalite(gm);
    }

    private int findAvgHalite(GameMap gm) {
        int sumHalite = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                sumHalite += gm.cells[cornerY + y][cornerX + x].halite;
            }
        }
        return (int) (sumHalite / Math.pow(width, 2));
    }

    public String toString() {
        return "" + avgHalite;
    }
}
