package util.io;

import util.object.BTStation;
import util.object.ObservationSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reader for different types of objects.
 *
 * @author Hellisk
 * @since 9/09/2019
 */
public class ObjectReader {
	public static List<BTStation> readBTStationList(String inputFolder) {
		List<File> inputFileList = IOService.getFiles(inputFolder).collect(Collectors.toList());
		List<BTStation> resultStationList = new ArrayList<>();
		for (File file : inputFileList) {
			List<String> stationInfoList = IOService.readFile(file);
			for (String info : stationInfoList) {
				resultStationList.add(BTStation.parseBTStation(info));
			}
		}
		return resultStationList;
	}
	
	public static Stream<ObservationSequence> readObservationSequenceStream(String observationFolder, String stationFolder) throws InterruptedException, ExecutionException {
		List<BTStation> stationList = readBTStationList(stationFolder);
		Map<String, BTStation> id2BTStation = new HashMap<>();
		for (BTStation currStation : stationList) {
			if (!id2BTStation.containsKey(currStation.getID()))
				id2BTStation.put(currStation.getID(), currStation);
			else
				throw new IllegalArgumentException("The same station appears multiple times in station list: " + currStation.getID());
		}
		List<ObservationSequence> resultObSequenceList = new ArrayList<>();
		Stream<File> inputFileStream = IOService.getFiles(observationFolder);
		ForkJoinPool pool = ForkJoinPool.commonPool();
		ForkJoinTask<Stream<ObservationSequence>> resultStream =
				pool.submit(() -> inputFileStream.parallel().map(x -> readObservationSequence(x, id2BTStation)));
		while (!resultStream.isDone())
			Thread.sleep(5);
		return resultStream.get();
	}
	
	public static ObservationSequence readObservationSequence(File obSequenceFile, Map<String, BTStation> id2BTStation) {
		String sequenceID = obSequenceFile.getName().substring(obSequenceFile.getName().lastIndexOf('_' + 1),
				obSequenceFile.getName().lastIndexOf('.'));
		List<String> infoList = IOService.readFile(obSequenceFile);
		return ObservationSequence.parseObSequence(infoList, Long.parseLong(sequenceID), id2BTStation);
	}
}
