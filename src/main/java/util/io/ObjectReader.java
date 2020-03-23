package util.io;

import org.apache.log4j.Logger;
import util.object.BTStation;
import util.object.ObservationSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reader for different types of objects.
 *
 * @author Hellisk
 * @since 9/09/2019
 */
public class ObjectReader {
	
	private static final Logger LOG = Logger.getLogger(ObjectReader.class);
	
	public static List<BTStation> readBTStationList(String inputFilePath) {
		List<String> stationInfoList = IOService.readFile(inputFilePath);
		List<BTStation> resultStationList = new ArrayList<>();
		for (String info : stationInfoList) {
			resultStationList.add(BTStation.parseBTStation(info));
		}
		return resultStationList;
	}
	
	public static List<ObservationSequence> readObservationSequenceList(String observationFolder, String stationFolder) {
		List<BTStation> stationList = readBTStationList(stationFolder + "Station.txt");
		Map<String, BTStation> id2BTStation = new HashMap<>();
		for (BTStation currStation : stationList) {
			if (!id2BTStation.containsKey(currStation.getID()))
				id2BTStation.put(currStation.getID(), currStation);
			else
				throw new IllegalArgumentException("The same station appears multiple times in station list: " + currStation.getID());
		}
		List<ObservationSequence> resultObSequenceList = new ArrayList<>();
		List<File> inputFileList = IOService.getFiles(observationFolder).collect(Collectors.toList());
		for (File file : inputFileList) {
			resultObSequenceList.addAll(readObservationSequenceList(file, id2BTStation));
		}
		LOG.info("Finish reading observation sequences, total number of sequences: " + resultObSequenceList.size());
		return resultObSequenceList;
	}
	
	private static List<ObservationSequence> readObservationSequenceList(File obSequenceFile, Map<String, BTStation> id2BTStation) {
		List<String> infoList = IOService.readFile(obSequenceFile);
		List<ObservationSequence> obSequenceList = new ArrayList<>();
		for (String s : infoList) {
			obSequenceList.add(ObservationSequence.parseObSequence(s, id2BTStation));
		}
		return obSequenceList;
	}
}
