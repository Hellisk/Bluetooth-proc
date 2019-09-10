package util.object;

import util.function.DistanceFunction;

import java.util.Map;

/**
 * The observation from the Bluetooth detector, including the device ID, the
 *
 * @author Hellisk
 * @since 6/09/2019
 */
public class BTObservation implements Comparable<BTObservation> {
	private final long deviceID;
	private final long enterTime;
	private final long leaveTime;
	private final BTStation station;
	private String owner;
	
	public BTObservation(long deviceID, long enterTime, long duration, BTStation station, String owner) {
		this.deviceID = deviceID;
		this.enterTime = enterTime;
		this.leaveTime = enterTime + duration;
		this.station = station;
		this.owner = owner;
	}
	
	public static BTObservation parseBTObservation(String info, Map<String, BTStation> id2BTStation) {
		String[] obInfo = info.split(" ");
		if (obInfo.length != 5)
			throw new IllegalArgumentException("Incorrect Bluetooth observation format: " + info);
		if (!id2BTStation.containsKey(obInfo[3]))
			throw new IllegalArgumentException("The Bluetooth reader is not found: " + obInfo[3]);
		return new BTObservation(Long.parseLong(obInfo[0]), Long.parseLong(obInfo[1]), Long.parseLong(obInfo[2]),
				id2BTStation.get(obInfo[3]), obInfo[4]);
	}
	
	public long getDeviceID() {
		return deviceID;
	}
	
	public long getEnterTime() {
		return enterTime;
	}
	
	public long getLeaveTime() {
		return leaveTime;
	}
	
	public BTStation getStation() {
		return station;
	}
	
	public String getOwner() {
		return owner;
	}
	
	@Override
	public int compareTo(BTObservation o) {
		int result = Long.compare(this.enterTime, o.enterTime);
		if (result != 0)
			return result;
		else
			return Long.compare(this.leaveTime, o.leaveTime);
	}
	
	public DistanceFunction getDistFunc() {
		return station.getDistFunc();
	}
	
	@Override
	public String toString() {
		return deviceID + " " + enterTime + " " + leaveTime + " " + station.getID() + " " + owner;
	}
}
