package treasureHunters;

import java.util.ArrayList;
import java.util.List;

import jzombies.Human;
import jzombies.Zombie;
import treasureHunters.Treasure;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
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

	public Explorer(ContinuousSpace<Object> space, Grid<Object> grid, int navigationMemory, int perceptionRadius) {
		this.space = space;
		this.grid = grid;
		this.navigationMemory = navigationMemory;
		this.perceptionRadius = perceptionRadius;
		this.uncoveredTreasure = false;
		this.treasureFound = false;
		this.alone = true;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void exploring() { //step()
		// See if this Explorer has uncovered a treasure
		if (this.uncoveredTreasure == false) {
			// Get the current grid location of this Explorer
			GridPoint pt = grid.getLocation(this);

			// Use the GridCellNgh class to create GridCells in order to find any treasures within the perceptionRadius
			GridCellNgh<Treasure> nghCreator = new GridCellNgh<Treasure>(grid, pt,
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
				GridPoint closestTreasurePoint = null;
				minDistance = Double.MAX_VALUE;
				double distance = 0;
				for (GridCell<Treasure> cell : gridCells) {
					distance = grid.getDistance(pt, cell.getPoint());
					if (distance < minDistance) {
						closestTreasurePoint = cell.getPoint();
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
				GridCellNgh<Explorer> nghExplorers = new GridCellNgh<Explorer>(grid, pt,
						Explorer.class, this.areaSideLength, this.areaSideLength);
				List<GridCell<Explorer>> explorerGridCells = nghExplorers.getNeighborhood(true);
				//double minDistance;
				Explorer nearestExplorer = null;
				minDistance = Double.MAX_VALUE;
				double distance = 0;
				for (GridCell<Explorer> cell : explorerGridCells) {
					distance = grid.getDistance(pt, cell.getPoint());
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
