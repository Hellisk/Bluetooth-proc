package util.io;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.object.RoadNetworkGraph;
import util.object.RoadNode;
import util.object.RoadWay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hellisk
 * @since 8/09/2019
 */
public class OSMMapLoader implements Sink {
	
	private static final Logger LOG = Logger.getLogger(OSMMapLoader.class);
	private final DistanceFunction distFunc = new GreatCircleDistanceFunction();
	private final String outputMapFolder;
	private List<RoadNode> nodeList = new ArrayList<>();
	private Map<String, RoadNode> id2RoadNode = new HashMap<>();
	private List<Way> tempWayList = new ArrayList<>();
	
	public OSMMapLoader(String outputMapFolder) {
		this.outputMapFolder = outputMapFolder;
	}
	
	@Override
	public void initialize(Map<String, Object> arg0) {
	}
	
	@Override
	public void process(EntityContainer entityContainer) {
		if (entityContainer instanceof NodeContainer) {
			Node osmNode = ((NodeContainer) entityContainer).getEntity();
			RoadNode currNode = new RoadNode(osmNode.getId() + "", osmNode.getLongitude(), osmNode.getLatitude(), distFunc);
			nodeList.add(currNode);
			id2RoadNode.put(currNode.getID(), currNode);
		} else if (entityContainer instanceof WayContainer) {
			Way osmWay = ((WayContainer) entityContainer).getEntity();
			tempWayList.add(osmWay);
		}
	}
	
	@Override
	public void complete() {
		List<RoadWay> wayList = new ArrayList<>();
		LOG.info("Initial map read finish, total number of road nodes: " + nodeList.size() + ". Start converting road ways.");
		for (Way osmWay : tempWayList) {
			List<RoadNode> miniNodeList = new ArrayList<>();
			List<WayNode> wayNodes = osmWay.getWayNodes();
			if (wayNodes.size() < 2) {
				LOG.error("The current way " + osmWay.getId() + " only contains " + wayNodes.size() + " points.");
				continue;
			}
			String startNodeID = wayNodes.get(0).getNodeId() + "";
			String endNodeID = wayNodes.get(wayNodes.size() - 1).getNodeId() + "";
			if (!id2RoadNode.containsKey(startNodeID) || !id2RoadNode.containsKey(endNodeID)) {
				LOG.error("The endpoint of way" + osmWay.getId() + " is not found: " + startNodeID + "," + endNodeID + ".");
				continue;
			}
			miniNodeList.add(id2RoadNode.get(wayNodes.get(0).getNodeId() + ""));
			for (int i = 1; i < wayNodes.size() - 1; i++) {
				WayNode currWayNode = wayNodes.get(i);
				RoadNode currNode = new RoadNode(currWayNode.getNodeId() + "", currWayNode.getLongitude(), currWayNode.getLatitude(), distFunc);
				miniNodeList.add(currNode);
			}
			miniNodeList.add(id2RoadNode.get(wayNodes.get(wayNodes.size() - 1).getNodeId() + ""));
			RoadWay currWay = new RoadWay(osmWay.getId() + "", miniNodeList, distFunc);
			wayList.add(currWay);
		}
		RoadNetworkGraph finalGraph = new RoadNetworkGraph(distFunc);
		finalGraph.setNodes(nodeList);
		finalGraph.addWays(wayList);
		finalGraph.isolatedNodeRemoval();
		try {
			MapWriter.writeMap(finalGraph, outputMapFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.info("Load initial map finish, total number of ways: " + wayList.size() + ", number of nodes after isolation removal: " +
				finalGraph.getNodes().size() + ". Boundary is : " + finalGraph.getMinLon() + "," + finalGraph.getMaxLon() + "," +
				finalGraph.getMinLat() + "," + finalGraph.getMaxLat() + ".");
		
	}
	
	@Override
	public void close() {
	}
}