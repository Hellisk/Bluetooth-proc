package util.display;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.marker.SimplePointMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import processing.core.PApplet;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.ObjectReader;
import util.object.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Display dataset using Unfolding map visualisation api.
 *
 * @author Hellisk
 */
//http://unfoldingmaps.org/
public class UnfoldingMapDisplay extends PApplet {
	String rootPath = "C:/data/Bluetooth/";
	//	String rootPath = "F:/data/Bluetooth/";
	String inputMapFolder = rootPath + "input/map/";
	String inputBTStationFolder = rootPath + "input/btStation/";
	String inputObSequenceFolder = rootPath + "input/obSequence/";
	int[] blue = {0, 128, 255};
	int[] green = {102, 255, 178};
	int[] red = {255, 0, 0};
	int[] lightPurple = {255, 0, 255};
	int[] pink = {255, 153, 153};
	int[] black = {0, 0, 0};
	int[] grey = {220, 220, 220};
	private UnfoldingMap currMapDisplay;
	private DistanceFunction distFunc = new GreatCircleDistanceFunction();
	private UnfoldingMap fullMapDisplay;    // full map visualization
	
	public void setup() {
		size(1440, 990);
//		this.fullMapDisplay = new UnfoldingMap(this, new OpenStreetMap.OpenStreetMapProvider());
//		this.fullMapDisplay = new UnfoldingMap(this, new Microsoft.HybridProvider());
		this.fullMapDisplay = new UnfoldingMap(this, new Google.GoogleMapProvider());
		MapUtils.createMouseEventDispatcher(this, fullMapDisplay);
//		MapUtils.createMouseEventDispatcher(this, compareMapDisplay);
		
		
		// read the complete map, fill into fullMapDisplay
		RoadNetworkGraph rawMap = MapReader.readMap(inputMapFolder + "Brisbane.txt", distFunc);
		Map<String, RoadWay> id2WayMap = new HashMap<>();
		for (RoadWay way : rawMap.getWays()) {
			if (!id2WayMap.containsKey(way.getId())) {
				id2WayMap.put(way.getId(), way);
			} else
				throw new IllegalArgumentException("Two roads has the same id:" + way.getId());
		}
//		List<Marker> mapMarker = roadWayMarkerGen(rawMap.getWays(), grey, 2);
//		fullMapDisplay.addMarkers(mapMarker);
		
		// set map centre
		Location mapCenter = new Location((float) (rawMap.getMaxLat() + rawMap.getMinLat()) / 2, (float) (rawMap
				.getMaxLon() + rawMap.getMinLon()) / 2);
		fullMapDisplay.zoomAndPanTo(14, mapCenter);
//		compareMapDisplay.zoomAndPanTo(14, mapCenter);
		
		List<BTStation> btStationList = ObjectReader.readBTStationList(inputBTStationFolder + "station.txt");
		for (BTStation btStation : btStationList) {
			fullMapDisplay.addMarker(pointMarkerGen(btStation.getCentre(), blue, 2));
		}
		
		currMapDisplay = fullMapDisplay;
	}
	
	private List<Marker> roadWayMarkerGen(List<RoadWay> w, int[] color, int strokeWeight) {
		List<Marker> result = new ArrayList<>();
		for (RoadWay currWay : w) {
			List<Location> locationList = new ArrayList<>();
			for (RoadNode n : currWay.getNodes()) {
//				Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.lon(), n.lat());        // for map provider other than Google
//				Pair<Double, Double> currPoint = new Pair<>(n.lon(), n.lat());
				Location pointLocation = new Location(n.lat(), n.lon());
				locationList.add(pointLocation);
			}
			SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
			currLineMarker.setColor(color(color[0], color[1], color[2]));
			currLineMarker.setStrokeWeight(strokeWeight);
			result.add(currLineMarker);
		}
		return result;
	}
	
	private Marker pointMarkerGen(Point point, int[] color, int strokeWeight) {
//			Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(n.x(), n.y());
//			Pair<Double, Double> currPoint = new Pair<>(n.x(), n.y());
		Location pointLocation = new Location(point.y(), point.x());
		SimplePointMarker currPointMarker = new SimplePointMarker(pointLocation);
		currPointMarker.setColor(color(pink[0], pink[1], pink[2], 70));
		currPointMarker.setRadius(120);
		currPointMarker.setStrokeColor(color(color[0], color[1], color[2]));
		currPointMarker.setStrokeWeight(strokeWeight);
		return currPointMarker;
	}
	
	private List<Marker> obSequenceMarkerGen(List<OBSequence> w, int[] color, int strokeWeight) {
		List<Marker> result = new ArrayList<>();
		for (OBSequence currObSequence : w) {
			List<Location> locationList = new ArrayList<>();
			for (BTObservation currOb : currObSequence.getObservationList()) {
//				Pair<Double, Double> currPoint = SpatialUtils.convertGCJ2WGS(currOb.lon(), currOb.lat());        // for map provider other than Google
//				Pair<Double, Double> currPoint = new Pair<>(currOb.lon(), currOb.lat());
				Point location = currOb.getStation().getCentre();
				Location pointLocation = new Location(location.y(), location.x());
				locationList.add(pointLocation);
			}
			SimpleLinesMarker currLineMarker = new SimpleLinesMarker(locationList);
			currLineMarker.setColor(color(color[0], color[1], color[2]));
			currLineMarker.setStrokeWeight(strokeWeight);
			result.add(currLineMarker);
		}
		return result;
	}
//
//	public void keyPressed() {
//		switch (key) {
//			case '1': {
//				currMapDisplay = fullMapDisplay;
//				break;
//			}
//			case '2': {
//				currMapDisplay = compareMapDisplay;
//				break;
//			}
//			default:
//				break;
//		}
//	}
	
	public void draw() {
		currMapDisplay.draw();
	}
	
	public void display() {
		PApplet.main(new String[]{this.getClass().getName()});
	}
	
}