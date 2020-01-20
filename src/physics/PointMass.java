package physics;

import geometry.Vector2D;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class PointMass
{
	public final DoubleProperty MASS;
	public final Vector2D POSITION;

	public PointMass(double x, double y, double mass)
	{
		POSITION = new Vector2D(x, y);
		MASS = new SimpleDoubleProperty(mass);
	}
}