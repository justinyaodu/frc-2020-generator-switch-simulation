package simulation;

import geometry.Vector2D;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import physics.PointMass;

public class PointMassOnSwitch extends PointMass
{
	public final Vector2D SWITCH_RELATIVE_POSITION;
	private final DoubleProperty SWITCH_ANGLE;
	public final DoubleBinding TORQUE = MASS.multiply(POSITION.X).multiply(-1);

	public PointMassOnSwitch(double relativeX, double relativeY, double mass, DoubleProperty switchAngle)
	{
		// don't initialize position yet
		super(0, 0, mass);

		SWITCH_ANGLE = switchAngle;

		SWITCH_RELATIVE_POSITION = new Vector2D(relativeX, relativeY);

		POSITION.X.bind(new DoubleBinding()
		{
			{
				super.bind(SWITCH_RELATIVE_POSITION.X, SWITCH_RELATIVE_POSITION.Y, SWITCH_ANGLE);
			}

			@Override
			protected double computeValue()
			{
				return getAbsolutePosition().X.get();
			}
		});

		POSITION.Y.bind(new DoubleBinding()
		{
			{
				super.bind(SWITCH_RELATIVE_POSITION.X, SWITCH_RELATIVE_POSITION.Y, SWITCH_ANGLE);
			}

			@Override
			protected double computeValue()
			{
				return getAbsolutePosition().Y.get();
			}
		});

		// update absolute position by setting relative position
		SWITCH_RELATIVE_POSITION.X.set(relativeX);
		SWITCH_RELATIVE_POSITION.Y.set(relativeY);
	}

	private Vector2D getAbsolutePosition()
	{
		Vector2D position = Vector2D.rotate(SWITCH_RELATIVE_POSITION, SWITCH_ANGLE.doubleValue());
		position = Vector2D.add(position, new Vector2D(0, Constants.SWITCH_PIVOT_HEIGHT));
		return position;
	}
}