package util.object;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a simple 2D point entity, with (x,y) coordinates.
 * <p>
 * Point objects may contain both spatial and semantic attributes. Spatial attributes of simple objects, however, are immutable, that
 * means once a Point object is created its spatial attributes cannot be changed.
 *
 * @author uqdalves, Hellisk
 */
public class Point {
	
	private static final Logger LOG = Logger.getLogger(Point.class);
	
	private String id;
	/**
	 * Point coordinates
	 */
	private double x;
	private double y;
	private DistanceFunction distFunc;
	
	/**
	 * Creates an empty Point with default (0,0) coordinates.
	 */
	public Point(DistanceFunction df) {
		this.x = 0.0;
		this.y = 0.0;
		this.distFunc = df;
	}
	
	/**
	 * Creates a 2D point with the given coordinates.
	 *
	 * @param x Point X/Longitude coordinate.
	 * @param y Point Y/Latitude coordinate.
	 */
	public Point(double x, double y, DistanceFunction df) {
		this.x = x;
		this.y = y;
		this.distFunc = df;
	}
	
	/**
	 * @return Point X coordinate.
	 */
	public double x() {
		return x;
	}
	
	/**
	 * @return Point Y coordinate.
	 */
	public double y() {
		return y;
	}
	
	/**
	 * Reset the point and set kn kn new
	 *
	 * @param x  Nex X coordinate.
	 * @param y  New Y coordinate.
	 * @param df New distance function.
	 */
	public void setPoint(double x, double y, DistanceFunction df) {
		this.x = x;
		this.y = y;
		this.distFunc = df;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return Array containing the [x,y] coordinates of this point.
	 */
	public double[] getCoordinate() {
		return new double[]{x, y};
	}
	
	public List<Point> getCoordinates() {
		ArrayList<Point> list = new ArrayList<>(1);
		list.add(this);
		return list;
	}
	
	public List<Segment> getEdges() {
		return new ArrayList<>(0);
	}
	
	public DistanceFunction getDistanceFunction() {
		return this.distFunc;
	}
	
	public boolean isClosed() {
		return false;
	}
	
	@Override
	public Point clone() {
		Point clone = new Point(x, y, distFunc);
		clone.setId(getId());
		return clone;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	public boolean equals2D(Point obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		return obj.x == x && obj.y == y;
	}
	
	/**
	 * Convert this point object to a AWT Point2D object.
	 *
	 * @return The Point2D representation of this point.
	 */
	public Point2D toPoint2D() {
		return new Point2D.Double(x, y);
	}
	
	@Override
	public String toString() {
		return String.format("%.5f %.5f", x, y);
	}
}
