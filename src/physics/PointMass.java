package physics;

import geometry.Vector2D;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class PointMass
{
    private DoubleProperty mass;
    private Vector2D position;

    public PointMass(double x, double y, double mass)
    {
        position = new Vector2D(x, y);
        this.mass = new SimpleDoubleProperty(mass);
    }

    public Vector2D getPosition()
    {
        return position;
    }

    public DoubleProperty massProperty()
    {
        return mass;
    }
}