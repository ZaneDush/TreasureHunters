package treasureHunters;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import treasureHunters.Treasure;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.query.space.grid.MooreQuery;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.Direction;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Explorer {

	//private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	//private boolean moved;
	private int navigationMemory;
	private int perceptionRadius;
	private boolean uncoveredTreasure;
	private boolean treasureFound;
	private boolean alone;
	private int areaSideLength;
	private Explorer teamMember;
	private Set<int[]> allGridPoints;
	// May need to be lists of GridCell instead of GridPoint
	private Set<int[]> workingMemory;
	private Set<int[]> permanentMemory;
	private GridPoint currentLocation;
	private GridPoint closestTreasurePoint;

	public Explorer(/*ContinuousSpace<Object> space, */Grid<Object> grid, int navigationMemory, int perceptionRadius) {
		//this.space = space;
		this.grid = grid;
		this.navigationMemory = navigationMemory;
		this.perceptionRadius = perceptionRadius;
		this.uncoveredTreasure = false;
		this.treasureFound = false;
		this.alone = true;
		this.areaSideLength = this.grid.getDimensions().getWidth();
		// Create a set filled with all x,y coordinate pairs in the grid
		this.allGridPoints = new HashSet<int[]>();
		int[] point = new int[2];
		for (int i = 0; i < this.grid.getDimensions().getHeight(); i++) {
			for (int j = 0; j < this.grid.getDimensions().getWidth(); j++) {
				point[0] = i;
				point[1] = j;
				this.allGridPoints.add(point);
			}
		}
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void exploring() { //step()
		// See if this Explorer has uncovered a treasure
		if (this.uncoveredTreasure == false) {
			// Get the current grid location of this Explorer
			this.currentLocation = grid.getLocation(this);

			// Use the GridCellNgh class to create GridCells in order to find any treasures within the perceptionRadius
			GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(grid, currentLocation,
					Treasure.class, this.perceptionRadius, this.perceptionRadius);
			List<GridCell<Treasure>> gridCells = nghCreator.getNeighborhood(true);
			// > 1 because Explorer agent is included in gridCells due to (true) parameter being passed above
			if (gridCells.size() > 1) {
				this.treasureFound = true;
			}
			// If Explorer has found a treasure
			double minDistance;
			if (this.treasureFound) {
				// Find the closest treasure to the Explorer
				minDistance = Double.MAX_VALUE;
				double distance = 0;
				for (GridCell<Treasure> cell : gridCells) {
					distance = grid.getDistance(this.currentLocation, cell.getPoint());
					if (distance < minDistance) {
						this.closestTreasurePoint = cell.getPoint();
						minDistance = distance;
					}
				}
				// If Explorer is on the treasure
				if (minDistance == 0) {
					this.uncoveredTreasure = true;
				} else { // Else Explorer is not on the treasure
					move();
				}
			} else { // Else Explorer has not found a treasure
				// Find the nearest Explorer to this Explorer
				GridCellNgh<Explorer> nghExplorers = new GridCellNgh<Explorer>(grid, currentLocation,
						Explorer.class, this.areaSideLength, this.areaSideLength);
				List<GridCell<Explorer>> explorerGridCells = nghExplorers.getNeighborhood(true);
				//double minDistance;
				Explorer nearestExplorer = null;
				minDistance = Double.MAX_VALUE;
				double distance = 0;
				for (GridCell<Explorer> cell : explorerGridCells) {
					distance = grid.getDistance(currentLocation, cell.getPoint());
					if (distance < minDistance) {
						nearestExplorer = cell.items().iterator().next();
						minDistance = distance;
					}
				}
				// If both Explorer agents are alone
				if (this.alone && nearestExplorer.alone) {
					// If both Explorer agents decide to not team up
					if (!teamUp(nearestExplorer)) {
						move();
					} else {
						this.teamMember = nearestExplorer;
						nearestExplorer.teamMember = this;
					}
				} else { // Else at least one of the Explorer agents is not alone
					move();
				}
			}

			//moveTowards(pointWithMostHumans);
			//infect();
		}
	}

	public void move() {
		// Initialize this.workingMemory and get the Explorer's current location's coordinates
		this.workingMemory = getPerceptionRegion();
		int explorerX = this.currentLocation.getX();
		int explorerY = this.currentLocation.getY();
		
		if (this.treasureFound) {
			// Move towards the treasure
			int treasureX = this.closestTreasurePoint.getX();
			int treasureY = this.closestTreasurePoint.getY();
			if ((explorerX - treasureX) < 0) {
				grid.moveByVector(this, 1, Direction.EAST);
			} else if ((explorerX - treasureX) > 0) {
				grid.moveByVector(this, 1, Direction.WEST);
			} else if ((explorerY - treasureY) < 0) {
				grid.moveByVector(this, 1, Direction.NORTH);
			} else if ((explorerY - treasureY) > 0) {
				grid.moveByVector(this, 1, Direction.SOUTH);
			} else {
				// At treasure location, so uncover the treasure
				this.uncoveredTreasure = true;
			}
		} else {
			// Move towards random unknown location
			// Set difference: allGridPoints \ workingMemory, and allGridPoints \ permanentMemory in order to find all unseen grid points
			// this.allGridPoints holds all unknown locations
			this.allGridPoints.removeAll(this.workingMemory);
			this.allGridPoints.removeAll(this.permanentMemory);
			int[][] allGridPointsArray = (int[][]) this.allGridPoints.toArray();
			int randomIndex = RandomHelper.nextIntFromTo(0, allGridPointsArray.length);
			int x = allGridPointsArray[randomIndex][0];
			int y = allGridPointsArray[randomIndex][1];
			if ((explorerX - x) < 0) {
				grid.moveByVector(this, 1, Direction.EAST);
			} else if ((explorerX - x) > 0) {
				grid.moveByVector(this, 1, Direction.WEST);
			} else if ((explorerY - y) < 0) {
				grid.moveByVector(this, 1, Direction.NORTH);
			} else if ((explorerY - y) > 0) {
				grid.moveByVector(this, 1, Direction.SOUTH);
			}
		}
		
		// Update this.workingMemory
		Set<int[]> newPerceptionRegion = getPerceptionRegion();
		this.workingMemory.removeAll(newPerceptionRegion);
		
		// Update this.permanentMemory to contain this.navigationMemory% of locations from this.workingMemory (Note: May need to make sure locations that are already in this.permanentMemory aren't being re-added
		int numLocationsToAdd = (int) Math.ceil((this.navigationMemory * this.workingMemory.size()));
		for (int i = 0; i < numLocationsToAdd; i++) {
			int randomIndex = RandomHelper.nextIntFromTo(0, this.workingMemory.size());
			this.permanentMemory.add((int[]) this.workingMemory.toArray()[randomIndex]);
		}
	}

	public Set<int[]> getPerceptionRegion() {
		Set<int[]> perceptionRegion = new HashSet<int[]>();
		int[] point = new int[2];
		MooreQuery mooreNeighborhood = new MooreQuery(this.grid, this, this.perceptionRadius);
		for (Object obj : mooreNeighborhood.query()) {
			GridCell gc = (GridCell) obj;
			point[0] = gc.getPoint().getX();
			point[1] = gc.getPoint().getY();
			perceptionRegion.add(point);	
		}
		return perceptionRegion;
	}

	public boolean teamUp(Explorer nearestExplorer) {
//		unknownArea = totalArea - explorerAgent.getSearchedArea()
//		currentSpeed = explorerAgent.getSearchedArea() / timeTaken	/*Get speed at 
//			which new
//			areas have
//			been
//			searched*/
//		prospectiveSearchedArea = nearestExplorerAgent.getSearchedArea() + explorerAgent.getSearchedArea() 
//		potentialSpeed = prospectiveSearchedArea / timeTaken 	/*Get speed at which 
//																new areas can be searched with new search area*/
//		timeToDiscoverTreasureCurrent = (unknownArea / currentSpeed) / numTreasures
//		timeToDiscoverTreasurePotential = (unknownArea / potentialSpeed) / numTreasures
//		treasureValueWithCurrent = timeToDiscoverTreasureCurrent * treasureDecayRate
//		treasureValueWithTeam = timeToDiscoverTreasurePotential * treasureDecayRate
//		individualPayoutCurrent = treasureValueWithCurrent / numTeamMembers
//		individualPayoutPotential = treasureValueWithTeam / numTeamMembers + 1
//		If individualPayoutCurrent < individualPayoutPotential then
//		     return true
//		Else 
//			return false

		double currentSpeed = this.getSearchedArea() /
	}
	
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
					otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
			moved = true;
		}
	}

	public void infect() {
		GridPoint pt = grid.getLocation(this);
		List<Object> treasures = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Treasure) {
				treasures.add(obj);
			}
		}

		if (treasures.size() > 0) {
			int index = RandomHelper.nextIntFromTo(0, treasures.size() - 1);
			Object obj = treasures.get(index);

			NdPoint spacePt = space.getLocation(obj);
			Context<Object> context = ContextUtils.getContext(obj);
			context.remove(obj);
			Zombie zombie = new Zombie(space, grid);
			context.add(zombie);
			space.moveTo(zombie, spacePt.getX(), spacePt.getY());
			grid.moveTo(zombie, pt.getX(), pt.getY());

			Network<Object> net = (Network<Object>)context.getProjection("infection network");
			net.addEdge(this, zombie);
		}
	}
}
