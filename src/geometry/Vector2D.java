package geometry;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Vector2D
{
    private DoubleProperty x;
	private DoubleProperty y;

	public Vector2D(double x, double y)
	{
		this.x = new SimpleDoubleProperty(x);
		this.y = new SimpleDoubleProperty(y);
	}

	public static Vector2D add(Vector2D a, Vector2D b)
	{
		return new Vector2D(a.x.get() + b.x.get(), a.y.get() + b.y.get());
	}

	// rotate vector counterclockwise by theta radians
	public static Vector2D rotate(Vector2D a, double theta)
	{
		double xPrime = a.x.get() * Math.cos(theta) - a.y.get() * Math.sin(theta);
		double yPrime = a.x.get() * Math.sin(theta) + a.y.get() * Math.cos(theta);
		return new Vector2D(xPrime, yPrime);
	}

    public DoubleProperty xProperty() {
        return x;
    }

    public DoubleProperty yProperty() {
        return y;
    }
}