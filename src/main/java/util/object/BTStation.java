package util.object;

import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Bluetooth station that detects the Bluetooth devices. The station is located at a fixed location, with a circle range that covers
 * one or multiple intersections.
 *
 * @author Hellisk
 * @since 6/09/2019
 */
public class BTStation {
	private final String stationID;
	private final Point centre;
	
	private double radius = 100;
	private List<String> coveringNodeIDList = new ArrayList<>();
	private DistanceFunction distFunc;
	
	public BTStation(String stationID, double lon, double lat, DistanceFunction distFunc) {
		this.stationID = stationID;
		this.distFunc = distFunc;
		this.centre = new Point(lon, lat, distFunc);
	}
	
	public static BTStation parseBTStation(String info) {
		String[] stationInfo = info.split("\\|");
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		if (stationInfo.length < 1 || stationInfo.length > 2)
			throw new IllegalArgumentException("Incorrect input Bluetooth reader format: " + info);
		String[] baseInfo = stationInfo[0].split(" ");
		if (baseInfo.length < 3 || baseInfo.length > 4)
			throw new IllegalArgumentException("Incorrect input Bluetooth reader format: " + info);
		BTStation currStation = new BTStation(baseInfo[0], Double.parseDouble(baseInfo[1]), Double.parseDouble(baseInfo[2])
				, distFunc);
		if (baseInfo.length == 4)
			currStation.setRadius(Double.parseDouble(baseInfo[3]));
		if (stationInfo.length == 2) {    // contains covering node list
			String[] nodeIDs = stationInfo[1].split(" ");
			List<String> coveringNodeID = new ArrayList<>(Arrays.asList(nodeIDs));
			currStation.setCoveringNodeIDList(coveringNodeID);
		}
		return currStation;
	}
	
	public String getID() {
		return stationID;
	}
	
	public Point getCentre() {
		return centre;
	}
	
	public double getRadius() {
		return radius;
	}
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public List<String> getCoveringNodeIDList() {
		return coveringNodeIDList;
	}
	
	public void setCoveringNodeIDList(List<String> coveringNodeIDList) {
		this.coveringNodeIDList = coveringNodeIDList;
	}
	
	public void addCoveringNode(String nodeID) {
		this.coveringNodeIDList.add(nodeID);
	}
	
	public DistanceFunction getDistFunc() {
		return distFunc;
	}
	
	@Override
	public String toString() {
		StringBuilder baseInfo = new StringBuilder(stationID + " " + centre.toString());
		if (radius != 100)
			baseInfo.append(" ").append(radius);
		if (!coveringNodeIDList.isEmpty()) {
			baseInfo.append("|");
			for (String currNode : coveringNodeIDList) {
				baseInfo.append(currNode).append(" ");
			}
			baseInfo.substring(0, baseInfo.length() - 1);
		}
		return baseInfo.toString();
	}
}
