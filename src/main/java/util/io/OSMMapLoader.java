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
import java.util.*;

/**
 * @author Hellisk
 * @since 8/09/2019
 */
public class OSMMapLoader implements Sink {
	
	private static final Logger LOG = Logger.getLogger(OSMMapLoader.class);
	private final DistanceFunction distFunc = new GreatCircleDistanceFunction();
	private final String outputMapFolder;
	private List<RoadNode> tempNodeList = new ArrayList<>();
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
			tempNodeList.add(currNode);
			id2RoadNode.put(currNode.getID(), currNode);
		} else if (entityContainer instanceof WayContainer) {
			Way osmWay = ((WayContainer) entityContainer).getEntity();
			tempWayList.add(osmWay);
		}
	}
	
	@Override
	public void complete() {
		Set<String> nodeIDSet = new HashSet<>();
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		LOG.info("Initial map read finish, total number of road nodes: " + nodeList.size() + ". Start converting road ways.");
		for (Way osmWay : tempWayList) {
			List<RoadNode> miniNodeList = new ArrayList<>();
			List<WayNode> wayNodes = osmWay.getWayNodes();
			if (wayNodes.size() < 2) {
				LOG.error("The current way " + osmWay.getId() + " only contains " + wayNodes.size() + " points.");
				continue;
			}
			// since the road may not entirely included in the map, find the sub-road if needed
			int startIndex = 0;
			int endIndex = wayNodes.size() - 1;
			String startNodeID = wayNodes.get(startIndex).getNodeId() + "";
			String endNodeID = wayNodes.get(endIndex).getNodeId() + "";
			while (!id2RoadNode.containsKey(startNodeID) && startIndex < endIndex) {
				startIndex++;
				startNodeID = wayNodes.get(startIndex).getNodeId() + "";
			}
			if (startIndex == endIndex) {
				LOG.warn("Way " + osmWay.getId() + " is not found in the map: forward.");
				continue;
			}
			while (!id2RoadNode.containsKey(endNodeID) && endIndex >= startIndex) {
				endIndex--;
				endNodeID = wayNodes.get(endIndex).getNodeId() + "";
			}
			if (startIndex == endIndex) {
				LOG.warn("Way " + osmWay.getId() + " is not found in the map: backward.");
				continue;
			}
			RoadNode currStartNode = id2RoadNode.get(wayNodes.get(startIndex).getNodeId() + "");
			miniNodeList.add(currStartNode);
			boolean isComplete = true;
			for (int i = startIndex + 1; i < endIndex; i++) {
				WayNode currWayNode = wayNodes.get(i);
				String currID = currWayNode.getNodeId() + "";
				if (!id2RoadNode.containsKey(currID)) {
					LOG.warn("Intermediate node " + currID + " from way " + osmWay.getId() + " is not found in node list. Ignore the " +
							"current road.");
					isComplete = false;
					break;
				}
				RoadNode currNode = id2RoadNode.get(currID);
				miniNodeList.add(currNode);
			}
			if (!isComplete)
				continue;
			RoadNode currEndNode = id2RoadNode.get(wayNodes.get(endIndex).getNodeId() + "");
			if (currStartNode.toPoint().equals2D(currEndNode.toPoint()))    // the current road has the same start and end point.
				continue;
			// the current road is confirmed to be added
			if (!nodeIDSet.contains(currStartNode.getID())) {
				nodeList.add(currStartNode);
				nodeIDSet.add(currStartNode.getID());
			}
			if (!nodeIDSet.contains(currEndNode.getID())) {
				nodeList.add(currEndNode);
				nodeIDSet.add(currEndNode.getID());
			}
			miniNodeList.add(currEndNode);
			RoadWay currWay = new RoadWay(osmWay.getId() + "", miniNodeList, distFunc);
			wayList.add(currWay);
		}
		RoadNetworkGraph finalGraph = new RoadNetworkGraph(distFunc);
		finalGraph.setNodes(nodeList);
		finalGraph.addWays(wayList);
		finalGraph.isolatedNodeRemoval();
		try {
			MapWriter.writeMap(finalGraph, outputMapFolder + "Brisbane.txt");
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