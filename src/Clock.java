abstract class ClockService{
	public void ticks() {}
}

interface ClockFactory {
	ClockService getClock();
}
class LogicalClock extends ClockService{
	
	private int processNo;
	LogicalTimeStamps internalLogicalClock;
	
	private LogicalClock(){
		internalLogicalClock = new LogicalTimeStamps(this.processNo, 0);
	}
	
	public void setProcessNo(int processNo){
		this.processNo = processNo;
	}
	
	public static ClockFactory factory = new ClockFactory(){
		public ClockService getClock() {
			return new LogicalClock();
		}
	};
	
	public void ticks(){
		this.internalLogicalClock.timeStamp++;
	}
}

class VectorClock extends ClockService{
	
	private int processNo;
	private int processCount;
	VectorTimeStamps internalVectorClock;
	
	public void setProcessNo(int processNo){
		this.processNo = processNo;
	}
	
	public void setProcessCount(int processCount){
		this.processCount = processCount;
	}
	
	private VectorClock(){
		internalVectorClock = new VectorTimeStamps(new int[this.processCount]);
	}
	
	public static ClockFactory factory = new ClockFactory(){
		public ClockService getClock() {
			return new VectorClock();
		}
	};
	
	public void ticks(){
		this.internalVectorClock.timeStampMatrix[this.processNo]++;
	}
}

enum ClockType{
	LOGICAL, VECTOR;
}

public class Clock {
	public static ClockService getClockService(ClockFactory factory){
		
		return factory.getClock();
		
	}
}

class LogicalTimeStamps{
	int processNo;
	int timeStamp;
	public LogicalTimeStamps(int processNo, int timeStamp){
		this.processNo = processNo;
		this.timeStamp = timeStamp;
	}
}

class VectorTimeStamps{
	int[] timeStampMatrix;
	public VectorTimeStamps(int[] timeStampMatrix){
		this.timeStampMatrix = timeStampMatrix;
	}
}

