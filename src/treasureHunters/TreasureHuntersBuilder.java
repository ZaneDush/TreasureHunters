package treasureHunters;

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

	@Override
	public Context build(Context<Object> context) {
		context.setId("TreasureHunters");

//		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>(
//				"infection network", context, true);
//		netBuilder.buildNetwork();

//		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder
//				.createContinuousSpaceFactory(null);
//		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
//				"space", context, new RandomCartesianAdder<Object>(),
//				new repast.simphony.space.continuous.WrapAroundBorders(), 50,
//				50);

		Parameters params = RunEnvironment.getInstance().getParameters();
		int areaDimensions = (Integer) params.getValue("area_dimensions");
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new StrictBorders(),
						new SimpleGridAdder<Object>(), true, areaDimensions, areaDimensions)); // true so that many agents may occupy the same location at a time

		int explorerCount = (Integer) params.getValue("explorer_count");
		int navigationMemory = (Integer) params.getValue("navigation_memory");
		int perceptionRadius = (Integer) params.getValue("perception_radius");
		for (int i = 0; i < explorerCount; i++) {
			context.add(new Explorer(grid, navigationMemory, perceptionRadius));
		}

		int treasureCount = (Integer) params.getValue("treasure_count");
		int treasureValue = (Integer) params.getValue("treasure_value");
		for (int i = 0; i < treasureCount; i++) {
			context.add(new Treasure(grid, treasureValue));
		}

//		for (Object obj : context) {
//			NdPoint pt = space.getLocation(obj);
//			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
//		}
//		
//		if (RunEnvironment.getInstance().isBatch()) {
//			RunEnvironment.getInstance().endAt(20);
//		}

		return context;

	}

}
