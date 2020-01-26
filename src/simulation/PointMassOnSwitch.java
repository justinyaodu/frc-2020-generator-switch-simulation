package simulation;

import geometry.Vector2D;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import physics.PointMass;

public class PointMassOnSwitch extends PointMass
{
    private Vector2D switchRelativePosition;
    private ObservableDoubleValue switchAngleReference;
    private DoubleBinding torque = massProperty().multiply(getPosition().xProperty()).multiply(-1);

    public PointMassOnSwitch(double relativeX, double relativeY, double mass, ObservableDoubleValue switchAngleReference)
    {
        // don't initialize position yet
        super(0, 0, mass);

        this.switchAngleReference = switchAngleReference;

        switchRelativePosition = new Vector2D(relativeX, relativeY);

        getPosition().xProperty().bind(new DoubleBinding()
        {
            {
                super.bind(switchRelativePosition.xProperty(), switchRelativePosition.yProperty(), switchAngleReference);
            }

            @Override
            protected double computeValue()
            {
                return getAbsolutePosition().xProperty().get();
            }
        });

        getPosition().yProperty().bind(new DoubleBinding()
        {
            {
                super.bind(switchRelativePosition.xProperty(), switchRelativePosition.yProperty(), switchAngleReference);
            }

            @Override
            protected double computeValue()
            {
                return getAbsolutePosition().yProperty().get();
            }
        });

        // update absolute position by setting relative position
        switchRelativePosition.xProperty().set(relativeX);
        switchRelativePosition.yProperty().set(relativeY);
    }

    public Vector2D getSwitchRelativePosition()
    {
        return switchRelativePosition;
    }

    private Vector2D getAbsolutePosition()
    {
        Vector2D position = Vector2D.rotate(switchRelativePosition, switchAngleReference.get());
        position = Vector2D.add(position, new Vector2D(0, Constants.SWITCH_PIVOT_HEIGHT));
        return position;
    }

    public DoubleBinding torqueProperty()
    {
        return torque;
    }
}