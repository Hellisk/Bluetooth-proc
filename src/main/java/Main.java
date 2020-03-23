import crosby.binary.osmosis.OsmosisReader;
import org.apache.log4j.Logger;
import preprocessing.ObservationPreprocess;
import util.display.UnfoldingMapDisplay;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.OSMMapLoader;
import util.object.Rectangle;
import util.settings.MapServiceLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Main entry of the Bluetooth project.
 *
 * @author Hellisk
 * Created 6/09/2019
 */
public class Main {
	
	
	public static void main(String[] args) throws FileNotFoundException {
		
		long initTaskTime = System.currentTimeMillis();
		String rootPath = "C:/data/Bluetooth/";
//		String rootPath = "F:/data/Bluetooth/";
//		String rootPath = "/media/cifs_dragon/uqpchao/Bluetooth/";
		String logPath = rootPath + "log/";
		String logFileName = "Bluetooth_" + initTaskTime;
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		final Logger LOG = Logger.getLogger(Main.class);
		
		String rawBTObFolder = rootPath + "raw/observation/";
		int month = -1;    // specify which month of data is processing, =-1 when all data are going to be processed
		String rawOSMMapFolder = rootPath + "raw/map/";
		String rawObSequenceFolder = rootPath + "raw/obSequence/";
		String inputMapFolder = rootPath + "input/map/";
		String inputBTStationFolder = rootPath + "input/btStation/";
		String inputObSequenceFolder = rootPath + "input/obSequence/";
		String outputObSequenceFolder = rootPath + "output/obSequence/";
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		double boundaryExtension = 1000;    // extend the boundary of the map by given meters to avoid missing roads
		Rectangle boundingBox = new Rectangle(152.87669, -27.66475, 153.26154, -27.27603, distFunc);
		ObservationPreprocess preprocess = new ObservationPreprocess();
		boundingBox = preprocess.rawObservationLoader(rawBTObFolder, inputBTStationFolder, rawObSequenceFolder, inputObSequenceFolder,
				boundaryExtension, distFunc);
//
		InputStream inputStream = new FileInputStream(rawOSMMapFolder + "Brisbane.osm.pbf");
		OsmosisReader reader = new OsmosisReader(inputStream);
		reader.setSink(new OSMMapLoader(inputMapFolder));
		reader.run();
		
		UnfoldingMapDisplay mapDisplay = new UnfoldingMapDisplay();
		mapDisplay.display();
	}
}
