package simulation;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class SwitchSimulation
{
    private DoubleProperty solvePrecision = new SimpleDoubleProperty(1e-6);
    private DoubleBinding comX = new SimpleDoubleProperty(0).add(0);
    private DoubleBinding comY = new SimpleDoubleProperty(0).add(0);
    private DoubleBinding totalMass = new SimpleDoubleProperty(0).add(0);
    private DoubleProperty equilibriumAngle = new SimpleDoubleProperty(0);
    private BooleanBinding isLevel;
    private List<DoubleBinding> levelXMin = new ArrayList<>();
    private List<DoubleBinding> levelXZero = new ArrayList<>();
    private List<DoubleBinding> levelXMax = new ArrayList<>();

    private List<PointMassOnSwitch> pointMasses = new ArrayList<>();

    // indicator variable to prevent erroneous listener firing when properties are changed during threshold calculation
    private boolean temporarilyDisableListeners = false;

    public SwitchSimulation()
    {
        PointMassOnSwitch switchMass = new PointMassOnSwitch(0, -Constants.SWITCH_COM_PIVOT_DISTANCE, Constants.SWITCH_WEIGHT, equilibriumAngle);
        pointMasses.add(switchMass);

        for (int i = 0; i < 3; i++)
        {
            PointMassOnSwitch robot = new PointMassOnSwitch(0, -Constants.SWITCH_RUNG_PIVOT_DISTANCE, 0, equilibriumAngle);
            pointMasses.add(robot);
        }

        // create bindings for COM
        for (PointMassOnSwitch pointMass : pointMasses)
        {
            totalMass = totalMass.add(pointMass.massProperty());
            comX = comX.add(pointMass.massProperty().multiply(pointMass.getSwitchRelativePosition().xProperty()));
            comY = comY.add(pointMass.massProperty().multiply(pointMass.getSwitchRelativePosition().yProperty()));
        }
        comX = comX.divide(totalMass);
        comY = comY.divide(totalMass);

        equilibriumAngle.bind(new DoubleBinding()
        {
            {
                super.bind(comX, comY);
            }

            @Override
            protected double computeValue()
            {
                double angle = Math.atan(comX.get() / comY.get());
                angle = Math.max(-Constants.SWITCH_MAX_ANGLE, Math.min(Constants.SWITCH_MAX_ANGLE, angle));
                return angle;
            }
        });

        isLevel = new BooleanBinding()
        {
            {
                super.bind(equilibriumAngle);
            }

            @Override
            protected boolean computeValue()
            {
                return Math.abs(equilibriumAngle.getValue()) <= Constants.SWITCH_LEVEL_THRESHOLD;
            }
        };

        for (int i = 0; i < pointMasses.size(); i++)
        {
            levelXMin.add(findMassPositionByAngle(i, Constants.SWITCH_LEVEL_THRESHOLD));
            levelXZero.add(findMassPositionByAngle(i, 0));
            levelXMax.add(findMassPositionByAngle(i, -Constants.SWITCH_LEVEL_THRESHOLD));
        }
    }

    private DoubleBinding findMassPositionByAngle(int index, double angle)
    {
        DoubleBinding numeratorLeft = new SimpleDoubleProperty(0).add(0);
        for (PointMassOnSwitch pointMass : pointMasses)
        {
            numeratorLeft = numeratorLeft.add(pointMass.massProperty()
                    .multiply(pointMass.getSwitchRelativePosition().yProperty()));
        }
        numeratorLeft = numeratorLeft.multiply(Math.tan(angle));

        DoubleBinding numeratorRight = new SimpleDoubleProperty(0).add(0);
        for (int i = 0; i < pointMasses.size(); i++)
        {
            // skip the object we are trying to solve for
            if (i == index)
            {
                continue;
            }

            PointMassOnSwitch pointMass = pointMasses.get(i);

            numeratorRight = numeratorRight.add(pointMass.massProperty()
                    .multiply(pointMass.getSwitchRelativePosition().xProperty()));
        }

        DoubleBinding theoretical = numeratorLeft.subtract(numeratorRight).divide(pointMasses.get(index).massProperty());

        return new DoubleBinding()
        {
            {
                super.bind(theoretical);
            }

            @Override
            protected double computeValue()
            {
                // return NaN if off the handle or infinite
                if (!Double.isFinite(theoretical.get())
                        || Math.abs(theoretical.get()) > Constants.SWITCH_HANDLE_LENGTH / 2)
                {
                    return Double.NaN;
                }

                return theoretical.get();
            }
        };
    }

    public List<PointMassOnSwitch> getPointMasses()
    {
        return pointMasses;
    }

    public List<DoubleBinding> getLevelXMin()
    {
        return levelXMin;
    }

    public List<DoubleBinding> getLevelXZero()
    {
        return levelXZero;
    }

    public List<DoubleBinding> getLevelXMax()
    {
        return levelXMax;
    }

    public DoubleBinding comXProperty()
    {
        return comX;
    }

    public DoubleBinding comYProperty()
    {
        return comY;
    }

    public DoubleProperty equilibriumAngleProperty()
    {
        return equilibriumAngle;
    }

    public BooleanBinding isLevelProperty()
    {
        return isLevel;
    }
}