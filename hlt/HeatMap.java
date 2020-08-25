package hlt;

import java.util.ArrayList;

public class HeatMap {
    public int heatMapWidth;
    public int heatMapHeight;
    public int avgTotalHalite;
    public OptimizeMapCell[][] heatMap;
    public int tileWidth;
    public int tileHeight;
    public ArrayList<OptimizeMapCell> richPoints;

    public HeatMap(GameMap gameMap, int heatMapWidth, int heatMapHeight, double richPointMultiplier) {
        this.heatMapWidth = heatMapWidth;
        this.heatMapHeight = heatMapHeight;
        this.tileHeight = gameMap.height / this.heatMapWidth;
        this.tileWidth = gameMap.width / this.heatMapHeight;
        this.heatMap = new OptimizeMapCell[heatMapHeight][];
        int sumHalite = 0;
        for (int y = 0; y < heatMapHeight; ++y) {
            this.heatMap[y] = new OptimizeMapCell[heatMapWidth];
        }
        for (int x = 0; x < heatMapWidth; x++) {
            for (int y = 0; y < heatMapHeight; y++) {
                this.heatMap[y][x] = new OptimizeMapCell(y * tileHeight, x * tileWidth, tileWidth, gameMap);
                sumHalite += this.heatMap[y][x].avgHalite;
            }
        }
        this.richPoints = new ArrayList<>();
        this.avgTotalHalite = (int) (sumHalite / (Math.pow(heatMapHeight, 2)));
        for (int x = 0; x < heatMapWidth; x++) {
            for (int y = 0; y < heatMapHeight; y++) {
                if (this.heatMap[x][y].avgHalite > this.avgTotalHalite * richPointMultiplier) {
                    this.richPoints.add(this.heatMap[x][y]);
                }
            }
        }
    }

    public OptimizeMapCell atPos(Position position, GameMap gameMap) {
        Position normalized = gameMap.normalize(position);
        return heatMap[(int) (normalized.y / (tileHeight))][(int) (normalized.x / (tileWidth))];
    }

    public void log() {
        for (int y = 0; y < heatMapHeight; y++) {
            String row = "";
            for (int x = 0; x < heatMapWidth; x++) {
                row += heatMap[x][y].avgHalite + " | ";
            }
            Log.log(row);
        }
    }
}