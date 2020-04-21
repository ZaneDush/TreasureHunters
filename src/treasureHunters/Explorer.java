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

	private Grid<Object> grid;
	private double navigationMemory;
	private int perceptionRadius;
	private boolean uncoveredTreasure;
	private boolean treasureFound;
	private boolean alone;
	private int areaSideLength;
	private Explorer teamMember;
	// unknownGridPoints holds all unknown locations
	private Set<List<Integer>> unknownGridPoints;
	private Set<List<Integer>> workingMemory;
	private Set<List<Integer>> permanentMemory = new HashSet<List<Integer>>();
	private GridPoint currentLocation;
	private GridPoint startingLocation;
	private GridPoint closestTreasurePoint;
	private int currentTimeStep = 0;
	private int treasureCount;
	private double treasureValue;
	private double treasureDecayRate;
	private List<Integer> randomLocation;
	private boolean flag = false;
	private int count;
	private Context<Object> context;
	private double finalUtility;
	private int finalTimeTick;

	public Explorer(Grid<Object> grid, double navigationMemory, int perceptionRadius, int treasureCount, double treasureValue, double treasureDecayRate, int count) {
		this.grid = grid;
		this.navigationMemory = navigationMemory;
		this.perceptionRadius = perceptionRadius;
		this.treasureCount = treasureCount;
		this.treasureValue = treasureValue;
		this.treasureDecayRate = treasureDecayRate;
		this.count = count;
		this.uncoveredTreasure = false;
		this.treasureFound = false;
		this.alone = true;
		this.areaSideLength = this.grid.getDimensions().getWidth();
		// Create a set filled with all x,y coordinate pairs in the grid
		this.unknownGridPoints = new HashSet<List<Integer>>();
		for (int i = 0; i < this.areaSideLength; i++) {
			for (int j = 0; j < this.areaSideLength; j++) {
				List<Integer> point = new ArrayList<Integer>(2);
				point.add(i);
				point.add(j);
				this.unknownGridPoints.add(point);
			}
		}
		this.startingLocation = this.grid.getLocation(this);

	}

	@ScheduledMethod(start = 1, interval = 1)
	public void exploring() {
		// Set the context
		this.context = ContextUtils.getContext(this);
		// See if this Explorer has uncovered a treasure
		if (this.uncoveredTreasure == false) {
			// Get the current grid location of this Explorer
			this.currentLocation = this.grid.getLocation(this);
			// Use the GridCellNgh class to create GridCells in order to find any treasures within the perceptionRadius
			GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(this.grid, this.currentLocation,
					Treasure.class, this.perceptionRadius, this.perceptionRadius);
			List<GridCell<Treasure>> gridCells = nghCreator.getNeighborhood(false);
			for (GridCell<Treasure> gc : gridCells) {
				if (gc.size() > 0) {
					this.treasureFound = true;
				}
			}
			// If Explorer has found a treasure
			double minDistance;
			if (this.treasureFound) {
				if (this.closestTreasurePoint == null) {
					// Find the closest treasure to the Explorer
					minDistance = Double.MAX_VALUE;
					double distance = 0;
					for (GridCell<Treasure> cell : gridCells) {
						if (cell.size() > 0) {
							distance = grid.getDistance(this.currentLocation, cell.getPoint());
							if (distance < minDistance) {
								//System.out.println(distance);
								this.closestTreasurePoint = cell.getPoint();
								minDistance = distance;
							}
						}
					}
				}
				move();

			} else {
				if (this.currentTimeStep != 0) {
					// Find the nearest Explorer to this Explorer
					GridCellNgh<Explorer> nghExplorers = new GridCellNgh<Explorer>(this.grid, this.currentLocation,
							Explorer.class, this.areaSideLength, this.areaSideLength);
					List<GridCell<Explorer>> explorerGridCells = nghExplorers.getNeighborhood(false);
					Explorer nearestExplorer = null;
					minDistance = Double.MAX_VALUE;
					double distance;
					for (GridCell<Explorer> cell : explorerGridCells) {
						if (cell.size() > 0) {
							distance = this.grid.getDistance(this.currentLocation, cell.getPoint());
							if (distance < minDistance && distance != 0.0) {
								//System.out.println(distance);
								for (Object obj : this.grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY())) {
									if (obj instanceof Explorer) {
										nearestExplorer = (Explorer) obj;
										minDistance = distance;
									}
								}
							}
						}

					}
					// If both Explorer agents are alone
					if (nearestExplorer != null && this.alone && nearestExplorer.alone) {
						// If both Explorer agents decide to not team up
						if (!teamUp(this, nearestExplorer) && !teamUp(nearestExplorer, this)) {
							move();
						} else {
							this.alone = false;
							this.teamMember = nearestExplorer;
							nearestExplorer.alone = false;
							nearestExplorer.teamMember = this;

							Network<Object> net = (Network<Object>) this.context.getProjection("team network");
							net.addEdge(this, this.teamMember);
						}
					} else {
						move();
					}
				} else {
					move();
				}
			}
			// Keep track of how much time has passed
			this.currentTimeStep++;
			// treasureValue decays over time
			this.treasureValue = this.treasureValue - (this.treasureValue * treasureDecayRate);
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
				this.finalUtility = this.treasureValue;
				this.finalTimeTick = this.currentTimeStep;
				// Remove the Explorer agents fromt the context
				if (!this.alone) {
					this.teamMember.finalUtility = this.treasureValue;
					this.teamMember.finalTimeTick = this.teamMember.currentTimeStep;
					this.context.remove(this.teamMember);
				}
				this.context.remove(this);
			}
		} else {
			// Move towards random unknown location
			// Set difference: unknownnGridPoints \ workingMemory, and unknownGridPoints \ permanentMemory in order to find all unseen grid points
			this.unknownGridPoints.removeAll(this.workingMemory);
			this.unknownGridPoints.removeAll(this.permanentMemory);
			List<List<Integer>> setToList = new ArrayList<List<Integer>>(this.unknownGridPoints);
			if (this.randomLocation == null) {
				int randomIndex = RandomHelper.nextIntFromTo(0, setToList.size() - 1);
				this.randomLocation = setToList.get(randomIndex);
			} else if (!this.unknownGridPoints.contains(this.randomLocation)) {
				int randomIndex = RandomHelper.nextIntFromTo(0, setToList.size() - 1);
				this.randomLocation = setToList.get(randomIndex);
			}
			int x = this.randomLocation.get(0);
			int y = this.randomLocation.get(1);
			int randomNum = RandomHelper.nextIntFromTo(0, 1);
			if (randomNum == 0) {
				if ((explorerX - x) < 0) {
					grid.moveByVector(this, 1, Direction.EAST);
				} else if ((explorerX - x) > 0) {
					grid.moveByVector(this, 1, Direction.WEST);
				}
			} else {
				if ((explorerY - y) < 0) {
					grid.moveByVector(this, 1, Direction.NORTH);
				} else if ((explorerY - y) > 0) {
					grid.moveByVector(this, 1, Direction.SOUTH);
				}
			}
		}
		// Update this.workingMemory
		Set<List<Integer>> newPerceptionRegion = getPerceptionRegion();
		this.workingMemory.removeAll(newPerceptionRegion);
		// Update this.permanentMemory to contain this.navigationMemory% of locations from this.workingMemory (Note: May need to make sure locations that are already in this.permanentMemory aren't being re-added
		int numLocationsToAdd = (int) Math.ceil((this.navigationMemory * this.workingMemory.size()));
		List<List<Integer>> setToList = new ArrayList<List<Integer>>(this.workingMemory);
		for (int i = 0; i < numLocationsToAdd; i++) {
			int randomIndex = RandomHelper.nextIntFromTo(0, this.workingMemory.size());
			this.permanentMemory.add(setToList.get(randomIndex));
		}
	}

	public Set<List<Integer>> getPerceptionRegion() {
		Set<List<Integer>> perceptionRegion = new HashSet<List<Integer>>();
		GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(this.grid, this.currentLocation,
				Treasure.class, this.perceptionRadius, this.perceptionRadius);
		List<GridCell<Treasure>> gridCells = nghCreator.getNeighborhood(false);
		for (GridCell<Treasure> gc : gridCells) {
			List<Integer> point = new ArrayList<Integer>(2);
			point.add(gc.getPoint().getX());
			point.add(gc.getPoint().getY());
			perceptionRegion.add(point);
		}
		// Share perception regions between team members
		if (this.alone == false) {
			GridCellNgh<Treasure> nghC = new GridCellNgh<Treasure>(this.teamMember.grid, this.teamMember.currentLocation,
					Treasure.class, this.teamMember.perceptionRadius, this.teamMember.perceptionRadius);
			List<GridCell<Treasure>> gridC = nghC.getNeighborhood(false);
			for (GridCell<Treasure> gc : gridC) {
				List<Integer> point = new ArrayList<Integer>(2);
				point.add(gc.getPoint().getX());
				point.add(gc.getPoint().getY());
				perceptionRegion.add(point);
			}
		}
		return perceptionRegion;
	}

	public boolean teamUp(Explorer thisExplorer, Explorer nearestExplorer) {		
		double aloneSpeed = (double) thisExplorer.getSearchedAreaSize() / thisExplorer.currentTimeStep;
		int teamSearchedArea = nearestExplorer.getSearchedAreaSize() + thisExplorer.getSearchedAreaSize();
		double teamSpeed = (double) teamSearchedArea / thisExplorer.currentTimeStep;
		//System.out.println(aloneSpeed + " " + teamSpeed);
		double timeToDiscoverTreasureAlone = (thisExplorer.unknownGridPoints.size() / aloneSpeed) / thisExplorer.treasureCount;
		double timeToDiscoverTreasureTeam = (thisExplorer.unknownGridPoints.size() / teamSpeed) / thisExplorer.treasureCount;
		//System.out.println(timeToDiscoverTreasureAlone + " vs " + timeToDiscoverTreasureTeam);
		double treasureValueAlone = this.treasureValue;
		double treasureValueTeam = this.treasureValue;
		for (int i = 0; i < timeToDiscoverTreasureAlone; i++) {
			treasureValueAlone = treasureValueAlone - (treasureValueAlone * thisExplorer.treasureDecayRate);
		}
		for (int i = 0; i < timeToDiscoverTreasureTeam; i++) {
			treasureValueTeam = treasureValueTeam - (treasureValueTeam * thisExplorer.treasureDecayRate);
		}
		treasureValueTeam = treasureValueTeam / 2;

		// If thisExplorer receives less when alone as compared to team, then want to team up
		if (this.flag) {
			System.out.println("2 " + treasureValueAlone + " vs " + treasureValueTeam);
			this.flag = false;
		}
		if (treasureValueAlone < treasureValueTeam) {
			//System.out.println("1 " + treasureValueAlone + " vs " + treasureValueTeam + " ID = " + this.count + " OTHER ID = " + nearestExplorer.count);
			this.flag = true;
			return true;
		} else {
			return false;
		}
	}

	public int getSearchedAreaSize() {
		return this.permanentMemory.size() + getPerceptionRegion().size();
	}
	
	public double getStartingDistanceFromFoundTreasure() {
		return this.grid.getDistance(this.startingLocation, this.closestTreasurePoint);
	}
	
	public int getFinalTimeTick() {
		return this.finalTimeTick;
	}
}
