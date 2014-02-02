import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	String source = "***";

	String destination = "***";

	String kind = "***";

	Object data = "***";

	int sequenceNumber = -1;

	String action = "***";

	boolean duplicate = false;

	public Message(String dest, String kind, Object data) {
		if (dest != null) {
			destination = dest;
		}
		if (kind != null) {
			this.kind = kind;
		}
		if (data != null) {
			this.data = data;
		}
	}

	public void set_source(String source) {
		this.source = source;
	}

	public void set_seqNum(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void set_duplicate() {
		this.duplicate = true;
	}

	public void set_action(String action) {
		this.action = action;
	}

	public String toString() {
		return "[source=" + source + "; destination=" + destination + "; kind=" + kind + "; data=" + (String) data + "; seqNum=" + sequenceNumber + "; action=" + action + "; duplicate=" + duplicate + "]";
	}
}

class TimeStampedMessage extends Message {

	private static final long serialVersionUID = 1L;

	private ClockType clockType;

	private LogicalTimeStamps logicalTimeStamps;

	private VectorTimeStamps vectorTimeStamps;

	public TimeStampedMessage(String dest, String kind, Object data,
			ClockType clockType) {
		super(dest, kind, data);
		this.clockType = clockType;
	}

	public ClockType getClockType() {
		return clockType;
	}

	public LogicalTimeStamps getLogicalTimeStamps() {
		return this.logicalTimeStamps;
	}

	public void setLogicalTimeStamps(LogicalTimeStamps lts) {
		this.logicalTimeStamps = lts;
	}

	public VectorTimeStamps getVectorTimeStamps() {
		return this.vectorTimeStamps;
	}

	public void setVectorTimeStamps(VectorTimeStamps vts) {
		this.vectorTimeStamps = vts;
	}

	public String toString() {
		switch (this.clockType) {
			case LOGICAL:
				return "[source=" + source + "; destination=" + destination + "; kind=" + kind + "; data=" + (String) data + "; seqNum=" + sequenceNumber + "; action=" + action + "; duplicate=" + duplicate + "; processNo=" + logicalTimeStamps.processNo + "; logicalTimeStamp=" + logicalTimeStamps.timeStamp + "]";
			case VECTOR:
				return "[source=" + source + "; destination=" + destination + "; kind=" + kind + "; data=" + (String) data + "; seqNum=" + sequenceNumber + "; action=" + action + "; duplicate=" + duplicate + "; vectorTimeStamp=" + Arrays.toString(vectorTimeStamps.timeStampMatrix) + "]";

			default:
				return "TIME STAMP MESSAGE ERROR";
		}
	}
}
