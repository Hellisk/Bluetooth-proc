import crosby.binary.osmosis.OsmosisReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.function.DistanceFunction;
import util.function.GreatCircleDistanceFunction;
import util.io.MapReader;
import util.io.OSMMapLoader;
import util.object.RoadNetworkGraph;
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
		
		// specify basic file paths
		long initTaskTime = System.currentTimeMillis();
//		String rootPath = "C:/data/BT-Brisbane/";
		String rootPath = "F:/data/BT-Brisbane/";
//		String rootPath = "/media/cifs_dragon/uqpchao/BT-Brisbane/";
		String logPath = rootPath + "log/";
		String logFileName = "BT-Brisbane" + initTaskTime;
		
		// initialize log file
		MapServiceLogger.logInit(logPath, logFileName);
		final Logger LOG = LogManager.getLogger(Main.class);
		
		String rawBTObFolder = rootPath + "raw/observation/";
		int month = -1;    // specify which month of data is processing, =-1 when all data are going to be processed
		String rawOSMMapFolder = rootPath + "raw/map/";
		String rawObSequenceFolder = rootPath + "raw/obSequence/";
		String inputMapFolder = rootPath + "input/map/";
		String inputBTStationFolder = rootPath + "input/btStation/";
		String inputObSequenceFolder = rootPath + "input/obSequence/";
		String outputObSequenceFolder = rootPath + "output/obSequence/";
		DistanceFunction distFunc = new GreatCircleDistanceFunction();
		
		/* data preprocessing step, including converting observations to sequence and map file to road network. */
		// convert observations to sequences
//		double boundaryExtension = 1000;    // extend the boundary of the map by given meters to avoid missing roads
//		Rectangle boundingBox = new Rectangle(152.87669, -27.66475, 153.26154, -27.27603, distFunc);
//		ObservationPreprocess preprocess = new ObservationPreprocess();
//		boundingBox = preprocess.rawObservationLoader(rawBTObFolder, inputBTStationFolder, rawObSequenceFolder, inputObSequenceFolder,
//				boundaryExtension, distFunc);
		// read OpenStreetMap file and convert to RoadNetworkGraph
		InputStream inputStream = new FileInputStream(rawOSMMapFolder + "Brisbane.osm.pbf");
		OsmosisReader reader = new OsmosisReader(inputStream);
		reader.setSink(new OSMMapLoader(inputMapFolder));
		reader.run();
		RoadNetworkGraph rawMap = MapReader.readMap(inputMapFolder + "Brisbane.txt", distFunc);
//		UnfoldingMapDisplay mapDisplay = new UnfoldingMapDisplay();
//		mapDisplay.display();
	}
}