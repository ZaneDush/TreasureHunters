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

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	//private boolean moved;
	private int navigationMemory;
	private int perceptionRadius;
	private boolean uncoveredTreasure;
	private boolean treasureFound;
	private boolean alone;
	private int areaSideLength;
	private Explorer teamMember;
	private Set<List<Integer>> unknownGridPoints;
	// May need to be lists of GridCell instead of GridPoint
	private Set<List<Integer>> workingMemory;
	private Set<List<Integer>> permanentMemory = new HashSet<List<Integer>>();
	private GridPoint currentLocation;
	private GridPoint closestTreasurePoint;
	private int currentTimeStep;
	private int treasureCount;
	private int treasureValue;
	private int treasureDecayRate;

	public Explorer(ContinuousSpace<Object> space, Grid<Object> grid, int navigationMemory, int perceptionRadius, int treasureCount, int treasureValue, int treasureDecayRate) {
		this.space = space;
		this.grid = grid;
		this.navigationMemory = navigationMemory;
		this.perceptionRadius = perceptionRadius;
		this.treasureCount = treasureCount;
		this.treasureValue = treasureValue;
		this.treasureDecayRate = treasureDecayRate;
		this.uncoveredTreasure = false;
		this.treasureFound = false;
		this.alone = true;
		this.areaSideLength = this.grid.getDimensions().getWidth();
		// Create a set filled with all x,y coordinate pairs in the grid
		this.unknownGridPoints = new HashSet<List<Integer>>();
		for (int i = 0; i < this.areaSideLength; i++) {
			for (int j = 0; j < this.areaSideLength; j++) {
//				point.set(0, i);
//				point.set(1, j);
				List<Integer> point = new ArrayList<Integer>(2);
				point.add(i);
				point.add(j);
				//System.out.println(i + " " + j);
				this.unknownGridPoints.add(point);
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
			GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(this.grid, this.currentLocation,
					Treasure.class, this.perceptionRadius, this.perceptionRadius);
			List<GridCell<Treasure>> gridCells = nghCreator.getNeighborhood(false);
			// > 1 because Explorer agent is included in gridCells due to (true) parameter being passed above
			for (GridCell<Treasure> gc : gridCells) {
				if (gc.size() > 0) {
					this.treasureFound = true;
				}
			}
			// If Explorer has found a treasure
			double minDistance;
			if (this.treasureFound) {
				// Find the closest treasure to the Explorer
				minDistance = Double.MAX_VALUE;
				double distance = 0;
				for (GridCell<Treasure> cell : gridCells) {
					if (cell.size() > 0) {
						distance = grid.getDistance(this.currentLocation, cell.getPoint());
						if (distance < minDistance) {
							this.closestTreasurePoint = cell.getPoint();
							minDistance = distance;
						}
					}
				}
				// If Explorer is on the treasure
				if (minDistance == 0) {
					System.out.println(gridCells.size());
					this.uncoveredTreasure = true;
				} else { // Else Explorer is not on the treasure
					//System.out.println("MADE IT");
					move();
				}
			} else { // Else Explorer has not found a treasure
				if (this.currentTimeStep != 0) {
					// Find the nearest Explorer to this Explorer
					GridCellNgh<Explorer> nghExplorers = new GridCellNgh<Explorer>(grid, currentLocation,
							Explorer.class, this.areaSideLength, this.areaSideLength);
					List<GridCell<Explorer>> explorerGridCells = nghExplorers.getNeighborhood(true);
					//double minDistance;
					Explorer nearestExplorer = null;
					minDistance = Double.MAX_VALUE;
					double distance = 0;
					for (GridCell<Explorer> cell : explorerGridCells) {
						if (cell.size() > 0) {
							distance = grid.getDistance(currentLocation, cell.getPoint());
							if (distance < minDistance) {
								nearestExplorer = cell.items().iterator().next();
								minDistance = distance;
							}
						}
						
					}
					// If both Explorer agents are alone
					if (this.alone && nearestExplorer.alone) {
						// If both Explorer agents decide to not team up
						if (!teamUp(this, nearestExplorer) && !teamUp(nearestExplorer, this)) {
							//System.out.println("MADE IT");
							move();
						} else {
							this.teamMember = nearestExplorer;
							nearestExplorer.teamMember = this;
						}
					} else { // Else at least one of the Explorer agents is not alone
						//System.out.println("MADE IT");
						move();
					}
				} else {
					move();
				}
				
			}
			
			// Keep track of how much time has passed
			this.currentTimeStep++;
			
			//moveTowards(pointWithMostHumans);
			//infect();
		}
	}

	public void move() {
		//		// only move if we are not already in this grid location
		//		if (!pt.equals(grid.getLocation(this))) {
		//			NdPoint myPoint = space.getLocation(this);
		//			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
		//			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
		//					otherPoint);
		//			space.moveByVector(this, 1, angle, 0);
		//			myPoint = space.getLocation(this);
		//			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
		//			moved = true;
		//		}
		
		
		// Initialize this.workingMemory and get the Explorer's current location's coordinates
		this.workingMemory = getPerceptionRegion();
		int explorerX = this.currentLocation.getX();
		int explorerY = this.currentLocation.getY();

		if (this.treasureFound) {
			// Move towards the treasure
			NdPoint myPoint = this.space.getLocation(this);
			NdPoint treasurePoint = new NdPoint(this.closestTreasurePoint.getX(), this.closestTreasurePoint.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(this.space, myPoint, treasurePoint);
			this.space.moveByVector(this, 1, angle, 0);
			myPoint = this.space.getLocation(this);
			this.grid.moveTo(this,  (int) myPoint.getX(), (int) myPoint.getY());
//			int treasureX = this.closestTreasurePoint.getX();
//			int treasureY = this.closestTreasurePoint.getY();
//			if ((explorerX - treasureX) < 0) {
//				grid.moveByVector(this, 1, Direction.EAST);
//			} else if ((explorerX - treasureX) > 0) {
//				grid.moveByVector(this, 1, Direction.WEST);
//			} else if ((explorerY - treasureY) < 0) {
//				grid.moveByVector(this, 1, Direction.NORTH);
//			} else if ((explorerY - treasureY) > 0) {
//				grid.moveByVector(this, 1, Direction.SOUTH);
//			} else {
//				// At treasure location, so uncover the treasure
//				this.uncoveredTreasure = true;
//			}
		} else {
			// Move towards random unknown location
			// Set difference: allGridPoints \ workingMemory, and allGridPoints \ permanentMemory in order to find all unseen grid points
			// this.unknownGridPoints holds all unknown locations
			this.unknownGridPoints.removeAll(this.workingMemory);
			this.unknownGridPoints.removeAll(this.permanentMemory);
			//int[][] allGridPointsArray = (int[][]) this.unknownGridPoints.toArray();
			ArrayList<List<Integer>> setToList = new ArrayList<List<Integer>>(this.unknownGridPoints);
			int randomIndex = RandomHelper.nextIntFromTo(0, setToList.size() - 1);
			//System.out.println(this.unknownGridPoints.size() + " " + setToList.size());
			int x = setToList.get(randomIndex).get(0);
			int y = setToList.get(randomIndex).get(1);
			//System.out.println(this.unknownGridPoints.size());
			//System.out.println(randomIndex + " " +  x + " " + y);
//			if ((explorerX - x) < 0) {
//				grid.moveByVector(this, 1, Direction.EAST);
//			} else if ((explorerX - x) > 0) {
//				grid.moveByVector(this, 1, Direction.WEST);
//			} else if ((explorerY - y) < 0) {
//				grid.moveByVector(this, 1, Direction.NORTH);
//			} else if ((explorerY - y) > 0) {
//				grid.moveByVector(this, 1, Direction.SOUTH);
//			}
			NdPoint myPoint = this.space.getLocation(this);
			NdPoint randomPoint = new NdPoint(x, y);
			double angle = SpatialMath.calcAngleFor2DMovement(this.space, myPoint, randomPoint);
			this.space.moveByVector(this, 1, angle, 0);
			myPoint = this.space.getLocation(this);
			this.grid.moveTo(this,  (int) myPoint.getX(), (int) myPoint.getY());
		}

		// Update this.workingMemory
		Set<List<Integer>> newPerceptionRegion = getPerceptionRegion();
		this.workingMemory.removeAll(newPerceptionRegion);

		// Update this.permanentMemory to contain this.navigationMemory% of locations from this.workingMemory (Note: May need to make sure locations that are already in this.permanentMemory aren't being re-added
		int numLocationsToAdd = (int) Math.ceil(((this.navigationMemory / 100) * this.workingMemory.size()));
		ArrayList<List<Integer>> setToList = new ArrayList<List<Integer>>(this.workingMemory);
		for (int i = 0; i < numLocationsToAdd; i++) {
			int randomIndex = RandomHelper.nextIntFromTo(0, this.workingMemory.size());
			this.permanentMemory.add(setToList.get(randomIndex));
		}
	}

	public Set<List<Integer>> getPerceptionRegion() {
		Set<List<Integer>> perceptionRegion = new HashSet<List<Integer>>();
		//MooreQuery<Object> mooreNeighborhood = new MooreQuery<Object>(this.grid, this, this.perceptionRadius, this.perceptionRadius);
		GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(this.grid, this.currentLocation,
				Treasure.class, this.perceptionRadius, this.perceptionRadius);
		List<GridCell<Treasure>> gridCells = nghCreator.getNeighborhood(false);
		for (GridCell<Treasure> gc : gridCells) {
//			point.set(0, gc.getPoint().getX());
//			point.set(1, gc.getPoint().getY());
			List<Integer> point = new ArrayList<Integer>(2);
			point.add(gc.getPoint().getX());
			point.add(gc.getPoint().getY());
			//System.out.println(point.get(0) + " " + point.get(1));
			perceptionRegion.add(point);
		}
		return perceptionRegion;
	}

	public boolean teamUp(Explorer thisExplorer, Explorer nearestExplorer) {
		double aloneSpeed = thisExplorer.getSearchedAreaSize() / thisExplorer.currentTimeStep;
		int teamSearchedArea = nearestExplorer.getSearchedAreaSize() + thisExplorer.getSearchedAreaSize();
		double teamSpeed = teamSearchedArea / thisExplorer.currentTimeStep;
		double timeToDiscoverTreasureAlone = (thisExplorer.unknownGridPoints.size() / aloneSpeed) / thisExplorer.treasureCount;
		double timeToDiscoverTreasureTeam = (thisExplorer.unknownGridPoints.size() / teamSpeed) / thisExplorer.treasureCount;
		double treasureValueAlone = timeToDiscoverTreasureAlone * thisExplorer.treasureDecayRate;
		double treasureValueTeam = (timeToDiscoverTreasureTeam * thisExplorer.treasureDecayRate) / 2;
		// If thisExplorer receives less when alone as compared to team, then want to team up
		if (treasureValueAlone < treasureValueTeam) {
			return true;
		} else {
			return false;
		}
	}

	public int getSearchedAreaSize() {
		return this.permanentMemory.size() + getPerceptionRegion().size();
	}


	//	public void moveTowards(GridPoint pt) {
	//		// only move if we are not already in this grid location
	//		if (!pt.equals(grid.getLocation(this))) {
	//			NdPoint myPoint = space.getLocation(this);
	//			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
	//			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
	//					otherPoint);
	//			space.moveByVector(this, 1, angle, 0);
	//			myPoint = space.getLocation(this);
	//			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
	//			moved = true;
	//		}
	//	}
	//
	//	public void infect() {
	//		GridPoint pt = grid.getLocation(this);
	//		List<Object> treasures = new ArrayList<Object>();
	//		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
	//			if (obj instanceof Treasure) {
	//				treasures.add(obj);
	//			}
	//		}
	//
	//		if (treasures.size() > 0) {
	//			int index = RandomHelper.nextIntFromTo(0, treasures.size() - 1);
	//			Object obj = treasures.get(index);
	//
	//			NdPoint spacePt = space.getLocation(obj);
	//			Context<Object> context = ContextUtils.getContext(obj);
	//			context.remove(obj);
	//			Zombie zombie = new Zombie(space, grid);
	//			context.add(zombie);
	//			space.moveTo(zombie, spacePt.getX(), spacePt.getY());
	//			grid.moveTo(zombie, pt.getX(), pt.getY());
	//
	//			Network<Object> net = (Network<Object>)context.getProjection("infection network");
	//			net.addEdge(this, zombie);
	//		}
	//	}
}
