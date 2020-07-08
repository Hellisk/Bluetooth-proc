package preprocessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.function.DistanceFunction;
import util.io.BTObservationLoader;
import util.io.IOService;
import util.io.ObjectWriter;
import util.object.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Preprocess the observation data, including converting raw observation into sequences and sequence segmentation
 *
 * @author Hellisk
 * Created 13/09/2019
 */
public class ObservationPreprocess {
	
	private static final Logger LOG = LogManager.getLogger(ObservationPreprocess.class);
	private int gapCount = 0;    // total number of gaps
	private int obCount = 0;    // total observation count
	private int sequenceCount = 0;    // total number of sequences after segmentation
	private double totalTimeDiff = 0;    // total time difference between pairwise observation
	private double totalDuration = 0;    // total duration within each reader
	private long maxDuration = 0;    // the maximum duration
	private int longDurationObCount = 0;    // number of records whose duration is longer than 300s
	private int lowSpeedSequences = 0;    // average speed is less than 5km/h
	
	/**
	 * Read the original observations from raw files and convert them into observation sequences, each sequence is regarded as a trip
	 * from a particular user, which consists of a sequence of chronologically-ordered observations. The process includes the reading
	 * of Bluetooth observations, the writing of unsegmented sequences (to raw folder) and segmented sequences (to input folder).
	 *
	 * @param rawBTObFolder         Input Bluetooth observation folder.
	 * @param inputBTStationFolder  Input list of Bluetooth stations.
	 * @param rawObSequenceFolder   Output unsegmented Bluetooth sequences.
	 * @param inputObSequenceFolder Output segmented Bluetooth sequences.
	 * @param boundaryExtension     The buffer size of the map boundary.
	 * @param distFunc              Distance function.
	 * @return The boundary of the map region.
	 */
	public Rectangle rawObservationLoader(String rawBTObFolder, String inputBTStationFolder, String rawObSequenceFolder,
										  String inputObSequenceFolder, double boundaryExtension, DistanceFunction distFunc) {
		int maxTimeGap = 1200;    // the maximum time gap (sec) between two observations within one trip, used for sequence segmentation
		IOService.cleanFolder(rawObSequenceFolder);
		IOService.cleanFolder(inputObSequenceFolder);
		Map<String, BTStation> id2BTStation = new LinkedHashMap<>();
		File[] filepathList = new File(rawBTObFolder).listFiles();
		List<File> inputFileList = new ArrayList<>();
		Set<Long> deviceIDSet = new HashSet<>();
		if (filepathList == null)
			throw new NullPointerException("Input observation folder is not found: " + rawBTObFolder);
		BTObservationLoader btObservationLoader = new BTObservationLoader();
		for (int month = 0; month < filepathList.length; month++) {
			File filePath = filepathList[month];
			if (filePath.isDirectory()) {    // the input folder is divided by multiple months, read each month separately
				LOG.info("Processing " + (month + 1) + "/" + filepathList.length + " folder.");
				inputFileList.addAll(IOService.getFiles(filePath.getAbsolutePath()).collect(Collectors.toList()));
				Pair<List<OBSequence>, List<BTStation>> btObResults = btObservationLoader.loadRawObservations(inputFileList, distFunc);
				for (BTStation btStation : btObResults._2()) {
					if (id2BTStation.containsKey(btStation.getID())) {
						if (!id2BTStation.get(btStation.getID()).getCentre().equals2D(btStation.getCentre()))
							throw new IllegalArgumentException("The same Bluetooth reader has different location: " + btStation.getID() + "," +
									id2BTStation.get(btStation.getID()).getCentre().toString() + "," + btStation.getCentre().toString());
					} else
						id2BTStation.put(btStation.getID(), btStation);
				}
				for (OBSequence currObSequence : btObResults._1()) {
					deviceIDSet.add(currObSequence.getDeviceID());
				}
				String fileName = "Sequence_" + filePath.getName().substring(filePath.getName().lastIndexOf('/') + 1) +
						".txt";
				ObjectWriter.writeObSequenceListToFile(btObResults._1(), rawObSequenceFolder, fileName);
				List<OBSequence> segmentedObSeqList = obSequenceSegmentation(btObResults._1(), maxTimeGap, 0, distFunc);
				ObjectWriter.writeObSequenceListToFile(segmentedObSeqList, inputObSequenceFolder, fileName);
				sequenceCount += segmentedObSeqList.size();
				inputFileList = new ArrayList<>();    // empty the current list
			} else {
				inputFileList.add(filePath);
			}
		}
		
		if (!inputFileList.isEmpty()) {    // the folder does not contains more sub-folders
			Pair<List<OBSequence>, List<BTStation>> btObResults = btObservationLoader.loadRawObservations(inputFileList, distFunc);
			for (BTStation btStation : btObResults._2()) {
				if (id2BTStation.containsKey(btStation.getID())) {
					if (!id2BTStation.get(btStation.getID()).getCentre().equals2D(btStation.getCentre()))
						throw new IllegalArgumentException("The same Bluetooth reader has different location: " + btStation.getID() + "," +
								id2BTStation.get(btStation.getID()).getCentre().toString() + "," + btStation.getCentre().toString());
				} else
					id2BTStation.put(btStation.getID(), btStation);
			}
			for (OBSequence currObSequence : btObResults._1()) {
				deviceIDSet.add(currObSequence.getDeviceID());
			}
			ObjectWriter.writeObSequenceListToFile(btObResults._1(), rawObSequenceFolder, "Sequence_all.txt");
			List<OBSequence> segmentedObSeqList = obSequenceSegmentation(btObResults._1(), 1200, 0, distFunc);
			ObjectWriter.writeObSequenceListToFile(segmentedObSeqList, inputObSequenceFolder, "Sequence_all.txt");
			sequenceCount += segmentedObSeqList.size();
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
		
		btObservationLoader.printStatistics();
		ObjectWriter.writeBTStationFile(btStationList, inputBTStationFolder);
		LOG.info("Total number of Bluetooth readers: " + btStationList.size() + ".");
		LOG.info("Total number of Bluetooth devices: " + deviceIDSet.size());
		LOG.info("Current map region is " + minLon + "," + maxLon + "," + minLat + "," + maxLat + ".");
		// extend the bounding box
		minLon = minLon - distFunc.getCoordinateOffsetX(boundaryExtension, (maxLat + minLat) / 2);
		minLat = minLat - distFunc.getCoordinateOffsetY(boundaryExtension, (maxLon + minLon) / 2);
		maxLon = maxLon + distFunc.getCoordinateOffsetX(boundaryExtension, (maxLat + minLat) / 2);
		maxLat = maxLat + distFunc.getCoordinateOffsetY(boundaryExtension, (maxLon + minLon) / 2);
		LOG.info("The bounding box is set to " + minLon + "," + maxLon + "," + minLat + "," + maxLat + " for map extraction.");
		
		LOG.info("Segmentation finished. Total number of sequences: " + sequenceCount + ", total number of observations: "
				+ obCount + ", average observation per sequence: " + (obCount / sequenceCount) + ", average time gap: " + (totalTimeDiff / gapCount) +
				", average duration: " + (totalDuration / obCount) + ", number of long stay points: " + longDurationObCount + ", maximum duration: " + maxDuration
				+ ", number of potential pedestrian sequence (<5km/h): " + lowSpeedSequences + ". ");
		
		return new Rectangle(minLon, minLat, maxLon, maxLat, distFunc);
	}
	
	/**
	 * Segment the observation sequences based on time and average speed. The current sequence is to be divided if its time gap exceed
	 * the threshold and the average speed to the next location is less than 15km/h.
	 *
	 * @param oriSequenceList Original observation sequences.
	 * @param maxTimeGap      The maximum time gap between two consecutive sequences.
	 * @return The segmented observation sequences.
	 */
	public List<OBSequence> obSequenceSegmentation(List<OBSequence> oriSequenceList, int maxTimeGap, int startID,
												   DistanceFunction distFunc) {
		List<OBSequence> resultObSequenceList = new ArrayList<>();
		for (OBSequence currObSeq : oriSequenceList) {
			List<BTObservation> currObList = new ArrayList<>();
			currObList.add(currObSeq.getObservationList().get(0));
			for (int i = 0; i < currObSeq.getObservationList().size() - 1; i++) {
				BTObservation currOb = currObSeq.getObservationList().get(i);
				BTObservation nextOb = currObSeq.getObservationList().get(i + 1);
				boolean isEndReached = false;
				while (nextOb.getLeaveTime() < currOb.getLeaveTime()) {    // next ob is fully contained by the current one, waive it
					i++;
					if (i < currObSeq.getObservationList().size() - 1) {
						nextOb = currObSeq.getObservationList().get(i + 1);
					} else {
						isEndReached = true;
						break;
					}
				}
				if (isEndReached)
					break;
				long timeDiff = nextOb.getEnterTime() - currOb.getLeaveTime();
				double avgSpeed = distFunc.distance(nextOb.getStation().getCentre(), currOb.getStation().getCentre()) / timeDiff;
				if (timeDiff > maxTimeGap && avgSpeed < 4.17) {    // average speed is less than 15km/h
//					if (avgSpeed > 1.39)
//						System.out.println("TEST");
					// cut the current sequence
					if (currObList.size() > 1) {
						OBSequence currSeq = new OBSequence(startID, currObList);
						if (currSeq.length() != 0) {
							startID++;
							resultObSequenceList.add(currSeq);
						}
						currObList = new ArrayList<>();
						currObList.add(nextOb);
					} else    // only one observation exists in the list, ignore it
						currObList = new ArrayList<>();
				} else {
					currObList.add(nextOb);
				}
			}
			if (currObList.size() > 1) {    // the end of the sequence
				OBSequence currSeq = new OBSequence(startID, currObList);
				if (currSeq.length() != 0) {
					startID++;
					resultObSequenceList.add(currSeq);
				}
			}
		}
		for (OBSequence currObSeq : resultObSequenceList) {
			double distance = 0;
			obCount += currObSeq.size();
			long duration = currObSeq.getObservationList().get(0).getLeaveTime() - currObSeq.getObservationList().get(0).getEnterTime();
			totalDuration += Math.min(duration, 300);
			if (duration > 300)
				longDurationObCount++;
			maxDuration = Math.max(maxDuration, duration);
			for (int i = 0; i < currObSeq.getObservationList().size() - 1; i++) {
				BTObservation currOb = currObSeq.getObservationList().get(i);
				BTObservation nextOb = currObSeq.getObservationList().get(i + 1);
				distance += distFunc.distance(currOb.getStation().getCentre(), nextOb.getStation().getCentre());
				totalTimeDiff += Math.max(nextOb.getEnterTime() - currOb.getLeaveTime(), 0);
				duration = nextOb.getLeaveTime() - nextOb.getEnterTime();
				maxDuration = Math.max(maxDuration, duration);
				totalDuration += duration;
				gapCount++;
			}
			double avgSpeed = distance / (currObSeq.getEndTime() - currObSeq.getStartTime());
			if (avgSpeed < 1.39 && avgSpeed > 0)    // pedestrian
				lowSpeedSequences++;
		}
		return resultObSequenceList;
	}
}