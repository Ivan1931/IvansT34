package aiml;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


import robocode.AdvancedRobot;
import robocode.Rules;

//***********************//
//Jonah Hooper - 728 237//
//***********************//
public class BulletCircle {
	private Ellipse2D circ;
	private double velocity;
	private Point2D center;
	private long startTime;
	private double radius;
	private double mostLikelyAngleStart;
	private double mostLikelyAngleEnd;
	private double startingAngle;
	private double firedAngle;
	private final double angleOffset = Math.PI / 6;
	private NormalisedKNNValues values;
	public BulletCircle (long startTime, double initialPower, HostileRobot hostile, AdvancedRobot us) {
			velocity = getBulletVelocity (initialPower);
			Point2D ourPosition = new Point2D.Double(us.getX(), us.getY());
			center = hostile.getCurrentPosition(ourPosition , us.getHeadingRadians());
			Point2D upc = upperCorner (velocity * 2,center);
			radius = velocity;
			circ = new Ellipse2D.Double(upc.getX(), upc.getY(), velocity * 2, velocity * 2);
			this.startTime = startTime;
			values = new NormalisedKNNValues(us.getVelocity(), hostile.getCurrent().getDistance(),
					hostile.getCurrent().getBearing() + Math.PI, us.getEnergy());
			mostLikelyAngleEnd = 0.0;
			mostLikelyAngleStart = 0.0;
			this.startingAngle = Math.atan2(us.getY()- center.getY(), us.getX() - center.getX());
			//System.out.println("Starting angle: " + Math.toDegrees(startingAngle));
	}
	public void setMostSimilarBullets (ArrayList<BulletCircle> pastHits) {
		if (pastHits.size() > 3)
		{
			BulletCircle closest = pastHits.get(0);
			BulletCircle secondClosest = pastHits.get(1);
			//We  now iterate through all of the past hits to see which ones are the closest matching to our position
			//We find both of those values and get their relative angles. From there it is assumed that the 
			//Angle of firing is between those two angles so it is assumed
			//that the area between those points is a no go zone
			double firstDistance = closest.getEuclidianDistanceFromBullet(this);
			double secondDistance = secondClosest.getEuclidianDistanceFromBullet(this);
			for (int i = 2 ; i < pastHits.size(); i ++) {
				double tempDistance = pastHits.get(i).getEuclidianDistanceFromBullet(this);
				if (firstDistance <= tempDistance) {
					closest = pastHits.get(i);
					firstDistance = tempDistance;
				} else if (secondDistance < tempDistance) {
					secondClosest = pastHits.get(i);
					secondDistance = tempDistance;
				}
			}
			System.out.println("Closest fire angle: " + Math.toDegrees(closest.getFiredAngle()));
			System.out.println("SClosest fire angle: " + Math.toDegrees(secondClosest.getFiredAngle()));
			System.out.println("Starting angle: " + Math.toDegrees(startingAngle) + "\n");
			mostLikelyAngleStart = startingAngle + closest.getFiredAngle() + this.angleOffset;
			mostLikelyAngleEnd = startingAngle + secondClosest.getFiredAngle() - this.angleOffset;
			
		}
	}
	public void drawCircle (Graphics2D g2d, Color c) {
		g2d.setColor(c);
		g2d.draw(circ);
		g2d.setColor(Color.CYAN);
		Line2D lineAtPoint = new Line2D.Double(
				this.center.getX() + this.radius * Math.cos(this.mostLikelyAngleStart),
				this.center.getY() + this.radius * Math.sin(this.mostLikelyAngleStart),
				this.center.getX() + this.radius * Math.cos(this.mostLikelyAngleEnd),
				this.center.getY() + this.radius * Math.sin(this.mostLikelyAngleEnd));
		Arc2D arc = new Arc2D.Double(circ.getBounds2D(), -Math.toDegrees(this.mostLikelyAngleStart), 
				Math.toDegrees(this.mostLikelyAngleStart - this.mostLikelyAngleEnd), 0);
		g2d.draw(arc);
		g2d.draw(lineAtPoint);
	}
	public static double getBulletVelocity (double bulletPower) {
		return 20 - (bulletPower * 3.0);
	}
	public void growToTime (long time) {
		this.growToTicks((int)(time - startTime));
	}
	public void growToTicks (int ticks) {
		radius = ticks*velocity;
		Point2D upper = this.upperCorner(radius, center);
		circ.setFrameFromCenter( center, upper);
	}
	private Point2D upperCorner (double radius, Point2D pcenter) {
		return new Point2D.Double(pcenter.getX() + radius, pcenter.getY() + radius);
	}
	public boolean hasPassed (Point2D ourPosition) {
		ourPosition.setLocation(ourPosition.getX(), ourPosition.getY());
		return (circ.contains(ourPosition));
	}
	public Ellipse2D getCircle () {
		return circ;
	}
	public double getRadius () {
		return radius;
	}
	public void bulletHit (Point2D ourPositionAtHit) {
		this.firedAngle = Math.atan2(ourPositionAtHit.getY() - center.getY()
				, ourPositionAtHit.getX() - center.getX()) - this.startingAngle;
		System.out.println("Fire angle: " + Math.toDegrees(firedAngle));
	}
	public double getFiredAngle () {
		return firedAngle;
	}
	public NormalisedKNNValues getNormalValues( ){
		return values;
	}
	public double getEuclidianDistanceFromBullet (BulletCircle bc) {
		return values.getEuclidDistance(bc.getNormalValues());
	}
	/**
	 * This thing checks if a path of movement will intersect with a likely path of a bullet or a bullet region
	 * Not very accurate like most things in the bot
	 * @param a The initial position of our robot
	 * @param b The we're moving too
	 * @param velocity Our velocity at the initial time of movement
	 * @return True if the path will probably not hit a bullet
	 */
	public boolean isSafe (Point2D a, Point2D b, double velocity) {
		double distance = a.distance(b);
		double inc = distance / velocity;
		Rectangle2D tmp = new Rectangle2D.Double(a.getX(), a.getY(),IVANS_T34_JGH.robotSize,IVANS_T34_JGH.robotSize);
		double dx = Math.abs(a.getX() - b.getX()) / velocity;
		double dy = Math.abs(a.getY() - b.getY()) / velocity;
		double tempRad = radius;
		Ellipse2D ellipse = this.circ;
		Arc2D arc = new Arc2D.Double(ellipse.getBounds2D(), this.mostLikelyAngleStart,
				this.mostLikelyAngleStart - this.mostLikelyAngleEnd, 0);
		
		/*Line2D lineAtPoint = new Line2D.Double(
				this.center.getX() + tempRad * Math.cos(this.mostLikelyAngleStart),
				this.center.getY() + tempRad * Math.sin(this.mostLikelyAngleStart),
				this.center.getX() + tempRad * Math.cos(this.mostLikelyAngleEnd),
				this.center.getY() + tempRad * Math.sin(this.mostLikelyAngleEnd));*/
		for (double i = 0; i < distance; i+=inc) {
			//if (lineAtPoint.intersects(tmp))
				//return false;
			if (arc.intersects(tmp))
				return false;
			tmp.setRect(tmp.getX() + dx, tmp.getY() + dy, IVANS_T34_JGH.robotSize, IVANS_T34_JGH.robotSize);
			tempRad += velocity;
			/*lineAtPoint = new Line2D.Double(
					this.center.getX() + tempRad * Math.cos(this.mostLikelyAngleStart),
					this.center.getY() + tempRad * Math.sin(this.mostLikelyAngleStart),
					this.center.getX() + tempRad * Math.cos(this.mostLikelyAngleEnd),
					this.center.getY() + tempRad * Math.sin(this.mostLikelyAngleEnd));*/
			ellipse.setFrameFromCenter(center, this.upperCorner(tempRad, center));
			arc.setArc(ellipse.getBounds2D(), Math.toDegrees(this.mostLikelyAngleStart),
					 -Math.toDegrees(this.mostLikelyAngleStart - this.mostLikelyAngleEnd), 0);
		}
		return true;
	}
	public double getStartFiringAngle () {
		return startingAngle;
	}
}
class NormalisedKNNValues {
	public double speed;
	public double initialDistance;
	public double relativeAngle;
	public double energy;
	public NormalisedKNNValues (double speed, double initialDistance, double relativeAngle, double energy) {
		this.speed = speed / Rules.MAX_VELOCITY;
		this.initialDistance = initialDistance / 1000;
		this.relativeAngle = relativeAngle / 2 * Math.PI;
		this.energy = 100.0;
	}
	public double getEuclidDistance (NormalisedKNNValues comp) {
		return Math.sqrt(Math.pow(speed - comp.speed, 2) + 
				Math.pow(this.initialDistance, comp.initialDistance) +
				Math.pow(this.relativeAngle - comp.relativeAngle, 2) +
				Math.pow(this.energy - comp.energy, 2)
				);
	}
}
//***********************//
//Jonah Hooper - 728 237//
//***********************//