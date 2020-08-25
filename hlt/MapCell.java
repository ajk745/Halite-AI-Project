package hlt;

public class MapCell {
    public final Position position;
    public int halite;
    public Ship ship;
    public Entity structure;
    public int targetShips;

    public MapCell(final Position position, final int halite) {
        this.position = position;
        this.halite = halite;
    }

    public void addTarget() {
        targetShips++;
    }

    public boolean isEmpty() {
        return ship == null && structure == null;
    }

    public boolean isOccupied() {
        return ship != null;
    }

    public boolean hasStructure() {
        return structure != null;
    }

    public void markUnsafe(final Ship ship) {
        this.ship = ship;
    }
}