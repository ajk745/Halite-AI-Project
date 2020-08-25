## Halite 3 Bot
#### Highest Rank Reached in Finals: #236 - Platinum Division (Top 5% of Bots)
#### Bot Profile: https://2018.halite.io/user/?user_id=3687 (Yes I know, cringey name)

### The Game 
Please read about the rules of the competition here: 
https://halite.io/learn-programming-challenge/game-overview

### The Algorithm
A game last between 400 and 500 turns, depending on map size. On each turn, the bot iterates though all ships, a calculates a move for each one. The bot functions in 3 "phases". 

### Game Phases
1. Early Game - Building Ships 
2. Mid Game - Harvesting Halite
3. End Game - Regrouping all ships back to dropoffs

#### Early game lasts 0.4 of the game in 4 player games, and 0.5 of the game in 2 player games
#### End game starts at a base of 0.95 of the game, but can be a bit earlier depending on how many ships are active

Even though the bot goes through 3 phases, the general procedure for each turn is nearly the same for each turn throughout the whole game, with only minor differences to the algorithm which will be noted.

### Summary of a Turn
Iterate thorugh all active ships, and for each ship:
1. Decide whether the ship will move on that turn
2. If the ship is moving, decide whether the move will be towards a new target to collect more halite, or back towards a dropoff
3. If the ship if moving to a new target, select the best open target for that ship that hasn't been selected by another ship on that turn yet
4. If the ship is moving back to a dropoff, calculate the nearest dropoff as the target
5. If the ship is moving, calculate which directions will take the ship closer to its target, and choose 1 that is marked as a "safe" move that will not result in a collision.
6. Decide whether to convert that ship to a dropoff
7. #### After iteration through all ships is complete, decide whether to spawn a new ship on that turn

### 1. Decide whether the ship will move on that turn
At the start of each turn, each ship is assigned a boolean canMove state. If a ship has sufficient resources to move on that turn, and the current square that the ship is on has less halite than the set "harvestHalite" threshold, that ship has a true canMove state. This means that this ship will be EXPECTED to move, and the space that it is occupying will be expected to be free when the turn is completed.

### 2. If the ship is moving, decide whether the move will be towards a new target to collect more halite, or back towards a dropoff. 
Simple. If a ship has more halite that the "returnHalite" threshold, then that ship will be returning to a dropoff on that turn. Otherwise, if the current square that the ship is on has less halite than the set "harvestHalite" threshold, it will be moving to a new target. If both of these conditions are false, then the canMove state of this ship is expected to be false, and it will not be moving on this turn. 

If the game is currently in end game, then all moving ships will be moving back to drop offs, regardless of the previously mentioned parameters.

### 3. If the ship if moving to a new target, select the best open target for that ship that hasn't been selected by another ship on that turn yet
#### Target Scoring
For each ship, each square on the map is given a "score", where a higher score means a more desirable destination. The score is simply calculated with the formula
#### HALITE_AT_SQUARE ^ 1.2 / RELATIVE_DISTANCE_TO_SQUARE

##### The difference between relative distance, as opposed to true distance, is that in 2 player games, relative distance weights vertical movement as only half of horizonatal movement. Since 2 player games always have the 2 players spawn horizontally from each other, this encourages the ships to move into the free space to the top and bottom, rather than the contested areas to the sides.

#### In 4 player games, squares with many enemy ships around then get a minor score reduction

#### Squares in zones where there is a lot of surrounding halite get a minor score boost

The first ship will pick a target with the highest score. An ArrayList is created on each turn to store all the targets that have already been picked. Each following ship will pick the highest score target that hasn't been picked yet.

### 4. If the ship is moving back to a dropoff, calculate the nearest dropoff as the target. 
If the ship will be returning on this turn, then the code simply iterates though an ArrayList of all the positions of the currently existing dropoffs (including the shipyard) and selects the one with the lowest true distance. That position will become the ships target position. 

### 5. If the ship is moving, calculate which directions will take the ship closer to its target, and choose 1 that is marked as a "safe" move that will not result in a collision.
 
#### A "safe" direction is defined as a direction where, if the ship were to move in that direction, the resulting position does not overlap with another owned ship with a false canMove state, and the resulting position does not overlap with the resulting position of another ship whose direction has already been determined. This means that ships that come first in the iteration get priority in selecting a "safe" direction, and once their direction has been decided, it cannot be changed by ships further along in the iteration. 

#### A direction where the resulting position overlaps another owned ship with a true canMove state is considered "safe", because that ship is expected to move on that turn, created space for the first ship to move into its place. 

#### A direction where the resulting position overlaps with an enemy ship is considered safe ONLY IF the position is closer to my dropoff than the enemies. This means that if the move results in a collision between my ship and the enemy's, my other ships will be closer to the dropped halite, and will pick it up first. 

At the start of each turn, an ArrayList is created that stores all positions resulting from ship moves. When a ship decides to move in a direction, the resulting position is added to the Arraylist, and any move of future ships in the iteration where the resulting position would overlap any position in the arraylist would be considered unsafe. 

Each ship start off by generating which of the 4 moves around it are safe moves. The ship will then generate the 1 or 2 directions that will take it closer to it's target. If both directions that take the ship closer to its target are safe, then the ship picks one randomly. If only one is safe, the ship picks that one. 

If no directions that move the ship closer to its target are safe, the ship will try move in the opposite direction to the shipyard, with the goal of "spreading out". If that isn't safe either, then the ship will pick randomly from the list of safe directions given before.

In the rare case that the ship doesn't have any safe moves, it will just stay still. While this may cause an unavoidable self collision, it is rare enough to where it doesn't have a major impact, and I could not find a way around such scenarios using oly the info given on 1 turn. 

#### When the game is in end game, self collisions on dropoffs are allowed and always considered safe, as any collisions on the dropoff will still deposit all the halite into that drop-off. 

### 6. Decide whether to convert that ship to a dropoff

A ship will be converted to a dropoff simply when it is a given distance away from any other dropoff or the shipyard, until the maximum number of dropoffs has been reached. 

After a given point in the game, dropoffs will stop being produced to save halite, even if the maximum limit has not been reached. 

### 7. After iteration through all ships is complete, decide whether to spawn a new ship on that turn

During the Early Game stage, a new ship is spawned every turn, so long as we have enough halite and the shipyard space is free, and at least 1 square adjacent to it is free as well, to avoid clustering. 

## This seemed to work quite well for how simple the algorithm is. Thanks for Reading!
