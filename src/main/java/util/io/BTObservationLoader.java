package util.io;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.*;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Parse the observation data from the raw file. The file read generates a list of Bluetooth stations and a list of Bluetooth observation
 * sequences.
 *
 * @author Hellisk
 * Created 6/09/2019
 */
public class BTObservationLoader {
	
	private static final Logger LOG = Logger.getLogger(BTObservation.class);
	private long sequenceCount = 0;
	private long obCount = 0;
	private long numOfWrongOrderPairs = 0;
	private long numOfIncludedPairs = 0;
	private long numOfWrongOrderSequence = 0;
	private long numOfUniqueStationVisit = 0;
	private double totalTimeDiff = 0;
	private long totalGaps = 0;
	private int overlapStationCount = 0;
	
	/**
	 * Load the original Bluetooth observations and generate the observation sequence for each device and the all Bluetooth station
	 * information.
	 *
	 * @param inputFileList Input Bluetooth observation file list.
	 * @param distFunc      Distance function.
	 * @return List of observation sequences, each of which belongs to a device, list of Bluetooth station information.
	 */
	public Pair<List<ObservationSequence>, List<BTStation>> loadRawObservations(List<File> inputFileList,
																				DistanceFunction distFunc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, BTStation> id2BTStation = new LinkedHashMap<>();
		Map<Long, List<BTObservation>> deviceID2ObList = new LinkedHashMap<>();
		int fileCount = 0;
		for (File file : inputFileList) {
			List<String> lines = IOService.readFile(file);
			for (String line : lines) {
				String[] info = line.split(",");
				if (info[0].equals("deviceid"))
					continue;    // the first line which is the column titles.
				if (info.length != 7)
					throw new IllegalArgumentException("Input record format length is wrong: " + line);
				BTStation currStation;
				if (id2BTStation.containsKey(info[3])) {
					currStation = id2BTStation.get(info[3]);
					Point currCentre = new Point(Double.parseDouble(info[5]), Double.parseDouble(info[4]), distFunc);
					if (!currStation.getCentre().equals2D(currCentre)) {
						LOG.error("The same station has different coordinates: " + info[3] + "," + currStation.getCentre().toString() +
								"," + info[5] + " " + info[4] + ". Distance: " + distFunc.distance(currStation.getCentre(), currCentre));
					}
				} else {
					currStation = new BTStation(info[3], Double.parseDouble(info[5]), Double.parseDouble(info[4]), distFunc);
					id2BTStation.put(currStation.getID(), currStation);
				}
				Date enterDate;
				try {
					enterDate = dateFormat.parse(info[1]);
				} catch (ParseException e) {
					LOG.error("The date information is not parsable: " + line);
					continue;
				}
				BTObservation currOb = new BTObservation(Long.parseLong(info[0]), enterDate.getTime() / 1000, Long.parseLong(info[2]),
						currStation, info[6]);
				if (deviceID2ObList.containsKey(currOb.getDeviceID())) {
					deviceID2ObList.get(currOb.getDeviceID()).add(currOb);
				} else {
					List<BTObservation> obList = new ArrayList<>();
					obList.add(currOb);
					deviceID2ObList.put(currOb.getDeviceID(), obList);
				}
			}
			fileCount++;
			LOG.info("Processed the " + fileCount + "/" + inputFileList.size() + " file.");
		}
		
		List<ObservationSequence> obSequenceList = new ArrayList<>();
		List<BTStation> btStationList = new ArrayList<>();
		Map<String, Set<String>> loc2BTStation = new LinkedHashMap<>();
		for (List<BTObservation> obList : deviceID2ObList.values()) {
			Set<String> visitedBTStationSet = new HashSet<>();
			Collections.sort(obList);
			for (BTObservation ob : obList) {
				visitedBTStationSet.add(ob.getStation().getID());
			}
			ObservationSequence currObSequence = new ObservationSequence(sequenceCount, obList);
			obSequenceList.add(currObSequence);
			boolean isWrongOrderedSequence = false;
			if (currObSequence.size() > 1) {
				for (int i = 0; i < currObSequence.size() - 1; i++) {
					BTObservation currObservation = currObSequence.getObservationList().get(i);
					BTObservation nextObservation = currObSequence.getObservationList().get(i + 1);
					if (currObservation.getLeaveTime() > nextObservation.getEnterTime()) {
						if (currObservation.getLeaveTime() > nextObservation.getLeaveTime()) {
							LOG.debug("The next observation is completely included in the last observation in sequence " +
									currObSequence.getSequenceID() + "," + i + "," + nextObservation.getEnterTime() + "," +
									nextObservation.getLeaveTime() + "," + currObservation.getEnterTime() + "," + currObservation.getLeaveTime());
							numOfIncludedPairs++;
						} else {
							LOG.debug("The next observation starts before the current observation in sequence " + currObSequence.getSequenceID()
									+ "," + i + "," + currObservation.getLeaveTime() + "," + nextObservation.getEnterTime());
						}
						numOfWrongOrderPairs++;
						isWrongOrderedSequence = true;
					} else {
						totalTimeDiff += nextObservation.getEnterTime() - currObservation.getLeaveTime();
						totalGaps++;
					}
				}
			}
			if (isWrongOrderedSequence)
				numOfWrongOrderSequence++;
			numOfUniqueStationVisit += visitedBTStationSet.size();
			sequenceCount++;
			obCount += currObSequence.size();
		}
		double minLon = Double.POSITIVE_INFINITY;
		double minLat = Double.POSITIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		double maxLat = Double.NEGATIVE_INFINITY;
		for (BTStation station : id2BTStation.values()) {
			String location = station.getCentre().toString();
			if (loc2BTStation.containsKey(location)) {
				loc2BTStation.get(location).add(station.getID());
			} else {
				minLon = Math.min(station.getCentre().x(), minLon);
				minLat = Math.min(station.getCentre().y(), minLat);
				maxLon = Math.max(station.getCentre().x(), maxLon);
				maxLat = Math.max(station.getCentre().y(), maxLat);
				Set<String> stationIDSet = new LinkedHashSet<>();
				stationIDSet.add(station.getID());
				loc2BTStation.put(location, stationIDSet);
				btStationList.add(station);
			}
		}
		LOG.info("Current map region is :" + minLon + "," + maxLon + "," + minLat + "," + maxLat);
		
		for (Map.Entry<String, Set<String>> entry : loc2BTStation.entrySet()) {
			if (entry.getValue().size() != 1) {    // multiple stations share the same location
				StringBuilder idListString = new StringBuilder();
				for (String id : entry.getValue()) {
					idListString.append(id).append(",");
				}
				LOG.info("The current location " + entry.getKey() + " has multiple Bluetooth readers: " + idListString.toString().substring(0, idListString.lastIndexOf(",")));
				overlapStationCount += 1;
			}
		}
		return new Pair<>(obSequenceList, btStationList);
	}
	
	public void printStatistics() {
		LOG.info("Bluetooth record read finished. Total number of observations: " + obCount + ", sequences: " + sequenceCount + ", " +
				"average observation per sequence: " + (obCount / sequenceCount) + ", number of unique station visit per sequence: " +
				(numOfUniqueStationVisit / sequenceCount) + ".");
		LOG.info("Average time gap between consecutive Bluetooth readers: " + totalTimeDiff / totalGaps);
		LOG.info("Total number of incorrect time sequence: " + numOfWrongOrderSequence + ", incorrect pairs " + numOfWrongOrderPairs +
				", " + "record that is completely contained by its preceding observation: " + numOfIncludedPairs);
		LOG.info("Total number of locations that have multiple stations assigned: " + overlapStationCount + ".");
	}
}
