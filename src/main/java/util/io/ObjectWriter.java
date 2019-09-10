package util.io;

import util.object.BTStation;
import util.object.ObservationSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer for different types of objects.
 *
 * @author Hellisk
 * Created 9/09/2019
 */
public class ObjectWriter {
	public static void writeBTStationFile(List<BTStation> btStationList, String outputFolder) {
		List<String> btStationStringList = new ArrayList<>();
		for (BTStation currStation : btStationList) {
			btStationStringList.add(currStation.toString());
		}
		IOService.createFolder(outputFolder);
		IOService.cleanFolder(outputFolder);
		IOService.writeFile(btStationStringList, outputFolder, "station.txt");
	}
	
	public static void writeObservationSequence(List<ObservationSequence> obSequenceList, String outputFolder) {
		IOService.createFolder(outputFolder);
		IOService.cleanFolder(outputFolder);
		for (ObservationSequence currObSequence : obSequenceList) {
			IOService.writeFile(currObSequence.toString(), outputFolder, "Sequence_" + currObSequence.getSequenceID() + ".txt");
		}
	}
}
