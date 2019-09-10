import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.BTObservationLoader;
import util.io.IOService;
import util.io.ObjectWriter;
import util.object.BTStation;
import util.object.ObservationSequence;
import util.object.Pair;
import util.settings.MapServiceLogger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry of the Bluetooth project.
 *
 * @author Hellisk
 * Created 6/09/2019
 */
public class Main {
	public static void main(String[] args) {
		
		long initTaskTime = System.currentTimeMillis();
//		String rootPath = "C:/data/Bluetooth/";
//		String rootPath = "F:/data/Bluetooth/";
		String rootPath = "/media/cifs_dragon/uqpchao/Bluetooth/";
		String logPath = rootPath + "log/";
		String logFileName = "Bluetooth_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		final Logger LOG = Logger.getLogger(Main.class);
		double boundaryExtension = 1000;    // extend the boundary of the map by given meters to avoid missing roads
		
		String rawBTObFolder = rootPath + "raw/observation/";
		String rawOSMMapFolder = rootPath + "raw/map/";
		String inputMapFolder = rootPath + "input/map/";
		String inputBTStationFolder = rootPath + "input/btStation/";
		String inputObSequenceFolder = rootPath + "input/obSequence/";
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		
		List<ObservationSequence> obSequenceList = new ArrayList<>();
		Map<String, BTStation> id2BTStation = new LinkedHashMap<>();
		File[] filepathList = new File(rawBTObFolder).listFiles();
		List<File> inputFileList = new ArrayList<>();
		if (filepathList == null)
			throw new NullPointerException("Input observation folder is not found: " + rawBTObFolder);
		BTObservationLoader btObservationLoader = new BTObservationLoader();
		int folderCount = 0;
		for (int i = 0; i < filepathList.length; i++) {
			File filePath = filepathList[i];
			if (filePath.isDirectory()) {    // the input folder is divided by multiple months, read each month separately
				LOG.info("Processing " + (i + 1) + "/" + filepathList.length + " folder.");
				inputFileList.addAll(IOService.getFiles(filePath.getAbsolutePath()).collect(Collectors.toList()));
				Pair<List<ObservationSequence>, List<BTStation>> btObResults = btObservationLoader.loadRawObservations(inputFileList, distFunc);
				obSequenceList.addAll(btObResults._1());
				for (BTStation btStation : btObResults._2()) {
					if (id2BTStation.containsKey(btStation.getID())) {
						if (!id2BTStation.get(btStation.getID()).getCentre().equals2D(btStation.getCentre()))
							throw new IllegalArgumentException("The same Bluetooth reader has different location: " + btStation.getID() + "," +
									id2BTStation.get(btStation.getID()).getCentre().toString() + "," + btStation.getCentre().toString());
					} else
						id2BTStation.put(btStation.getID(), btStation);
				}
				inputFileList = new ArrayList<>();    // empty the current list
			} else {
				inputFileList.add(filePath);
			}
		}
		
		if (!inputFileList.isEmpty()) {
			Pair<List<ObservationSequence>, List<BTStation>> btObResults = btObservationLoader.loadRawObservations(inputFileList, distFunc);
			obSequenceList.addAll(btObResults._1());
			for (BTStation btStation : btObResults._2()) {
				if (id2BTStation.containsKey(btStation.getID())) {
					if (!id2BTStation.get(btStation.getID()).getCentre().equals2D(btStation.getCentre()))
						throw new IllegalArgumentException("The same Bluetooth reader has different location: " + btStation.getID() + "," +
								id2BTStation.get(btStation.getID()).getCentre().toString() + "," + btStation.getCentre().toString());
				} else
					id2BTStation.put(btStation.getID(), btStation);
			}
		}
		
		double minLon = Double.POSITIVE_INFINITY;
		double minLat = Double.POSITIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		double maxLat = Double.NEGATIVE_INFINITY;
		List<BTStation> btStationList = new ArrayList<>();
		for (BTStation station : id2BTStation.values()) {
			minLon = Math.min(station.getCentre().x(), minLon);
			minLat = Math.min(station.getCentre().y(), minLat);
			maxLon = Math.max(station.getCentre().x(), maxLon);
			maxLat = Math.max(station.getCentre().y(), maxLat);
			btStationList.add(station);
		}
		Set<Long> deviceIDSet = new HashSet<>();
		for (ObservationSequence currObSequence : obSequenceList) {
			deviceIDSet.add(currObSequence.getDeviceID());
		}
		btObservationLoader.printStatistics();
		LOG.info("Total number of Bluetooth readers: " + btStationList.size() + ".");
		LOG.info("Current map region is " + minLon + "," + maxLon + "," + minLat + "," + maxLat + ". Start writing the data.");
		ObjectWriter.writeBTStationFile(btStationList, inputBTStationFolder);
		ObjectWriter.writeObservationSequence(obSequenceList, inputObSequenceFolder);
		// extend the bounding box
		minLon = minLon - distFunc.getCoordinateOffsetX(boundaryExtension, (maxLat + minLat) / 2);
		minLat = minLat - distFunc.getCoordinateOffsetY(boundaryExtension, (maxLon + minLon) / 2);
		maxLon = maxLon + distFunc.getCoordinateOffsetX(boundaryExtension, (maxLat + minLat) / 2);
		minLon = maxLat + distFunc.getCoordinateOffsetY(boundaryExtension, (maxLon + minLon) / 2);
		LOG.info("The bounding box is set to " + minLon + "," + maxLon + "," + minLat + "," + maxLat + " for map extraction.");

//		Rectangle boundingBox = new Rectangle(minLon, minLat, maxLon, maxLat, distFunc);
//		InputStream inputStream = new FileInputStream(rawOSMMapFolder + "");
//		OsmosisReader reader = new OsmosisReader(inputStream);
//		reader.setSink(new OSMMapLoader(inputMapFolder));
//		reader.run();
	}
}
