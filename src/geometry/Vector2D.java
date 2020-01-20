package geometry;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class Vector2D
{
	public final DoubleProperty X;
	public final DoubleProperty Y;

	public Vector2D(double x, double y)
	{
		this.X = new SimpleDoubleProperty(x);
		this.Y = new SimpleDoubleProperty(y);
	}

	public static Vector2D add(Vector2D a, Vector2D b)
	{
		return new Vector2D(a.X.get() + b.X.get(), a.Y.get() + b.Y.get());
	}

	public static Vector2D rotate(Vector2D a, double theta)
	{
		double xPrime = a.X.get() * Math.cos(theta) + a.Y.get() * Math.sin(theta);
		double yPrime = -a.X.get() * Math.sin(theta) + a.Y.get() * Math.cos(theta);
		return new Vector2D(xPrime, yPrime);
	}

	public static Vector2D scale(Vector2D a, double scalar)
	{
		return new Vector2D(a.X.get() * scalar, a.Y.get() * scalar);
	}

	public static double magnitude(Vector2D a)
	{
		return Math.sqrt(a.X.get() * a.X.get() + a.Y.get() * a.Y.get());
	}
}