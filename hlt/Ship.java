package hlt;

import java.util.ArrayList;

public class Ship extends Entity {
    public final int halite;
    public Position curTarget;
    public Direction moveDirection = Direction.STILL;
    public boolean hasCommand = false;
    public boolean canMove = false;

    public Position getClosestDropoff(ArrayList<Position> dropoffs, GameMap gameMap, Shipyard shipyard) {
        int minDistance = 9999;
        Position returnPos = shipyard.position;
        for (int i = 0; i < dropoffs.size(); i++) {
            int tempDistance = gameMap.calculateDistance(position, dropoffs.get(i));
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                returnPos = dropoffs.get(i);
            }
        }
        return returnPos;
    }

    public Ship(final PlayerId owner, final EntityId id, final Position position, final int halite) {
        super(owner, id, position);
        this.halite = halite;
    }

    public void setHasCommand(boolean hasCommand) {
        this.hasCommand = hasCommand;
    }

    public void setCanMove(GameMap gameMap, int harvestHalite, int returnHalite) {
        if ((this.halite > (gameMap.at(this.position).halite * 0.1) - 1)
                && ((gameMap.at(this.position).halite < harvestHalite) || ((this.halite > returnHalite)))) {
            canMove = true;
        } else
            canMove = false;
        Log.log(this.id.id + "Can move: " + this.canMove);
    }

    public void setMoveDirection(Direction moveDirection) {
        this.moveDirection = moveDirection;
    }

    public boolean isFull() {
        return halite >= Constants.MAX_HALITE;
    }

    public Command makeDropoff() {
        return Command.transformShipIntoDropoffSite(id);
    }

    public Command move(final Direction direction) {
        return Command.move(id, direction);
    }

    public Command stayStill() {
        return Command.move(id, Direction.STILL);
    }

    static Ship _generate(final PlayerId playerId) {
        final Input input = Input.readInput();

        final EntityId shipId = new EntityId(input.getInt());
        final int x = input.getInt();
        final int y = input.getInt();
        final int halite = input.getInt();

        return new Ship(playerId, shipId, new Position(x, y), halite);
    }

    public void setCurTarget(Position curTarget) {
        this.curTarget = curTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        Ship ship = (Ship) o;

        return halite == ship.halite;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + halite;
        return result;
    }
}
