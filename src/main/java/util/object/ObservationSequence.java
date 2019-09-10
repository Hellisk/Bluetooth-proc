package util.object;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A sequence of chronologically ordered observation for a particular device.
 *
 * @author Hellisk
 * Created 6/09/2019
 */
public class ObservationSequence {
	
	private static final Logger LOG = Logger.getLogger(ObservationSequence.class);
	
	private long sequenceID;
	private long deviceID;
	private List<BTObservation> observationList;
	private long startTime;
	private long endTime;
	
	/**
	 * Construct an observation sequence for a single device. The observations should be sorted already before forming a sequence.
	 *
	 * @param sequenceID   The id of the sequence.
	 * @param observations The sorted list of observations.
	 */
	public ObservationSequence(long sequenceID, List<BTObservation> observations) {
		this.sequenceID = sequenceID;
		this.observationList = observations;
		if (observations.size() != 0) {
			this.startTime = observations.get(0).getEnterTime();
			this.endTime = observations.get(observations.size() - 1).getLeaveTime();
			this.deviceID = observations.get(0).getDeviceID();
		} else {
			this.startTime = Long.MAX_VALUE;
			this.endTime = Long.MAX_VALUE;
			this.deviceID = -1;
		}
	}
	
	public static ObservationSequence parseObSequence(List<String> info, long sequenceID, Map<String, BTStation> id2BTStation) {
		if (info.size() != 2)
			throw new IllegalArgumentException("Incorrect observation sequence element length: " + info.size());
		String[] baseInfo = info.get(0).split(" ");
		if (baseInfo.length != 3)
			throw new IllegalArgumentException("Incorrect observation sequence base information: " + baseInfo.length);
		String[] obInfo = info.get(1).split("\\|");
		List<BTObservation> obList = new ArrayList<>();
		for (String s : obInfo) {
			obList.add(BTObservation.parseBTObservation(s, id2BTStation));
		}
		ObservationSequence currObSequence = new ObservationSequence(sequenceID, obList);
		if (currObSequence.getDeviceID() != Long.parseLong(baseInfo[0]) || currObSequence.getStartTime() != Long.parseLong(baseInfo[1])
				|| currObSequence.getEndTime() != Long.parseLong(baseInfo[2]))
			throw new IllegalArgumentException("The constructed observation sequence has different information as provided: " +
					currObSequence.getDeviceID() + " " + currObSequence.getStartTime() + " " + currObSequence.getEndTime() + "," + info.get(0));
		return currObSequence;
	}
	
	public long getSequenceID() {
		return sequenceID;
	}
	
	public List<BTObservation> getObservationList() {
		return observationList;
	}
	
	public void setObservationList(List<BTObservation> observationList) {
		this.observationList = observationList;
		if (observationList.size() != 0) {
			this.startTime = observationList.get(0).getEnterTime();
			this.endTime = observationList.get(observationList.size() - 1).getLeaveTime();
			this.deviceID = observationList.get(0).getDeviceID();
		} else {
			this.startTime = Long.MAX_VALUE;
			this.endTime = Long.MAX_VALUE;
			this.deviceID = -1;
		}
	}
	
	public void addObservation(BTObservation observation) {
		if (this.observationList.isEmpty()) {
			List<BTObservation> observationList = new ArrayList<>();
			observationList.add(observation);
			setObservationList(observationList);
		} else {
			if (observation.getEnterTime() < this.getEndTime())
				throw new RuntimeException("The new observation should not be added to the end of the current sequence due to early " +
						"detect time: " + observation.getEnterTime() + "," + this.getEndTime());
			this.getObservationList().add(observation);
			this.endTime = observation.getLeaveTime();
		}
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}
	
	public int size() {
		return observationList.size();
	}
	
	/**
	 * Validate if the sequence is chronologically sorted.
	 *
	 * @return True if the sequence satisfy the requirement.
	 */
	public boolean chronologyCheck() {
		if (this.observationList.size() > 1) {
			for (int i = 0; i < observationList.size() - 1; i++) {
				BTObservation currObservation = observationList.get(i);
				BTObservation nextObservation = observationList.get(i + 1);
				if (currObservation.getLeaveTime() > nextObservation.getEnterTime()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public long getDeviceID() {
		return deviceID;
	}
	
	@Override
	public String toString() {
		String firstLine = deviceID + " " + startTime + " " + endTime;
		StringBuilder secondLine = new StringBuilder();
		for (BTObservation currOb : observationList) {
			secondLine.append(currOb.toString()).append("|");
		}
		secondLine.substring(0, secondLine.length() - 1);
		return firstLine + "\n" + secondLine.toString();
	}
}