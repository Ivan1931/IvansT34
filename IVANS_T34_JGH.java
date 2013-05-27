package aiml;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
//***********************//
// Jonah Hooper - 728 237//
//***********************//
public class IVANS_T34_JGH extends AdvancedRobot {
	private boolean radarLock = false;
	private final double GUN_STRENGTH_DIVIDER = 1000;
	private long lastScan = 0;
	private final long MAX_BETWEEN_SCANS = 2;
	private HostileRobot hostile;
	Point2D future;
	private double previousPower;
	public static final double robotSize = 36.0;
	private Line2D moveLine;
	//private static int bulletsFired;
	//Defined as static so that is stays around for a round
	private static ArrayList<BulletCircle> shotHit = new ArrayList<BulletCircle> (); 
	private ArrayList<BulletCircle> hostileShots = new ArrayList<BulletCircle>();
	private ArrayList<BulletCircle> shots = new ArrayList<BulletCircle>();
	private boolean enemyDisabled = false;
	public void run ()
	{
		this.setAdjustGunForRobotTurn(true);
		this.setAdjustRadarForGunTurn(true);
		this.setAdjustRadarForRobotTurn(true);
		this.setColors(Color.gray, Color.black, Color.red);
		this.setBulletColor(Color.red);
		while (true)
		{
			try {
				if (!enemyDisabled){
					if (hostileShots.size() > 0)
						updateHostileShots ();
					if(this.getRadarTurnRemainingRadians() == 0 || this.getVelocity() == 0)
						move ();
					//
						//moveToPoint (this.getBattleFieldWidth() / 2, this.getBattleFieldHeight() / 2);
					if (!radarLock || (this.getTime() - lastScan) > MAX_BETWEEN_SCANS)
						this.setTurnRadarLeftRadians(java.lang.Double.POSITIVE_INFINITY);
				} else {
					this.setColors(Color.RED, Color.RED, Color.RED);
					this.moveToPoint(hostile.getCurrentPosition(getLocation(), this.getHeadingRadians()));
				}
				this.execute();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
		
	}
	public void onPaint (Graphics2D g) {
		if (future != null)
			drawLine(getLocation(),future,g, Color.GREEN);
		for (int i = 0 ; i < hostileShots.size(); i++)
			hostileShots.get(i).drawCircle(g,Color.red);
		for (int i = 0 ; i < shots.size(); i++) 
			shots.get(i).drawCircle(g, Color.blue);
		g.setColor(Color.magenta);
		if (moveLine!=null)
			g.draw(moveLine);
	}
	public void drawLine (Point2D a, Point2D b, Graphics2D g2d, Color c) {
		g2d.setColor(c);
		g2d.drawLine((int)a.getX(), (int)a.getY(), (int)b.getX(), (int)b.getY());
	}
	public void onScannedRobot (ScannedRobotEvent e)
	{
		//System.out.println(getHostilePosition(e).toString());
		if (hostile == null)
			hostile = new HostileRobot(e);
		else
			hostile.update(e);
		if (e.getEnergy() > 0) {
			if (hostile.hasFired())  {
				hostileShotFired(hostile);
			}
			updateShots();
			radarLock = true;
			//this.setTurnRadarRight(0);
			lastScan = this.getTime();// Rembers last scan, after set time of not finding enemy, rescan
			//This chuck over here calculates the future linear position of the hostile bot
			Point2D pf = getLinearInterceptWithRobot(previousPower, hostile);
			double aimX = pf.getX() - getX();
			double aimY = pf.getY() - getY();
			double turnAngle = Math.atan2(aimX, aimY);
			double trng = IVANS_T34_JGH.normaliseAngleRadians(turnAngle - this.getGunHeadingRadians());
			//
			//System.out.println("TRNG: " + trng * 180 / Math.PI );
			double trnr = IVANS_T34_JGH.normaliseAngleRadians(getHeadingRadians() - getRadarHeadingRadians() + e.getBearingRadians());
			setTurnRadarRightRadians(trnr);
			setTurnGunRightRadians(trng);
			Point2D futureHostileInOneTick = this.getFutureHostilePosition(e, 1);
			double distanceAwayInOneTick = Math.abs(e.getDistance() - futureHostileInOneTick.distance(getLocation()));
			if (Math.abs(this.getGunTurnRemaining()) < 5 && this.getEnergy() > 5 && distanceAwayInOneTick < 6.0)
			{
				previousPower = getGunPower(e);
				//shotFired(previousPower);
				setFire(previousPower);
				//this.execute();
			}
			
			//System.out.println(pf.toString());
			if (pf!= null);
				future = (Point2D) pf.clone();
			/*if (future != null)  
				System.out.println(future.toString());*/
		} else this.enemyDisabled = true;
		
	}
	// Gets the absolute bearing between two points
	//***********************//
	// Jonah Hooper - 728 237//
	//***********************//
	double getAbsoluteBearing(Point2D a, Point2D b) {
		double xa = a.getX() - b.getX();
		double ya = a.getY() - b.getY();
		double hyp = a.distance(b);
		double arcSin = Math.asin(xa / hyp);
		double bearing = 0;

		if (xa > 0 && ya > 0)
			bearing = arcSin;
		if (xa < 0 && ya > 0)  
			bearing = 360 + arcSin;
		else if (xa > 0 && ya < 0) 
			bearing = 180 - arcSin;
		else 
			bearing = 180 - arcSin; 

		return bearing;
	}
	//This part took a while
	public Point2D getLinearInterceptWithRobot(double power, HostileRobot h) {
		int ticks = 1;
		double vel = BulletCircle.getBulletVelocity(power);
		Point2D current = getLocation();
		Point2D fh = this.getFutureHostilePosition(h.getCurrent(), ticks);
		while (fh.distance(current) > (ticks-1) * vel) {
			//System.out.println("Vel: " + vel);
			fh = this.getFutureHostilePosition(h.getCurrent(), ticks);
			ticks++;
			if (!(fh.getX() < this.getBattleFieldWidth() && fh.getX() > 0
					&& fh.getY() < this.getBattleFieldHeight() && fh.getY() > 0))
				break;
			if (ticks > 2000) {
				//System.out.println("Not Found");
				break;
			}
		}
		//System.out.println("FH: " + fh);
		return fh;
	}
	//The tenery operator makes simple code way more awesome, dont you think?
	public double getQuadraticRoot (double a, double b, double c, boolean negative) {
		return (-b + (negative ? -1 : 1) * Math.sqrt(Math.pow(b,2) - 4 * a * c))/(2 * a);
	}
	public double getYFromEquation (double mval, double xval, double cval) {
		return mval*xval + cval;
	}
	@Override
	public void fire(double power) {
		shotFired(power);
		super.fire(power);
	}
	//Must consider its own life, enemy life and most importantly, distance
	//Tenery operator...
	public double getGunPower (ScannedRobotEvent e)
	{
		double pow = (1.0 - e.getDistance() / this.GUN_STRENGTH_DIVIDER) * Rules.MAX_BULLET_POWER;
		if (e.getEnergy() > 10)
			return  pow < Rules.MAX_BULLET_POWER ? pow : Rules.MAX_BULLET_POWER;
		else return 1;
	}
	public double getBulletVelocity (double power)
	{
		return 20.0 - 3.0 * power;
	}
	public Point2D getHostilePosition (ScannedRobotEvent e) {
		double absangle = e.getBearingRadians() + getHeadingRadians();
		
		return new Point2D.Double(e.getDistance() * Math.sin(absangle) + this.getX()
				, e.getDistance() * Math.cos(absangle) + this.getY());
	}
	public Point2D getFutureHostilePosition (ScannedRobotEvent e, int ticks) {
		//fy = currenty + rsin(theta)
		//fx = currentx + rcos(theta)
		double hostileMove = e.getVelocity() * ticks;
		double hostileHeading  = e.getHeadingRadians();
		Point2D current = getHostilePosition(e);
		
		return new Point2D.Double(current.getX() + 
				hostileMove * Math.sin(hostileHeading)
				,current.getY() + hostileMove * Math.cos(hostileHeading));
	}
	//After and excessive ammount of testing, I discovered that the gun was being aimed in an inefficient manner when angles where close to 360 or negative
	//This normalises the angle to a small one between  -PI and PI as those are the turning Ls of a robocode gun
	public static double normaliseAngleRadians (double angle){
		while (angle > Math.PI) angle -= 2 * Math.PI;
		while (angle < -Math.PI) angle += 2 * Math.PI;
		return angle;
	}
	public void onHitWall (HitWallEvent e) {
		//this.back(100);
	}
	//@SuppressWarnings("static-access")
	public void moveToPoint (Point2D point) {
		setTurnRightRadians( normalAngle(absoluteBearing(getLocation(), point) - getHeadingRadians()) );
        setAhead(getLocation().distance(point));
	}
	public void move () {
		Point2D p = null;
		if (this.getShotHit().size() > 4) {
			Point2D cur = getLocation ();
			double vel = this.getVelocity();
			int count = 0;
			boolean found = false;
			while (!found && count < 40) {
				found = true;
				
				p = new Point2D.Double(IVANS_T34_JGH.randBetween(50, this.getBattleFieldWidth() - 50),
						IVANS_T34_JGH.randBetween(50, this.getBattleFieldHeight() - 50));
				
				if (this.hostile != null) {
					if (!this.hostile.isInAvoidZone(p, getLocation(), this.getHeadingRadians())){
						for (int i = 0 ; i < hostileShots.size(); i ++ ) {
							found =  hostileShots.get(i).isSafe(cur, p, vel);
							if (!found) break;
						}
					} else System.out.println("Is not safe");
				}
				count++;
			}
			System.out.println(count < 40 ? "Found" : "Not Found");
		} else {
			p = new Point2D.Double(randBetween (50,this.getBattleFieldWidth() - 50), 
				randBetween(50,this.getBattleFieldHeight() - 50));
		}
		System.out.println("Setting");
		this.moveLine = new Line2D.Double(getLocation(), p);
		this.moveToPoint(p);
		
	}
	private double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() -
            source.getX(), target.getY() - source.getY());
    }

    private double normalAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private Point2D getLocation() {
        return new Point2D.Double(getX(), getY());
    }
	public static double randBetween (double a, double b)
	{
		return a + Math.random() * (Math.abs(b - a)) ;
	}
	private void shotFired (double energy) {
		//BulletCircle bc = new BulletCircle (getTime(),energy,getLocation());
		//shots.add(bc);
		System.out.println("Fired!");
	}
	private void hostileShotFired (HostileRobot h) {
		BulletCircle bc = new BulletCircle (getTime() - 1,h.hostileShotDetected(), h,this);
		if (getShotHit().size() > 4)
			bc.setMostSimilarBullets(getShotHit());
		hostileShots.add(bc);
		
		//System.out.println("Shot fired");
	}
	private void updateShots (){
		for (int i = 0; i < shots.size(); i++) {
			shots.get(i).growToTime(getTime());
			if (shots.get(i).hasPassed(
				getHostilePosition(hostile.getCurrent()))) 
					shots.remove(i);
			
		}
	}
	private void updateHostileShots () {
		for (int i = 0 ; i < hostileShots.size(); i++) {
			hostileShots.get(i).growToTime(getTime());
			if (hostileShots.get(i).hasPassed(getLocation()))
				hostileShots.remove(i);
		}
		
	}
	//@SuppressWarnings("static-access")
	public void onHitByBullet(HitByBulletEvent hb) {
		this.hostileShots.get(0).bulletHit(getLocation());
		if (hostileShots.size() > 0) {
			this.getShotHit().add(this.hostileShots.get(0));
			hostileShots.remove(0);
		}
		System.out.println("Hit by bullet\nSize: " + getShotHit().size());
	}
	public static ArrayList<BulletCircle> getShotHit() {
		return shotHit;
	}
	public static void setShotHit(ArrayList<BulletCircle> shotHit) {
		IVANS_T34_JGH.shotHit = shotHit;
	}
	//***********************//
	// Jonah Hooper - 728 237//
	//***********************//
}
