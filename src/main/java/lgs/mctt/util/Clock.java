package lgs.mctt.util;

public class Clock {
	public int min, max;
	public long next;
	
	public Clock(int min, int max, long next){
		this.min = min;
		this.max = max;
		this.next = next;
	}
	
	public boolean check(){
		if (System.currentTimeMillis() >= next){
			set();
			return true;
		}
		return false;
	}
	
	public void set(){
		next = System.currentTimeMillis() + (long)(Math.random() * (max - min)) + min; // Set next time check to a random time between min and max added to the last.
	}
	
}