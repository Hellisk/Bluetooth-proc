package util.object;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.function.DistanceFunction;

import java.io.Serializable;
import java.util.*;

/**
 * A Road Network Graph object, based on OpenStreetMap (OSM) data model.
 *
 * @author uqdalves, Hellisk
 */
public class RoadNetworkGraph implements Serializable {
	
	private static final Logger LOG = LogManager.getLogger(RoadNetworkGraph.class);
	private DistanceFunction distFunc;
	/**
	 * OSM primitives
	 */
	private List<RoadNode> nodeList = new ArrayList<>();
	private Map<String, RoadNode> id2NodeMap = new HashMap<>();
	private List<RoadWay> wayList = new ArrayList<>();
	private Map<String, RoadWay> id2WayMap = new HashMap<>();
	/**
	 * Map boundaries
	 */
	private boolean hasBoundary = false;
	private double minLat = Double.POSITIVE_INFINITY, minLon = Double.POSITIVE_INFINITY;
	private double maxLat = Double.NEGATIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
	
	private boolean isDirectedMap = true;    // false when it stores an undirected map, all roads has its reverse road
	
	private boolean isCompactMap = false;    // A compact map does not have intersection whose degree is 2 (intermediate point in a road).
	// Otherwise it is a loose map whose road ways are all straight line (no intermediate point on the road).
	
	public RoadNetworkGraph(DistanceFunction df) {
		this.distFunc = df;
	}
	
	/**
	 * @return The list of nodes in this road network graph.
	 */
	public List<RoadNode> getNodes() {
		return nodeList;
	}
	
	/**
	 * Reset the map by firstly setting the road node list.
	 *
	 * @param newNodeList The road node list representing the intersections or road ends.
	 */
	public void setNodes(List<RoadNode> newNodeList) {
		if (!this.wayList.isEmpty() || !this.id2WayMap.isEmpty())
			throw new IllegalCallerException("The setNodes() should not be called when there were road ways in the map.");
		this.clear();
		this.addNodes(newNodeList);
	}
	
	public RoadNode getNode(int index) {
		return nodeList.get(index);
	}
	
	/**
	 * Reset the road network graph.
	 */
	private void clear() {
		this.nodeList.clear();
		this.id2NodeMap.clear();
		this.wayList.clear();
		this.id2WayMap.clear();
		this.hasBoundary = false;
		this.minLat = Double.POSITIVE_INFINITY;
		this.minLon = Double.POSITIVE_INFINITY;
		this.maxLat = Double.NEGATIVE_INFINITY;
		this.maxLon = Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Adds the given node to this road network graph.
	 *
	 * @param node The road node to add.
	 */
	public void addNode(RoadNode node) {
		if (node != null) {
			if (!id2NodeMap.containsKey(node.getId()) || node.getId().equals("")) {
				node.clearConnectedWays();
				nodeList.add(node);
				id2NodeMap.put(node.getId(), node);
				updateBoundary(node);
			} else
				LOG.error("Insert node to network failed. Node already exist: " + node.getId());
		}
	}
	
	private void removeNode(RoadNode node) {
		if (!this.id2NodeMap.containsKey(node.getId()))
			throw new IllegalArgumentException("The node " + node.getId() + " is not an intersection in the map.");
		if (node.getDegree() != 0)
			throw new IllegalArgumentException("The node to be removed is connected by some edges.");
		if (!this.nodeList.remove(node))
			throw new IllegalArgumentException("The node " + node.getId() + " is in the dictionary but not in the item list.");
		this.id2NodeMap.remove(node.getId());
	}
	
	/**
	 * Add all the nodes in the list to this road network graph.
	 *
	 * @param nodes The list of road nodes to add.
	 */
	public void addNodes(List<RoadNode> nodes) {
		if (nodes == null) {
			throw new NullPointerException("List of road nodes to add must not be null.");
		}
		for (RoadNode node : nodes) {
			if (!id2NodeMap.containsKey(node.getId())) {
				node.clearConnectedWays();
				nodeList.add(node);
				id2NodeMap.put(node.getId(), node);
			} else LOG.error("Insert node to network failed. Node already exist: " + node.getId());
		}
		updateBoundary();
	}
	
	/**
	 * @return The list of Ways in this road network graph.
	 */
	public List<RoadWay> getWays() {
		return wayList;
	}
	
	public RoadWay getWay(int index) {
		return wayList.get(index);
	}
	
	/**
	 * Check if the boundary is preset.
	 *
	 * @return True if the boundary is preset
	 */
	public boolean hasBoundary() {
		return hasBoundary;
	}
	
	/**
	 * Check if the current map is directed. The default value is true.
	 *
	 * @return True if it is directed.
	 */
	public boolean isDirectedMap() {
		return this.isDirectedMap;
	}
	
	private void setDirectedMap(boolean directedMap) {
		isDirectedMap = directedMap;
	}
	
	/**
	 * Get all nodes in the map, including intersections and mini nodes.
	 *
	 * @return both intersections and mini nodes
	 */
	public List<RoadNode> getAllTypeOfNodes() {
		if (!isCompactMap)
			return this.nodeList;    // no intermediate point in a loose map, return the node list directly.
		List<RoadNode> pointList = new ArrayList<>(this.getNodes());
		for (RoadWay w : this.getWays()) {
			for (RoadNode n : w.getNodes())
				if (!this.id2NodeMap.containsKey(n.getId()))
					pointList.add(n);
		}
		return pointList;
	}
	
	/**
	 * Adds all the ways in the list to this road network graph.
	 *
	 * @param waysList The list of road ways to add.
	 */
	public void addWays(List<RoadWay> waysList) {
		if (waysList == null) {
			throw new NullPointerException("List of road ways to add must not be null.");
		}
		for (RoadWay way : waysList)
			addWay(way);
	}
	
	/**
	 * Set bounding box of the road network.
	 *
	 * @param minLon minimum longitude
	 * @param maxLon maximum longitude
	 * @param minLat minimum latitude
	 * @param maxLat maximum latitude
	 */
	public void setBoundary(double minLon, double maxLon, double minLat, double maxLat) {
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * Add the given Way to this road network graph. Make sure the endpoints of the road way should exist in the current node list
	 * unless it is a temp road.
	 *
	 * @param way The road way to add.
	 */
	public void addWay(RoadWay way) {
		if (!isCompactMap && way.getNodes().size() != 2) {
			LOG.info("A polyline road added to the current map, set as a compact map.");
			isCompactMap = true;
		}
		if (way != null && way.getNodes().size() > 1) {
			if (!id2WayMap.containsKey(way.getId())) {
				if (!id2NodeMap.containsKey(way.getFromNode().getId()) || !id2NodeMap.containsKey(way.getToNode().getId()))
					throw new IllegalArgumentException("The endpoints of the inserted road way do not exist in the current map: "
							+ way.getFromNode().getId() + "," + way.getToNode().getId());
				wayList.add(way);
				id2WayMap.put(way.getId(), way);
				way.getFromNode().addOutGoingWay(way);
				way.getToNode().addInComingWay(way);
				if (!isDirectedMap) {    // for undirected map, the road should be both incoming and outgoing adjacent road.
					way.getFromNode().addInComingWay(way);
					way.getToNode().addOutGoingWay(way);
				}
				for (RoadNode n : way.getNodes())
					updateBoundary(n);
			} else
				throw new IllegalArgumentException("Road way already exist: " + way.getId());
		}
	}
	
	/**
	 * Reset the boundary to better represent the size.
	 */
	public void updateBoundary() {
		this.setMaxLon(Double.NEGATIVE_INFINITY);
		this.setMaxLat(Double.NEGATIVE_INFINITY);
		this.setMinLon(Double.POSITIVE_INFINITY);
		this.setMinLat(Double.POSITIVE_INFINITY);
		this.hasBoundary = false;
		for (RoadNode n : this.getAllTypeOfNodes())
			updateBoundary(n);
	}
	
	private void updateBoundary(RoadNode node) {
		// update the map boarder
		this.hasBoundary = true;
		if (this.maxLon < node.lon()) {
			this.maxLon = node.lon();
		}
		if (this.minLon > node.lon()) {
			this.minLon = node.lon();
		}
		if (this.maxLat < node.lat()) {
			this.maxLat = node.lat();
		}
		if (this.minLat > node.lat()) {
			this.minLat = node.lat();
		}
	}
	
	/**
	 * @return The minimum latitude value of this road map's boundary.
	 */
	public double getMinLat() {
		return minLat;
	}
	
	/**
	 * Set the minimum latitude value of this road map's boundary.
	 */
	public void setMinLat(double minLat) {
		this.minLat = minLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The minimum longitude value of this road map's boundary.
	 */
	public double getMinLon() {
		return minLon;
	}
	
	/**
	 * Set the minimum longitude value of this road map's boundary.
	 */
	public void setMinLon(double minLon) {
		this.minLon = minLon;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum latitude value of this road map's boundary.
	 */
	public double getMaxLat() {
		return maxLat;
	}
	
	/**
	 * Set he maximum latitude value of this road map's boundary.
	 */
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum longitude value of this road map's boundary.
	 */
	public double getMaxLon() {
		return maxLon;
	}
	
	/**
	 * Set the maximum longitude value of this road map's boundary.
	 */
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
		this.hasBoundary = true;
	}
	
	public Rectangle getBoundary() {
		if (hasBoundary)
			return new Rectangle(minLon, minLat, maxLon, maxLat, distFunc);
		else {
			LOG.warn("The current map does not have boundary.");
			return new Rectangle(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, distFunc);
		}
	}
	
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	public void setDistanceFunction(DistanceFunction distFunc) {
		this.distFunc = distFunc;
	}
	
	/**
	 * Check whether this road network graph is empty.
	 *
	 * @return Returns true if this road network graph has no nodes.
	 */
	public boolean isEmpty() {
		return nodeList == null || nodeList.isEmpty();
	}
	
	public void removeRoadWayList(Collection<RoadWay> roadWayList) {
		List<RoadWay> removedWayList = new ArrayList<>();
		for (RoadWay way : roadWayList) {
			if (id2WayMap.containsKey(way.getId())) {
				id2WayMap.remove(way.getId());
				way.getFromNode().removeInComingWayFromList(way);
				way.getToNode().removeOutGoingWayFromList(way);
				if (!isDirectedMap()) {
					way.getFromNode().removeOutGoingWayFromList(way);
					way.getToNode().removeInComingWayFromList(way);
				}
			} else
				LOG.error("The road to be removed is not in the map: " + way.getId());
			removedWayList.add(way);
		}
		this.wayList.removeAll(removedWayList);
	}
	
	public int isolatedNodeRemoval() {
		int nodeSize = this.nodeList.size();
		for (Iterator<RoadNode> iterator = this.nodeList.iterator(); iterator.hasNext(); ) {
			RoadNode n = iterator.next();
			if (n.getDegree() == 0) {
				LOG.debug("Removed node ID: " + n.getId());
				iterator.remove();
				this.id2NodeMap.remove(n.getId());
			}
		}
		return nodeSize - this.nodeList.size();
	}
	
	public boolean containsWay(String id) {
		return this.id2WayMap.containsKey(id);
	}
	
	public RoadWay getWayByID(String id) {
		if (!containsWay(id))
			throw new IllegalArgumentException("The requested road way ID " + id + " is not in the map.");
		return id2WayMap.get(id);
	}
	
	public boolean containsNode(String id) {
		return this.id2NodeMap.containsKey(id);
	}
	
	public RoadNode getNodeByID(String id) {
		if (!containsNode(id))
			throw new IllegalArgumentException("The requested road node ID " + id + " is not in the map.");
		return id2NodeMap.get(id);
	}
	
	@Override
	public RoadNetworkGraph clone() {
		RoadNetworkGraph clone = new RoadNetworkGraph(distFunc);
		Map<String, RoadNode> id2NodeMapping = new HashMap<>();
		for (RoadNode n : this.getNodes()) {
			RoadNode cloneNode = n.clone();
			cloneNode.clearConnectedWays();
			clone.addNode(cloneNode);
			id2NodeMapping.put(cloneNode.getId(), cloneNode);
		}
		for (RoadWay w : this.getWays()) {
			RoadWay cloneWay = new RoadWay(w.getId(), w.getDistanceFunction());
			if (!id2NodeMapping.containsKey(w.getNode(0).getId()) || !id2NodeMapping.containsKey(w.getNode(w.size() - 1).getId()))
				throw new IllegalArgumentException("The road way to be cloned " + w.getId() + " is not originally linked to the " +
						"intersections.");
			cloneWay.addNode(id2NodeMapping.get(w.getNode(0).getId()));
			for (int i = 1; i < w.getNodes().size() - 1; i++) {
				cloneWay.addNode(w.getNode(i).clone());
			}
			cloneWay.addNode(id2NodeMapping.get(w.getNode(w.size() - 1).getId()));
			clone.addWay(cloneWay);
		}
		clone.updateBoundary();
		if (clone.getMaxLon() != this.getMaxLon() || clone.getMinLon() != this.getMinLon() || clone.getMaxLat() != this.getMaxLat()
				|| clone.getMinLat() != this.getMinLat())
			LOG.warn("Clone result has different boundary as the original object.");
		return clone;
	}
	
	/**
	 * Convert a directed map to an undirected map.
	 *
	 * @return The undirected map.
	 */
	public RoadNetworkGraph toUndirectedMap() {
		RoadNetworkGraph tempMap = this.clone();
		int maxVisitCount = 0;
		List<RoadWay> reverseWayList = new ArrayList<>();
		List<RoadWay> remainingWayList = new ArrayList<>();
		Set<String> wayEndPointPositionSet = new HashSet<>();
		for (RoadWay w : tempMap.getWays()) {
			if (!w.getId().contains("-")) {    // reverse
				String endPointPosition =
						w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
				String reverseEndPointPosition =
						w.getToNode().lon() + "_" + w.getToNode().lat() + "," + w.getFromNode().lon() + "_" + w.getFromNode().lat();
				if (wayEndPointPositionSet.contains(endPointPosition) || wayEndPointPositionSet.contains(reverseEndPointPosition)) {
					LOG.error("Multiple roads have the same endpoints: " + endPointPosition);
				} else {
					wayEndPointPositionSet.add(endPointPosition);
					maxVisitCount = Math.max(maxVisitCount, w.getVisitCount());
					remainingWayList.add(w);
				}
			} else
				reverseWayList.add(w);
		}
		
		// check if the removed roads have unique connection. Theoretically, they should all have reverse road included in the new map
		for (RoadWay w : reverseWayList) {
			String endPointPosition =
					w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
			String reverseEndPointPosition =
					w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
			if (!wayEndPointPositionSet.contains(endPointPosition) && !wayEndPointPositionSet.contains(reverseEndPointPosition)) {
				LOG.error("Reverse road of " + w.getId() + " does not appear in the map.");
				if (tempMap.id2WayMap.containsKey(w.getId().substring(1))) {
					LOG.error("More interestingly, " + w.getId() + " has reverse road but is not included in the new map.");
				} else {
					w.setId(w.getId().substring(1));
					remainingWayList.add(w);
				}
			}
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(this.distFunc);
		resultMap.setDirectedMap(false);
		List<RoadNode> nodeList = new ArrayList<>();
		for (RoadNode node : tempMap.getNodes()) {
			node.clearConnectedWays();
			nodeList.add(node);
		}
		resultMap.setNodes(nodeList);
		resultMap.addWays(remainingWayList);
		resultMap.updateBoundary();
		return resultMap;
	}
	
	public boolean isPlanarMap() {
		return nonPlanarNodeCount() == 0;
	}
	
	/**
	 * Calculate the total number of crosses happens for roads that do not have intersection. =0 means the map is planar.
	 *
	 * @return Count of potential intersections
	 */
	public int nonPlanarNodeCount() {
		RoadNetworkGraph currMap = this.clone();
		int count = 0;
		for (int i = 0; i < currMap.getWays().size(); i++) {
			RoadWay firstWay = currMap.getWay(i);
			for (int j = i + 1; j < currMap.getWays().size(); j++) {
				RoadWay secondWay = currMap.getWay(j);
				if (secondWay.getFromNode().toPoint().equals2D(firstWay.getToNode().toPoint())
						|| secondWay.getFromNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getToNode().toPoint())) {
					continue;
				}
				for (Segment firstEdge : firstWay.getEdges()) {
					for (Segment secondEdge : secondWay.getEdges()) {
						if (firstEdge.crosses(secondEdge.x1(), secondEdge.y1(), secondEdge.x2(), secondEdge.y2()))
							count++;
					}
				}
			}
		}
		return count;
	}
	
	
	/**
	 * Convert a map to its compact form. A compact map does not have intersections whose degree is two. The roads whose degree is two are
	 * merged to one new road, the new road ID is the concatenation of previous roads with "," as separator, i.e.: id1,id2,id3
	 *
	 * @return The output compact map.
	 */
	public RoadNetworkGraph toCompactMap() {
		if (isCompactMap) {
			LOG.info("The current map is already a compact map, skip the toCompactMap() step.");
			return this;
		}
		boolean wasCompactMap = false;    // the original map was a compact map and we try to merge them back.
		int degree2NodeCount = 0;
		RoadNetworkGraph cloneMap = this.clone();
		List<RoadNode> removeNodeList = new ArrayList<>();
		for (RoadWay way : cloneMap.getWays()) {
			if (way.getId().contains("_S")) {    // check if the previous map was converted from a compact map
				wasCompactMap = true;
				break;
			}
		}
		for (RoadNode node : cloneMap.getAllTypeOfNodes()) {
			if (node.getDegree() == 2 && node.getInComingDegree() == node.getOutGoingDegree()) {
				degree2NodeCount++;
				RoadWay inComingWay = node.getInComingWayList().iterator().next();
				RoadWay outGoingWay = node.getOutGoingWayList().iterator().next();
				String mergedWayID;
				if (wasCompactMap) {
					// Merge the split roads back and try to recover their road ID.
					String inComingID = inComingWay.getId().split("_S")[0];
					String outGoingID = outGoingWay.getId().split("_S")[0];
					if (inComingID.equals(outGoingID)) {
						mergedWayID = inComingID;
					} else
						throw new IllegalArgumentException("Fail to merge two previously separated roads when compacting, ID conflict: "
								+ inComingWay + "," + outGoingWay);
				} else {
					mergedWayID = inComingWay.getId() + "," + outGoingWay.getId();
				}
				List<RoadNode> mergedNodeList = new ArrayList<>();
				List<RoadWay> removeWayList = new ArrayList<>();
				mergedNodeList.addAll(inComingWay.getNodes());
				mergedNodeList.addAll(outGoingWay.getNodes().subList(1, outGoingWay.getNodes().size()));
				node.removeInComingWayFromList(inComingWay);
				node.removeOutGoingWayFromList(outGoingWay);
				removeWayList.add(inComingWay);
				removeWayList.add(outGoingWay);
				cloneMap.removeRoadWayList(removeWayList);
				removeNodeList.add(node);
				RoadWay mergeWay = new RoadWay(mergedWayID, mergedNodeList, cloneMap.getDistanceFunction());
				cloneMap.addWay(mergeWay);
			} else if (node.getDegree() == 2) {
				LOG.warn("Current end point only contains incoming or outgoing roads: " + node.getInComingDegree() + "," + node.getOutGoingDegree());
			}
		}
		for (RoadNode node : removeNodeList) {
			cloneMap.removeNode(node);
		}
		
		// evaluate conversion result
		for (RoadWay way : cloneMap.getWays()) {
			if (way.getId().contains("_S"))
				throw new IllegalArgumentException("The current map still contains unmerged road after compact map conversion: " + way.getId());
		}
		for (RoadNode node : cloneMap.getNodes()) {
			if (node.getDegree() == 2 && node.getInComingDegree() == node.getOutGoingDegree())
				throw new IllegalArgumentException("The current map is still not compact after the compact conversion.");
		}
		cloneMap.isCompactMap = true;
		
		LOG.info("Finish compact map conversion, total number of node removed: " + degree2NodeCount + ". New map contains " + cloneMap.getNodes().size() + " nodes.");
		return cloneMap;
	}
	
	/**
	 * Convert a compact map to its loose form. The roads in a loose map are all straight lines, no polyline appears. New road ID is the
	 * combination
	 *
	 * @return The output simple map.
	 */
	public RoadNetworkGraph toLooseMap() {
		if (!isCompactMap) {
			LOG.info("The current map is already a loose map, skip the toLooseMap() step.");
			return this;
		}
		
		boolean wasLooseMap = false;    // the current map was a loose map and we try to separate it back with its original road ID
		
		RoadNetworkGraph cloneMap = this.clone();
		
		// check if it is a loose map
		for (RoadWay currWay : cloneMap.wayList) {
			if (currWay.getId().contains(",")) {
				wasLooseMap = true;    // the original map was a loose map
				break;
			}
		}
		List<RoadWay> removedWayList = new ArrayList<>();
		List<RoadWay> insertWayList = new ArrayList<>();
		for (RoadWay currWay : cloneMap.getWays()) {
			if (currWay.getNodes().size() > 2) {
				// the current road is a polyline, separate it
				String[] idList = currWay.getId().split(",");
				removedWayList.add(currWay);
				if (wasLooseMap) {
					// retrieve the previous road IDs
					if (idList.length != currWay.getNodes().size() - 1)
						throw new IllegalArgumentException("The current road to be separated during loose map conversion contains " +
								"inconsistent number of previous ids: " + (currWay.getNodes().size() - 1) + "," + idList.length);
				} else {
					if (idList.length != 1)
						throw new IllegalArgumentException("The current road to be separated during loose map conversion contains " +
								"complex road ID: " + currWay.getId());
				}
				for (int i = 1; i < currWay.getNodes().size(); i++) {
					List<RoadNode> insertWayEndNodeList = new ArrayList<>();
					RoadNode startNode = currWay.getNode(i - 1);
					RoadNode endNode = currWay.getNode(i);
					insertWayEndNodeList.add(startNode);
					insertWayEndNodeList.add(endNode);    // the last point already in the intersection list, do not add twice
					if (i != currWay.getNodes().size() - 1)
						cloneMap.addNode(endNode);
					RoadWay insertWay;
					if (wasLooseMap)
						insertWay = new RoadWay(idList[i - 1], insertWayEndNodeList, cloneMap.getDistanceFunction());
					else
						insertWay = new RoadWay(idList[0] + "_S" + (i - 1), insertWayEndNodeList, cloneMap.getDistanceFunction());
					insertWayList.add(insertWay);
				}
			}
		}
		cloneMap.removeRoadWayList(removedWayList);
		cloneMap.addWays(insertWayList);
		
		// evaluate conversion result
		for (RoadWay currWay : cloneMap.getWays()) {
			if (currWay.getNodes().size() != 2)
				throw new IllegalArgumentException("Some roads are still non-straight after the loose map conversion.");
			if (currWay.getId().contains(","))
				throw new IllegalArgumentException("Incorrect road ID after loose map conversion: " + currWay.getId());
		}
		if (this.getAllTypeOfNodes().size() != cloneMap.getNodes().size())
			throw new IllegalArgumentException("The number of nodes changes during the loose map conversion: "
					+ this.getAllTypeOfNodes().size() + "," + cloneMap.getNodes().size());
		isCompactMap = false;
		LOG.info("Finish loose map conversion, total number of roads affected: " + removedWayList.size() + ". Number of new way created: "
				+ insertWayList.size() + ".");
		return cloneMap;
	}
}