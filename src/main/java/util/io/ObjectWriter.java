package util.io;

import util.object.BTStation;
import util.object.OBSequence;

import java.io.File;
import java.io.IOException;
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
	
	public static void writeObSequenceListToFile(List<OBSequence> obSequenceList, String outputFolder, String fileName) {
		IOService.createFolder(outputFolder);
		File file = new File(outputFolder, fileName);
		if (file.exists())
			if (!file.delete()) try {
				throw new IOException("Failed to delete file: " + file.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		List<String> obSequenceListContent = new ArrayList<>();
		for (OBSequence OBSequence : obSequenceList) {
			obSequenceListContent.add(OBSequence.toString());
		}
		IOService.writeFile(obSequenceListContent, outputFolder, fileName);
	}
}