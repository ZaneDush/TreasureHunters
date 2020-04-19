package treasureHunters;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.grid.Grid;

public class Treasure {

	private Grid<Object> grid;
	private int treasureValue;
	private int treasureDecayRate;
	
	public Treasure(Grid<Object> grid, int treasureValue, int treasureDecayRate) {
		this.grid = grid;
		this.treasureValue = treasureValue;
		this.treasureDecayRate = treasureDecayRate;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void decayValue() {
		this.treasureValue = this.treasureValue - (this.treasureValue * (treasureDecayRate / 100));
	}

}
