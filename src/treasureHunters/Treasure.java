package treasureHunters;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.grid.Grid;

public class Treasure {

	private Grid<Object> grid;
	private double treasureValue;
	private double treasureDecayRate;
	
	/**
	 * Constructor for the Treasure items. Treated as an agent.
	 * @param grid
	 * @param treasureValue
	 * @param treasureDecayRate
	 */
	public Treasure(Grid<Object> grid, double treasureValue, double treasureDecayRate) {
		this.grid = grid;
		this.treasureValue = treasureValue;
		this.treasureDecayRate = treasureDecayRate;
	}
	
	/**
	 * Main algorithm that is responsible for setting the treasureValue after it decays at every time step.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void decayValue() {
		this.treasureValue = this.treasureValue - (this.treasureValue * treasureDecayRate);
	}

}
