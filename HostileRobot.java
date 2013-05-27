package aiml;
import java.awt.geom.Point2D;

import robocode.ScannedRobotEvent;

//***********************//
//Jonah Hooper - 728 237//
//***********************//
public class HostileRobot {
	private String name;
	private ScannedRobotEvent prev;
	private ScannedRobotEvent curr;
	private final double LOSS_FROM_SHOT = 3.0;
	private final double AVOIDZONE = 1000; //This the radius of the area a robot will attempt to avoid in its travels (Kludge)
	public HostileRobot (ScannedRobotEvent e){
		name = e.getName();
		prev = e;
		curr = e;
	}
	public void update (ScannedRobotEvent e) {
		if (e.getName() == name) {
			prev = curr;
			curr = e;
			
		}
	}
	public double hostileShotDetected () {
		return prev.getEnergy() - curr.getEnergy();
	}
	public boolean hasFired (){
		double firedif = hostileShotDetected ();
		//System.out.println("Edif: " + firedif);
		return firedif > 0.1 && firedif <= LOSS_FROM_SHOT;
	}
	public ScannedRobotEvent getCurrent () {
		return curr;
	}
	public Point2D getCurrentPosition(Point2D sightedPosition, double heading) {
		double absangle = curr.getBearingRadians() + heading;
		
		return new Point2D.Double(curr.getDistance() * Math.sin(absangle) + sightedPosition.getX()
				, curr.getDistance() * Math.cos(absangle) + sightedPosition.getY());
	}
	public boolean isInAvoidZone (Point2D point, Point2D positionSighting, double heading) {
		Point2D hostilePoint = this.getCurrentPosition(positionSighting, heading);
		return Math.pow(hostilePoint.getX() - point.getX(), 2)
				+ Math.pow(hostilePoint.getY() - point.getY(), 2) < this.AVOIDZONE;
	}
}
//***********************//
//Jonah Hooper - 728 237//
//***********************//
