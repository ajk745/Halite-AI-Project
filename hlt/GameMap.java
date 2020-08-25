package hlt;

import java.util.ArrayList;

public class GameMap {
    public final int width;
    public final int height;
    public final MapCell[][] cells;

    public GameMap(final int width, final int height) {
        this.width = width;
        this.height = height;

        cells = new MapCell[height][];
        for (int y = 0; y < height; ++y) {
            cells[y] = new MapCell[width];
        }
    }

    public MapCell at(final Position position) {
        final Position normalized = normalize(position);
        return cells[normalized.y][normalized.x];
    }

    public MapCell at(final Entity entity) {
        return at(entity.position);
    }

    public int calculateDistance(final Position source, final Position target) {
        final Position normalizedSource = normalize(source);
        final Position normalizedTarget = normalize(target);

        final int dx = Math.abs(normalizedSource.x - normalizedTarget.x);
        final int dy = Math.abs(normalizedSource.y - normalizedTarget.y);

        final int toroidal_dx = Math.min(dx, width - dx);
        final int toroidal_dy = Math.min(dy, height - dy);

        return toroidal_dx + toroidal_dy;
    }

    public int calculateRelativeDistance(final Position source, final Position target, int gamePlayers,
            Shipyard shipyard) {
        final Position normalizedSource = normalize(source);
        final Position normalizedTarget = normalize(target);

        int dx = Math.abs(normalizedSource.x - normalizedTarget.x);
        int dy = Math.abs(normalizedSource.y - normalizedTarget.y);

        int toroidal_dx = Math.min(dx, width - dx);
        int toroidal_dy = Math.min(dy, height - dy);

        if (gamePlayers == 2) {
            if ((width / 4) < toroidal_dx) {
                toroidal_dx *= 16;
            }
        }

        return toroidal_dx + toroidal_dy;
    }

    public Position normalize(final Position position) {
        final int x = ((position.x % width) + width) % width;
        final int y = ((position.y % height) + height) % height;
        return new Position(x, y);
    }

    public ArrayList<Direction> getUnsafeMoves(final Position source, final Position destination) {
        final ArrayList<Direction> possibleMoves = new ArrayList<>();

        final Position normalizedSource = normalize(source);
        final Position normalizedDestination = normalize(destination);

        final int dx = Math.abs(normalizedSource.x - normalizedDestination.x);
        final int dy = Math.abs(normalizedSource.y - normalizedDestination.y);
        final int wrapped_dx = width - dx;
        final int wrapped_dy = height - dy;

        if (normalizedSource.x < normalizedDestination.x) {
            possibleMoves.add(dx > wrapped_dx ? Direction.WEST : Direction.EAST);
        } else if (normalizedSource.x > normalizedDestination.x) {
            possibleMoves.add(dx < wrapped_dx ? Direction.WEST : Direction.EAST);
        }

        if (normalizedSource.y < normalizedDestination.y) {
            possibleMoves.add(dy > wrapped_dy ? Direction.NORTH : Direction.SOUTH);
        } else if (normalizedSource.y > normalizedDestination.y) {
            possibleMoves.add(dy < wrapped_dy ? Direction.NORTH : Direction.SOUTH);
        }

        return possibleMoves;
    }

    public Direction awayNavigate(Ship ship, Position destination, GameMap gameMap) {
        Direction Tdirection = Direction.STILL;
        ArrayList<Direction> moves = new ArrayList<>();
        for (Direction direction : gameMap.getUnsafeMoves(ship.position, destination)) {
            Position targetPos = directionalOffset(direction, ship.position);
            moves.add(direction);
        }
        if (moves.size() == 0) {
            moves.add(Direction.STILL);
        }
        Tdirection = moves.get((int) (Math.random() * moves.size()));
        return Tdirection.invertDirection();
    }

    public Direction awayNavigateRandom(Ship ship, Position destination, GameMap gameMap) {
        ArrayList<Direction> Tdirection = new ArrayList<>();
        Tdirection.add(Direction.STILL);
        for (Direction direction : gameMap.getUnsafeMoves(ship.position, destination)) {
            Position targetPos = directionalOffset(direction, ship.position);
            Tdirection.add(direction);
        }
        return Tdirection.get((int) (Math.random() * Tdirection.size()));
    }

    public Direction naiveNavigateRandom(Ship ship, Position destination, GameMap gameMap) {
        ArrayList<Direction> Tdirection = new ArrayList<>();
        for (Direction direction : gameMap.getUnsafeMoves(ship.position, destination)) {
            Tdirection.add(direction);
        }
        if (Tdirection.size() == 0) {
            Tdirection.add(Direction.STILL);
        }
        return Tdirection.get((int) (Math.random() * Tdirection.size()));
    }

    public Direction naiveNavigate(final Ship ship, final Position destination) {
        ArrayList<Direction> Tdirection = new ArrayList<>();
        for (final Direction direction : getUnsafeMoves(ship.position, destination)) {
            final Position targetPos = ship.position.directionalOffset(direction);
            if (!at(targetPos).isOccupied()) {
                at(targetPos).markUnsafe(ship);
                Tdirection.add(direction);
            }
        }
        if (Tdirection.size() == 0) {
            Tdirection.add(Direction.STILL);
        }
        return Tdirection.get((int) (Math.random() * Tdirection.size()));
    }

    void _update() {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                cells[y][x].ship = null;
            }
        }

        final int updateCount = Input.readInput().getInt();

        for (int i = 0; i < updateCount; ++i) {
            final Input input = Input.readInput();
            final int x = input.getInt();
            final int y = input.getInt();

            cells[y][x].halite = input.getInt();
        }
    }

    static GameMap _generate() {
        final Input mapInput = Input.readInput();
        final int width = mapInput.getInt();
        final int height = mapInput.getInt();

        final GameMap map = new GameMap(width, height);

        for (int y = 0; y < height; ++y) {
            final Input rowInput = Input.readInput();

            for (int x = 0; x < width; ++x) {
                final int halite = rowInput.getInt();
                map.cells[y][x] = new MapCell(new Position(x, y), halite);
            }
        }

        return map;
    }

    public Position directionalOffset(Direction d, Position p) {
        final int dx;
        final int dy;

        switch (d) {
        case NORTH:
            dx = 0;
            dy = -1;
            break;
        case SOUTH:
            dx = 0;
            dy = 1;
            break;
        case EAST:
            dx = 1;
            dy = 0;
            break;
        case WEST:
            dx = -1;
            dy = 0;
            break;
        case STILL:
            dx = 0;
            dy = 0;
            break;
        default:
            throw new IllegalStateException("Unknown direction " + d);
        }

        return new Position(p.x + dx, p.y + dy);
    }
}
