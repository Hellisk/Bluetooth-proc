package util.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
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
	
	private static final Logger LOG = LogManager.getLogger(OSMMapLoader.class);
	private final DistanceFunction distFunc = new GreatCircleDistanceFunction();
	private final String outputMapFolder;
	private final Map<String, RoadNode> id2RoadNode = new HashMap<>();
	private final List<Way> tempOSMWayList = new ArrayList<>();
	Map<String, Set<String>> nodeTagMapping = new HashMap<>();
	Map<String, Set<String>> wayTagMapping = new HashMap<>();
	Set<String> validRoadTagSet = new HashSet<>();
	
	public OSMMapLoader(String outputMapFolder) {
		this.outputMapFolder = outputMapFolder;
	}
	
	@Override
	public void initialize(Map<String, Object> arg0) {
		// set all valid road types for road filtering
		validRoadTagSet.add("motorway");
		validRoadTagSet.add("trunk");
		validRoadTagSet.add("primary");
		validRoadTagSet.add("secondary");
		validRoadTagSet.add("tertiary");
		validRoadTagSet.add("unclassified");
		validRoadTagSet.add("service");
		validRoadTagSet.add("motorway_link");
		validRoadTagSet.add("trunk_link");
		validRoadTagSet.add("primary_link");
		validRoadTagSet.add("secondary_link");
		validRoadTagSet.add("tertiary_link");
		validRoadTagSet.add("living_street");
		validRoadTagSet.add("road");
	}
	
	@Override
	public void process(EntityContainer entityContainer) {
		if (entityContainer instanceof NodeContainer) {
			Node osmNode = ((NodeContainer) entityContainer).getEntity();
			RoadNode currNode = new RoadNode(osmNode.getId() + "", osmNode.getLongitude(), osmNode.getLatitude(), distFunc);
			for (Tag tag : osmNode.getTags()) {
				if (!nodeTagMapping.containsKey(tag.getKey())) {
					Set<String> valueSet = new HashSet<>();
					valueSet.add(tag.getValue());
					nodeTagMapping.put(tag.getKey(), valueSet);
				} else {
					nodeTagMapping.get(tag.getKey()).add(tag.getValue());
				}
				currNode.addTag(tag.getKey(), tag.getValue());
			}
			id2RoadNode.put(currNode.getId(), currNode);
		} else if (entityContainer instanceof WayContainer) {
			Way osmWay = ((WayContainer) entityContainer).getEntity();
			boolean isRoadWay = false;
			for (Tag tag : osmWay.getTags()) {
				if (!wayTagMapping.containsKey(tag.getKey())) {
					Set<String> valueSet = new HashSet<>();
					valueSet.add(tag.getValue());
					wayTagMapping.put(tag.getKey(), valueSet);
				} else {
					wayTagMapping.get(tag.getKey()).add(tag.getValue());
				}
				if (tag.getKey().equals("highway") && validRoadTagSet.contains(tag.getValue())) {
					isRoadWay = true;
				}
			}
			if (isRoadWay)
				tempOSMWayList.add(osmWay);
		}
	}
	
	@Override
	public void complete() {
		validRoadTagSet.add("residential");
		Set<String> nodeIdSet = new LinkedHashSet<>();    // set of intersections
		List<RoadWay> tempWayList = new ArrayList<>();    // list of roads that require further segmentation if it has intersections along
		// the road
		List<RoadNode> nodeList = new ArrayList<>();
		List<RoadWay> wayList = new ArrayList<>();
		int cyclicRoadCount = 0;
		LOG.info("Initial map read finish, start converting road ways.");
		
		// register all intersections first
		for (Way osmWay : tempOSMWayList) {
			List<RoadNode> miniNodeList = new ArrayList<>();
			List<WayNode> wayNodes = osmWay.getWayNodes();
			if (wayNodes.size() < 2) {
				LOG.error("The current way " + osmWay.getId() + " only contains " + wayNodes.size() + " points.");
				continue;
			}
			// since a road may not entirely included in the map, find the sub-road if needed
			int startIndex = 0;
			int endIndex = wayNodes.size() - 1;
			String startNodeId = wayNodes.get(startIndex).getNodeId() + "";
			String endNodeId = wayNodes.get(endIndex).getNodeId() + "";
			while (!id2RoadNode.containsKey(startNodeId) && startIndex < endIndex) {
				startIndex++;
				startNodeId = wayNodes.get(startIndex).getNodeId() + "";
			}
			if (startIndex == endIndex) {
				LOG.warn("Way " + osmWay.getId() + " is not found in the map: forward.");
				continue;
			}
			while (!id2RoadNode.containsKey(endNodeId) && endIndex > startIndex) {
				endIndex--;
				endNodeId = wayNodes.get(endIndex).getNodeId() + "";
			}
			if (startIndex == endIndex) {
				LOG.warn("Way " + osmWay.getId() + " is not found in the map: backward.");
				continue;
			}
			RoadNode currStartNode = id2RoadNode.get(wayNodes.get(startIndex).getNodeId() + "");
			miniNodeList.add(currStartNode);
			boolean isComplete = true;
			// check if a middle part of the road is out of the map range, ignore such road if appears
			for (int i = startIndex + 1; i < endIndex; i++) {
				WayNode currWayNode = wayNodes.get(i);
				String currId = currWayNode.getNodeId() + "";
				if (!id2RoadNode.containsKey(currId)) {
					LOG.warn("Intermediate node " + currId + " from way " + osmWay.getId() + " is not found in node list. Ignore the " +
							"current road.");
					isComplete = false;
					break;
				}
				RoadNode currNode = id2RoadNode.get(currId);
				miniNodeList.add(currNode);
			}
			if (!isComplete)
				continue;
			RoadNode currEndNode = id2RoadNode.get(wayNodes.get(endIndex).getNodeId() + "");
			
			// the current road is confirmed to be added
			nodeIdSet.add(currStartNode.getId());
			nodeIdSet.add(currEndNode.getId());
			miniNodeList.add(currEndNode);
			RoadWay currWay = new RoadWay(osmWay.getId() + "", miniNodeList, distFunc);
			for (Tag tag : osmWay.getTags()) {
				currWay.addTag(tag.getKey(), tag.getValue());
			}
			tempWayList.add(currWay);
		}
		
		int totalSplitCount = 0;
		// split the road ways whose intermediate point is an intersection
		for (RoadWay currWay : tempWayList) {
			List<RoadNode> currNodeList = new ArrayList<>();
			List<RoadWay> roadWays = new ArrayList<>();
			currNodeList.add(currWay.getFromNode());
			int subRoadCount = 0;
			// check intermediate nodes
			for (int i = 1; i < currWay.getNodes().size() - 1; i++) {
				RoadNode roadNode = currWay.getNode(i);
				currNodeList.add(roadNode);
				if (nodeIdSet.contains(roadNode.getId())) {    // split the current road
					RoadWay splitWay = new RoadWay(currWay.getId() + "_" + subRoadCount, currNodeList, distFunc);
					for (Map.Entry<String, Object> entry : currWay.getTags().entrySet()) {
						splitWay.addTag(entry.getKey(), entry.getValue());
					}
					roadWays.add(splitWay);
					currNodeList = new ArrayList<>();
					currNodeList.add(roadNode);
					totalSplitCount++;
					subRoadCount++;
				}
			}
			currNodeList.add(currWay.getToNode());
			if (subRoadCount == 0) {    // no road split
				roadWays.add(currWay);
			} else {
				RoadWay lastWay = new RoadWay(currWay.getId() + "_" + subRoadCount, currNodeList, distFunc);
				for (Map.Entry<String, Object> entry : currWay.getTags().entrySet()) {
					lastWay.addTag(entry.getKey(), entry.getValue());
				}
				roadWays.add(lastWay);
			}
			// direction check
			if (currWay.getTags().containsKey("oneway")) {
				if (currWay.getTags().get("oneway").equals("-1")) {    // reverse road, change the direction
					for (RoadWay roadWay : roadWays) {
						List<RoadNode> reverseList = new ArrayList<>();
						for (int i = roadWay.getNodes().size() - 1; i >= 0; i--) {
							reverseList.add(roadWay.getNode(i));
						}
						roadWay.setNodes(reverseList);
					}
				} else if (currWay.getTags().get("oneway").equals("no")) {    // double direction, add reverse roads
					List<RoadWay> reverseWays = new ArrayList<>();
					for (RoadWay roadWay : roadWays) {
						List<RoadNode> reverseList = new ArrayList<>();
						reverseList.add(roadWay.getToNode());
						for (int i = roadWay.getNodes().size() - 2; i > 0; i--) {
							RoadNode reverseNode = roadWay.getNode(i).clone();
							reverseNode.setId(roadWay.getNode(i).getId() + "-");
							reverseList.add(reverseNode);
						}
						reverseList.add(roadWay.getFromNode());
						RoadWay reverseWay = new RoadWay("-" + roadWay.getId(), reverseList, distFunc);
						reverseWays.add(reverseWay);
					}
					roadWays.addAll(reverseWays);
				}
			}
			wayList.addAll(roadWays);
		}
		
		for (RoadWay currWay : wayList) {
			if (currWay.getFromNode().toPoint().equals2D(currWay.getToNode().toPoint())) {   // the current road has the same start and
				// end point.
				cyclicRoadCount++;
			}
			for (RoadNode node : currWay.getNodes()) {
				node.clearTag();
			}
			currWay.clearTag();
		}
		for (String nodeId : nodeIdSet) {
			RoadNode currNode = id2RoadNode.get(nodeId);
			currNode.clearTag();
			nodeList.add(currNode);
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
		LOG.info("Total number of cyclic roads: " + cyclicRoadCount + ", number of splits performed: " + totalSplitCount + ".");
	}
	
	@Override
	public void close() {
	}
}