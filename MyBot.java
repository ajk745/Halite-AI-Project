import hlt.*;

import java.util.ArrayList;
import java.util.Random;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        game.ready("Bot.v9 Final Touches");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        // Game Memory
        int memoryInt = 0;
        Position curTarget = game.me.shipyard.position;
        ArrayList<Position> curDropOffs = new ArrayList<>();
        curDropOffs.add(game.me.shipyard.position);
        for (;;) {

            // Game Elements Update
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;
            final ArrayList<Command> commandQueue = new ArrayList<>();
            final ArrayList<Direction> commandLog = new ArrayList<>();
            final ArrayList<EntityId> commandOrderLog = new ArrayList<>();
            final ArrayList<Position> movePositions = new ArrayList<>();
            final ArrayList<Integer> movePositionsLog = new ArrayList<>();
            final ArrayList<Position> targetPositions = new ArrayList<>();
            final Shipyard shipyard = game.me.shipyard;
            final int gamePlayers = game.players.size();

            // Constants
            final double richPointMultiplier = 1.15;
            final double minShips = 100;
            int minDropOffDistance = (int) ((int) gameMap.width / 5);
            if (game.players.size() == 4) {
                minDropOffDistance = (int) ((int) gameMap.width / 7);
            }
            final int maxDropOffs;
            switch (gameMap.width) {
            case 32:
                maxDropOffs = 3;
                break;
            case 40:
                maxDropOffs = 3;
                break;
            case 48:
                maxDropOffs = 4;
                break;
            case 56:
                maxDropOffs = 5;
                break;
            case 64:
                maxDropOffs = 6;
                break;
            default:
                maxDropOffs = 5;
                break;
            }

            // Turn Waves
            int spreadTurns = 0;
            int targetResetTurns = 1;
            double midGameTurnRatio = 0.05;
            double richTurnRatio = 0.1;
            double noDropOffTurnRatio = 0.6;
            double endGameTurnRatio = 0.5 + (0.05 * (gameMap.width / 48));
            double finalGameTurnRatio = 0.95 - (0.09 * (double) ((double) me.ships.size() / 600));

            // HeatMap Update
            HeatMap heatMap = new HeatMap(gameMap, 8, 8, richPointMultiplier);
            Log.log("HeatMap Updated. " + "Average Halite " + heatMap.avgTotalHalite + " Richpoints "
                    + heatMap.richPoints);
            heatMap.log();

            // Shipyard check
            boolean openShipYard = gameMap.at(shipyard).isOccupied();

            // gameVars
            double spreadRandom = 0; // Obsolete, remove soon
            int minSpawnAvgHalite = 90;
            int avgHaliteCap = 300;
            int returnHalite = 700;
            int harvestHalite = (int) ((heatMap.atPos(shipyard.position, gameMap).avgHalite) / 2);

            if (game.turnNumber > Constants.MAX_TURNS * richTurnRatio) {
                switch (gameMap.width) {
                case 32:
                    returnHalite = 660;
                    harvestHalite = (int) ((heatMap.avgTotalHalite / 2.6));
                    // if (game.players.size() == 4) {
                    // harvestHalite = (int) ((heatMap.avgTotalHalite / 2));
                    // }
                    break;
                case 40:
                    returnHalite = 670;
                    harvestHalite = (int) ((heatMap.avgTotalHalite / 2.7));
                    // if (game.players.size() == 4) {
                    // harvestHalite = (int) ((heatMap.avgTotalHalite / 2));
                    // }
                    break;
                case 48:
                    returnHalite = 700;
                    harvestHalite = (int) ((heatMap.avgTotalHalite / 3));
                    // if (game.players.size() == 4) {
                    // harvestHalite = (int) ((heatMap.avgTotalHalite / 2));
                    // }
                    break;
                case 56:
                    returnHalite = 700;
                    harvestHalite = (int) ((heatMap.avgTotalHalite / 2.8));
                    // if (game.players.size() == 4) {
                    // harvestHalite = (int) ((heatMap.avgTotalHalite / 2));
                    //
                    break;
                case 64:
                    returnHalite = 700;
                    harvestHalite = (int) ((heatMap.avgTotalHalite / 3));
                    // if (game.players.size() == 4) {
                    // harvestHalite = (int) ((heatMap.avgTotalHalite / 2));
                    // }
                    break;
                }
            }

            // Checks if ship will move and sets CanMove var
            for (final Ship ship : me.ships.values()) {
                ship.setCanMove(gameMap, harvestHalite, returnHalite);
            }

            // Ship Movement Calculations
            for (final Ship ship : me.ships.values()) {
                Log.log("============= SHIP " + ship.id.id + " =============");
                // Set Targets
                Position oldTarget = shipyard.position;
                if (game.turnNumber % targetResetTurns == 0) {
                    ship.setCurTarget(getBalancedRichPoint(ship.position, heatMap.richPoints, targetPositions, gameMap,
                            gamePlayers, shipyard));
                    if (ship.curTarget == shipyard.position) {
                        ship.setCurTarget(getClosestRichPoint(ship.position, heatMap.richPoints, gameMap));
                    }
                    Log.log("Position " + ship.curTarget.x + ", " + ship.curTarget.y + " has targetShips total of "
                            + gameMap.at(ship).targetShips);
                }

                if (ship.curTarget == null) {
                    ship.setCurTarget(shipyard.position);
                }

                if (game.turnNumber >= targetResetTurns) {
                    curTarget = ship.curTarget;
                }

                // Ship conversion to dropoff conversion
                if (game.turnNumber < (Constants.MAX_TURNS * noDropOffTurnRatio) && me.halite > Constants.DROPOFF_COST
                        && curDropOffs.size() < maxDropOffs && heatMap.avgTotalHalite > minSpawnAvgHalite) {
                    if (gameMap.calculateDistance(ship.position,
                            ship.getClosestDropoff(curDropOffs, gameMap, shipyard)) > minDropOffDistance) {
                        curDropOffs.add(ship.position);
                        Log.log(ship.id + "Converting to Dropoff " + ship.position);
                        Log.log("Average halite around Dropoff: " + heatMap.atPos(ship.position, gameMap).avgHalite);
                        me.halite = me.halite - Constants.DROPOFF_COST;
                        commandQueue.add(ship.makeDropoff());
                        continue;
                    }

                }

                if (game.turnNumber <= spreadTurns && (ship.canMove)) {
                    Direction moveDirection = outDirection(ship, ship.position, gameMap, movePositions, shipyard);
                    commandQueue.add(ship.move(moveDirection));
                    commandLog.add(moveDirection);
                    ship.setMoveDirection(moveDirection);
                    movePositions.add(directionalOffset(moveDirection, ship.position, gameMap));
                    movePositionsLog.add(directionalOffset(moveDirection, ship.position, gameMap).hashCode());
                }

                else if ((game.turnNumber > spreadTurns) && (game.turnNumber < Constants.MAX_TURNS * finalGameTurnRatio)
                        && (ship.canMove)) {
                    if (((ship.halite > returnHalite) && (ship.canMove) && (gameMap.at(ship).halite < harvestHalite))
                            || ship.halite > 950) {
                        Direction moveDirection = returnDirection(ship, ship.position, gameMap, movePositions, shipyard,
                                openShipYard, curDropOffs);
                        commandQueue.add(ship.move(moveDirection));
                        commandLog.add(moveDirection);
                        ship.setMoveDirection(moveDirection);
                        movePositions.add(directionalOffset(moveDirection, ship.position, gameMap));
                        movePositionsLog.add(directionalOffset(moveDirection, ship.position, gameMap).hashCode());
                    }

                    else if ((gameMap.at(ship).halite < harvestHalite) && (ship.canMove)) {
                        Direction moveDirection = harvestDirection(ship, ship.position, gameMap, movePositions,
                                targetPositions, harvestHalite, shipyard, spreadRandom, curTarget, oldTarget, game);
                        commandQueue.add(ship.move(moveDirection));
                        commandLog.add(moveDirection);
                        ship.setMoveDirection(moveDirection);
                        movePositions.add(directionalOffset(moveDirection, ship.position, gameMap));
                        movePositionsLog.add(directionalOffset(moveDirection, ship.position, gameMap).hashCode());
                    }

                    else {
                        Direction moveDirection = stillDirection(ship, ship.position, gameMap, movePositions, shipyard);
                        commandQueue.add(ship.move(moveDirection));
                        commandLog.add(moveDirection);
                        ship.setMoveDirection(moveDirection);
                        movePositions.add(directionalOffset(moveDirection, ship.position, gameMap));
                        movePositionsLog.add(directionalOffset(moveDirection, ship.position, gameMap).hashCode());
                    }

                } else if ((ship.canMove)) {
                    Direction moveDirection = finalReturnDirection(ship, ship.position, gameMap, movePositions,
                            shipyard, curDropOffs);
                    commandQueue.add(ship.move(moveDirection));
                    commandLog.add(moveDirection);
                    ship.setMoveDirection(moveDirection);
                    movePositions.add(directionalOffset(moveDirection, ship.position, gameMap));
                    movePositionsLog.add(directionalOffset(moveDirection, ship.position, gameMap).hashCode());
                }
                commandOrderLog.add(ship.id);
                ship.setHasCommand(true);
                Log.log("\n");
            }

            // New ship spawn calculations
            if ((((game.turnNumber <= Constants.MAX_TURNS * midGameTurnRatio && me.halite >= Constants.SHIP_COST)
                    || ((me.ships.size() < minShips) && game.turnNumber <= Constants.MAX_TURNS * endGameTurnRatio
                            && me.halite >= Constants.SHIP_COST)))
                    && (!gameMap.at(shipyard).isOccupied() && (!moveSearch(movePositions, shipyard.position)))) {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
            Log.log("======== COMMAND INPUTS =======");
            Log.log("Ship commands in order" + commandOrderLog);
            Log.log("Turn with commandQueue" + commandLog);
            Log.log("ArrayList movePositions populated" + movePositions);
            Log.log("ArrayList targetPositions populated" + targetPositions);
            Log.log("ArrayList dropoffPositions populated" + curDropOffs);
            Log.log("\n\n\n\n");

        }
    }

    public static Position getClosestRichPoint(Position position, ArrayList<OptimizeMapCell> richPoints,
            GameMap gameMap) {
        int lowestDistance = 100;
        Position closestPoint = position;
        for (int i = 0; i < richPoints.size(); i++) {
            int distance = gameMap.calculateDistance(position,
                    new Position(richPoints.get(i).centerX, richPoints.get(i).centerY));
            if (distance < lowestDistance) {
                lowestDistance = distance;
                closestPoint = new Position(richPoints.get(i).centerX, richPoints.get(i).centerY);
            }
        }
        Log.log("New Halite target set " + closestPoint.x + ", " + closestPoint.y);
        return closestPoint;
    }

    public static Position getBestRichPoint(Position position, ArrayList<OptimizeMapCell> richPoints, GameMap gameMap) {
        int lowestHalite = 100;
        Position closestPoint = position;
        for (int i = 0; i < richPoints.size(); i++) {
            int halite = richPoints.get(i).avgHalite;
            int distance = gameMap.calculateDistance(position,
                    new Position(richPoints.get(i).centerX, richPoints.get(i).centerY));
            if (halite > lowestHalite && distance < gameMap.width / 2) {
                lowestHalite = halite;
                closestPoint = new Position(richPoints.get(i).centerX, richPoints.get(i).centerY);
            }
        }
        Log.log("New Halite target set " + closestPoint.x + closestPoint.y);
        return closestPoint;
    }

    public static Position getBalancedRichPoint(Position position, ArrayList<OptimizeMapCell> richPoints,
            ArrayList<Position> targetPositions, GameMap gameMap, int gamePlayers, Shipyard shipyard) {
        double bestVal = 0;
        Position bestPosition = position;
        int bestDistance = (gameMap.height + gameMap.width) / 2;
        for (int x = 0; x < gameMap.width; ++x)
            for (int y = 0; y < gameMap.height; ++y) {
                Position tempPosition = new Position(x, y);
                if (tempPosition.equals(position) || moveSearch(targetPositions, tempPosition)) {
                    continue;
                }

                double tempVal;
                int tempDistance = bestDistance = gameMap.calculateRelativeDistance(position, tempPosition, gamePlayers,
                        shipyard);
                int totalDistance = gameMap.calculateRelativeDistance(position, tempPosition, gamePlayers, shipyard);
                if (totalDistance != 0) {
                    tempVal = Math.pow((gameMap.at(tempPosition).halite), 1.2) / totalDistance;
                    if (tempVal >= bestVal) {
                        bestVal = tempVal;
                        bestPosition = tempPosition;
                        bestDistance = gameMap.calculateRelativeDistance(position, bestPosition, gamePlayers, shipyard);
                    }
                }
            }
        Log.log("New Halite target set " + bestPosition.x + ", " + bestPosition.y);
        return bestPosition;
    }

    public static ArrayList<Direction> safeDirectionArray(Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Ship ship) {
        ArrayList<Direction> safeDirectionsList = new ArrayList<>(Direction.ALL_CARDINALS);
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move) || gameMap.at(move).hasStructure()) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move) || gameMap.at(move).hasStructure()) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move) || gameMap.at(move).hasStructure()) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move) || gameMap.at(move).hasStructure()) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move) || gameMap.at(move).hasStructure()) {
                safeDirectionsList.remove(point);
            }
        }
        if ((safeDirectionsList.size() == 0)) {
            safeDirectionsList.add(Direction.STILL);
            return safeDirectionsList;
        } else
            return safeDirectionsList;

    }

    public static ArrayList<Direction> safeDirectionArrayReturn(Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Ship ship) {
        ArrayList<Direction> safeDirectionsList = new ArrayList<>(Direction.ALL_CARDINALS);
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move)) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move)) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move)) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move)) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && gameMap.at(move).ship.owner == ship.owner
                    && gameMap.at(move).ship.moveDirection == Direction.STILL && !gameMap.at(move).ship.canMove)
                    || moveSearch(movePositions, move)) {
                safeDirectionsList.remove(point);
            }
        }
        if ((safeDirectionsList.size() == 0)) {
            safeDirectionsList.add(Direction.STILL);
            return safeDirectionsList;
        } else
            return safeDirectionsList;

    }

    public static ArrayList<Direction> safeDirectionArrayReturnFinal(Position position, GameMap gameMap,
            ArrayList<Position> movePositions) {
        ArrayList<Direction> safeDirectionsList = new ArrayList<>(Direction.ALL_CARDINALS);
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && !gameMap.at(move).hasStructure())
                    || (moveSearch(movePositions, move) && !gameMap.at(move).hasStructure())) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && !gameMap.at(move).hasStructure())
                    || (moveSearch(movePositions, move) && !gameMap.at(move).hasStructure())) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && !gameMap.at(move).hasStructure())
                    || (moveSearch(movePositions, move) && !gameMap.at(move).hasStructure())) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && !gameMap.at(move).hasStructure())
                    || (moveSearch(movePositions, move) && !gameMap.at(move).hasStructure())) {
                safeDirectionsList.remove(point);
            }
        }
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if ((gameMap.at(move).isOccupied() && !gameMap.at(move).hasStructure())
                    || (moveSearch(movePositions, move) && !gameMap.at(move).hasStructure())) {
                safeDirectionsList.remove(point);
            }
        }
        if ((safeDirectionsList.size() == 0)) {
            safeDirectionsList.add(Direction.STILL);
            return safeDirectionsList;
        } else
            return safeDirectionsList;

    }

    public static Direction stillDirection(Ship ship, Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Shipyard shipyard) {
        if (moveSearch(movePositions, ship.position)) {
            ArrayList<Direction> safeDirections = safeDirectionArray(position, gameMap, movePositions, ship);
            Log.log(ship.id + " Making Space " + safeDirections);
            return safeDirections.get((int) (Math.random() * safeDirections.size()));
        } else
            Log.log(ship.id + "Staying still to Harvest");
        return Direction.STILL;
    }

    public static Direction outDirection(Ship ship, Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Shipyard shipyard) {
        ArrayList<Direction> safeDirections = safeDirectionArray(position, gameMap, movePositions, ship);
        Direction outDirection = gameMap.awayNavigate(ship, shipyard.position, gameMap);
        Log.log(ship.id + " Spreading " + safeDirections);
        Log.log(" Spreading " + outDirection);
        if ((moveSearch(safeDirections, outDirection)))
            return outDirection;
        else
            return safeDirections.get((int) (Math.random() * safeDirections.size()));
    }

    public static Direction harvestDirection(Ship ship, Position position, GameMap gameMap,
            ArrayList<Position> movePositions, ArrayList<Position> targetPositions, int harvestHalite,
            Shipyard shipyard, double spreadRandom, Position curTarget, Position oldTarget, Game game) {
        int maxHalite = harvestHalite;
        ArrayList<Direction> safeDirections = safeDirectionArray(position, gameMap, movePositions, ship);
        Direction newTargetDirection = gameMap.naiveNavigate(ship, curTarget);
        if ((gameMap.calculateDistance(ship.position, curTarget) != 0)) {
            if (moveSearch(safeDirections, newTargetDirection)) {
                Log.log(ship.id.id + " Moving to new target " + newTargetDirection);
                targetPositions.add(curTarget);
                return newTargetDirection;
            }
        }
        if (gameMap.calculateDistance(ship.position, shipyard.position) == 0) {
            if (safeDirections.size() > 0) {
                return safeDirections.get((int) (Math.random() * safeDirections.size()));
            } else
                return Direction.STILL;
        }
        safeDirections.remove(gameMap.naiveNavigate(ship, shipyard.position));
        Direction awayDirection = gameMap.awayNavigate(ship, shipyard.position, gameMap);
        if (moveSearch(safeDirections, awayDirection) && Math.random() < spreadRandom) {
            Log.log("Moving in generic direction " + awayDirection);
            return awayDirection;
        } else {
            Log.log(ship.id + " No spots found. Moving Randomly" + safeDirections);
            if (safeDirections.size() == 0) {
                safeDirections.add(Direction.STILL);
            }
            return safeDirections.get((int) (Math.random() * safeDirections.size()));
        }
    }

    public static Direction returnDirection(Ship ship, Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Shipyard shipyard, boolean openShipYard,
            ArrayList<Position> curDropoffs) {
        ArrayList<Direction> safeDirections = safeDirectionArrayReturn(position, gameMap, movePositions, ship);
        Direction outDirection = gameMap.naiveNavigateRandom(ship,
                ship.getClosestDropoff(curDropoffs, gameMap, shipyard), gameMap);
        Log.log(ship.id + " Returning " + safeDirections);
        Log.log("Returning " + outDirection + " Avaliable exits " + shipyardBorder(shipyard, gameMap)
                + " Yard Occupied " + openShipYard);
        if ((moveSearch(safeDirections, outDirection)))
            return outDirection;
        if (moveSearch(movePositions, ship.position)) {
            return safeDirections.get((int) (Math.random() * safeDirections.size()));
        } else
            return Direction.STILL;
    }

    public static Direction finalReturnDirection(Ship ship, Position position, GameMap gameMap,
            ArrayList<Position> movePositions, Shipyard shipyard, ArrayList<Position> curDropoffs) {
        ArrayList<Direction> safeDirections = safeDirectionArrayReturn(position, gameMap, movePositions, ship);
        Direction outDirection = gameMap.naiveNavigate(ship, ship.getClosestDropoff(curDropoffs, gameMap, shipyard));
        Log.log(ship.id + " Returning " + safeDirections);
        Log.log("Returning " + outDirection);
        if (gameMap.calculateDistance(ship.position, ship.getClosestDropoff(curDropoffs, gameMap, shipyard)) == 0) {
            return Direction.STILL;
        }
        if (gameMap.calculateDistance(ship.position, ship.getClosestDropoff(curDropoffs, gameMap, shipyard)) == 1) {
            return gameMap.naiveNavigateRandom(ship, ship.getClosestDropoff(curDropoffs, gameMap, shipyard), gameMap);
        }
        if ((moveSearch(safeDirections, outDirection))) {
            return outDirection;
        } else {
            return Direction.STILL;
        }
    }

    public static boolean moveSearch(ArrayList<Position> movePositions, Position position) {
        boolean search = false;
        for (int i = 0; i < movePositions.size(); i++) {
            if (movePositions.get(i).equals(position)) {
                search = true;
                break;
            }

        }
        return search;
    }

    public static boolean moveSearch(ArrayList<Direction> moveDirections, Direction direction) {
        boolean search = false;
        for (int i = 0; i < moveDirections.size(); i++) {
            if (moveDirections.get(i) == direction) {
                search = true;
                break;
            }

        }
        return search;
    }

    public static Position directionalOffset(Direction d, Position p, GameMap gameMap) {
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
        int newX = (p.x + dx);
        int newY = (p.y + dy);
        if (newX == -1) {
            newX = gameMap.width - 1;
        }
        if (newY == -1) {
            newY = gameMap.height - 1;
        }
        return new Position(newX % gameMap.width, newY % gameMap.height);
    }

    public static int shipyardBorder(Shipyard shipyard, GameMap gameMap) {
        Position position = shipyard.position;
        ArrayList<Direction> safeDirectionsList = new ArrayList<>(Direction.ALL_CARDINALS);
        ArrayList<Direction> borderList = new ArrayList<>(Direction.ALL_CARDINALS);
        for (int i = 0; i < safeDirectionsList.size(); i++) {
            Direction point = safeDirectionsList.get(i);
            Position move = directionalOffset(point, position, gameMap);
            if (gameMap.at(move).isOccupied()) {
                borderList.remove(point);
            }
        }
        return borderList.size();
    }

}
