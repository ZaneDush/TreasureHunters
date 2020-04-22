package treasureHunters;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
import repast.simphony.space.grid.WrapAroundBorders;

public class TreasureHuntersBuilder implements ContextBuilder<
Object> {

	/**
	 * Method responsible for building the TreasureHunters simulation context.
	 */
	@Override
	public Context build(Context<Object> context) {
		context.setId("TreasureHunters");
		// Create the projection network to show which Explorers have teamed up visually
		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
				"team network", context, false);
		netBuilder.buildNetwork();
		// Retrieve the parameters that are set in the GUI
		Parameters params = RunEnvironment.getInstance().getParameters();
		int areaDimensions = (Integer) params.getValue("area_dimensions");
		int explorerCount = (Integer) params.getValue("explorer_count");
		double navigationMemory = (Double) params.getValue("navigation_memory");
		int perceptionRadius = (Integer) params.getValue("perception_radius");
		int treasureCount = (Integer) params.getValue("treasure_count");
		double treasureValue = (Double) params.getValue("treasure_value");
		double treasureDecayRate = (Double) params.getValue("treasure_decay_rate");
		// Create the grid projection on which the agents operate
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new StrictBorders(),
						new SimpleGridAdder<Object>(), true, areaDimensions, areaDimensions)); // true so that many agents may occupy the same location at a time
		// Create Explorers, assigning a unique "count" to each that serves as an ID (useful for debugging)
		int count = 0;
		for (int i = 0; i < explorerCount; i++) {
			context.add(new Explorer(grid, navigationMemory, perceptionRadius, treasureCount, treasureValue, treasureDecayRate, count));
			count++;
		}
		// Create treasures
		for (int i = 0; i < treasureCount; i++) {
			context.add(new Treasure(grid, treasureValue, treasureDecayRate));
		}
		// Place all Explorers and treasures on the grid at a random location
		for (Object obj : context) {
			int x = RandomHelper.nextIntFromTo(0, areaDimensions - 1);
			int y = RandomHelper.nextIntFromTo(0, areaDimensions - 1);
			grid.moveTo(obj, x, y);
		}
		return context;
	}
}
