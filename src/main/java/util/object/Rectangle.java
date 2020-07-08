package util.object;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.function.DistanceFunction;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2D rectangle defined by its bottom-left and upper-right coordinates, and whose edges are parallel to the X and Y axis.
 * <p>
 * Rectangle objects may contain both spatial and semantic attributes. Spatial attributes of simple objects, however, are immutable, that
 * means once a Rectangle object is created its spatial attributes cannot be changed.
 *
 * @author uqdalves, Hellisk
 */
public class Rectangle {
	
	private static final Logger LOG = LogManager.getLogger(Rectangle.class);
	
	// X and Y axis position
	/**
	 * lower-left corner x
	 **/
	private final double minX;
	/**
	 * lower-left corner y
	 **/
	private final double minY;
	/**
	 * upper-right corner x
	 **/
	private final double maxX;
	/**
	 * upper-right corner y
	 **/
	private final double maxY;
	
	private final DistanceFunction distFunc;
	
	/**
	 * Creates a new empty rectangle.
	 */
	public Rectangle(DistanceFunction df) {
		this.minX = 0.0;
		this.minY = 0.0;
		this.maxX = 0.0;
		this.maxY = 0.0;
		this.distFunc = df;
	}
	
	/**
	 * Create a new rectangle with the given coordinates.
	 *
	 * @param minX Lower-left corner X.
	 * @param minY Lower-left corner Y.
	 * @param maxX Upper-right corner X.
	 * @param maxY Upper-right corner Y.
	 */
	public Rectangle(double minX, double minY, double maxX, double maxY, DistanceFunction df) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.distFunc = df;
	}
	
	/**
	 * @return Lower-left corner X coordinate.
	 */
	public double minX() {
		return minX;
	}
	
	/**
	 * @return Lower-left corner Y coordinate.
	 */
	public double minY() {
		return minY;
	}
	
	/**
	 * @return Upper-right corner X coordinate.
	 */
	public double maxX() {
		return maxX;
	}
	
	/**
	 * @return Upper-right corner Y coordinate.
	 */
	public double maxY() {
		return maxY;
	}
	
	public List<Point> getCoordinates() {
		List<Point> corners = new ArrayList<>(4);
		Point p1 = new Point(minX, minY, distFunc);
		Point p2 = new Point(minX, maxY, distFunc);
		Point p3 = new Point(maxX, maxY, distFunc);
		Point p4 = new Point(maxX, minY, distFunc);
		corners.add(p1);
		corners.add(p2);
		corners.add(p3);
		corners.add(p4);
		
		return corners;
	}
	
	public List<Segment> getEdges() {
		List<Segment> edges = new ArrayList<>(4);
		edges.add(leftEdge());
		edges.add(upperEdge());
		edges.add(rightEdge());
		edges.add(lowerEdge());
		
		return edges;
	}
	
	public DistanceFunction getDistanceFunction() {
		return this.distFunc;
	}
	
	public boolean isClosed() {
		return true;
	}
	
	/**
	 * @return The perimeter of this rectangle.
	 */
	public double perimeter() {
		return (2 * Math.abs(maxY - minY) + 2 * Math.abs(maxX - minX));
	}
	
	/**
	 * @return The area of this rectangle.
	 */
	public double area() {
		return (maxY - minY) * (maxX - minX);
	}
	
	/**
	 * @return The height of this rectangle.
	 */
	public double height() {
		return Math.abs(maxY - minY);
	}
	
	/**
	 * @return The width of this rectangle.
	 */
	public double width() {
		return Math.abs(maxX - minX);
	}
	
	/**
	 * @return The center of this rectangle as a coordinate point.
	 */
	public Point center() {
		double xCenter = minX + (maxX - minX) / 2;
		double yCenter = minY + (maxY - minY) / 2;
		return new Point(xCenter, yCenter, distFunc);
	}
	
	/**
	 * @return The left edge of this rectangle.
	 */
	public Segment leftEdge() {
		return new Segment(minX, minY, minX, maxY, distFunc);
	}
	
	/**
	 * @return The right edge of this rectangle.
	 */
	public Segment rightEdge() {
		return new Segment(maxX, minY, maxX, maxY, distFunc);
	}
	
	/**
	 * @return The bottom edge of this rectangle.
	 */
	public Segment lowerEdge() {
		return new Segment(minX, minY, maxX, minY, distFunc);
	}
	
	/**
	 * @return The top edge of this rectangle.
	 */
	public Segment upperEdge() {
		return new Segment(minX, maxY, maxX, maxY, distFunc);
	}
	
	/**
	 * Check whether this rectangle is a square, i.e. if height equal width.
	 *
	 * @return True is this rectangle is a square.
	 */
	public boolean isSquare() {
		return (Double.doubleToLongBits(height()) == Double.doubleToLongBits(width()));
	}
	
	/**
	 * Check whether these two rectangles are adjacent, i.e. If they share any edge.
	 *
	 * @param r The rectangle to check.
	 * @return True is these two rectangles are adjacent.
	 */
	public boolean isAdjacent(Rectangle r) {
		if (r == null) return false;
		for (Segment e1 : this.getEdges()) {
			for (Segment e2 : r.getEdges()) {
				if (e1.equals2D(e2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public Rectangle extendByDist(double distance) {
		double currMinX = minX - distFunc.getCoordinateOffsetX(distance, (minY + maxY) / 2);
		double currMaxX = maxX + distFunc.getCoordinateOffsetX(distance, (minY + maxY) / 2);
		double currMinY = minY - distFunc.getCoordinateOffsetY(distance, (minX + maxX) / 2);
		double currMaxY = maxY + distFunc.getCoordinateOffsetY(distance, (minX + maxX) / 2);
		return new Rectangle(currMinX, currMinY, currMaxX, currMaxY, distFunc);
	}
	
	/**
	 * Check whether this rectangle contains the given point p = (x,y) inside its perimeter.
	 *
	 * @param x The x axis of the point
	 * @param y The y axis of the point
	 * @return True if the point p = (x,y) lies inside the rectangle's perimeter.
	 */
	public boolean contains(double x, double y) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}
	
	/**
	 * Check whether these two rectangles overlap.
	 *
	 * @param r The other rectangle to check.
	 * @return True if these two rectangles overlap.
	 */
	public boolean overlaps(Rectangle r) {
		if (r == null) return false;
		if (this.maxX < r.minX) return false;
		if (this.minX > r.maxX) return false;
		if (this.maxY < r.minY) return false;
		return !(this.minY > r.maxY);
	}
	
	/**
	 * Convert this point object to a AWT Rectangle2D object.
	 *
	 * @return The Rectangle2D representation of this rectangle.
	 */
	public Rectangle2D toRectangle2D() {
		return new Rectangle2D.Double(minX, maxY, width(), height());
	}
	
	@Override
	public String toString() {
		String s = "( ";
		s += minX + " " + minY + ", ";
		s += minX + " " + maxY + ", ";
		s += maxX + " " + maxY + ", ";
		s += maxX + " " + minY + " )";
		return s;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(maxX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	public boolean equals2D(Rectangle obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		return (obj.minX == minX && obj.minY == minY && obj.maxX == maxX && obj.maxY == obj.maxY);
	}
	
	@Override
	public Rectangle clone() {
		Rectangle clone = new Rectangle(minX, minY, maxX, maxY, distFunc);
		return clone;
	}
}
