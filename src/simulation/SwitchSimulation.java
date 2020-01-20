package simulation;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class SwitchSimulation
{
	public final DoubleProperty SWITCH_ANGLE = new SimpleDoubleProperty(0);
	public final DoubleProperty SOLVE_PRECISION = new SimpleDoubleProperty(1e-6);
	public final ObservableList<Double> TOLERANCES_MINUS = new ObservableListWrapper<>(new ArrayList<>());
	public final ObservableList<Double> TOLERANCES_PLUS = new ObservableListWrapper<>(new ArrayList<>());
	public DoubleBinding NET_TORQUE;
	public DoubleBinding COM_X = new SimpleDoubleProperty(0).add(0);
	public DoubleBinding COM_Y = new SimpleDoubleProperty(0).add(0);
	private DoubleBinding TOTAL_MASS = new SimpleDoubleProperty(0).add(0);

	public final BooleanBinding IS_LEVEL = new BooleanBinding()
	{
		{
			super.bind(SWITCH_ANGLE);
		}

		@Override
		protected boolean computeValue()
		{
			return Math.abs(SWITCH_ANGLE.getValue()) <= Constants.SWITCH_BALANCED_ANGLE;
		}
	};

	public final List<PointMassOnSwitch> MASSES;

	// indicator variable to prevent erroneous listener firing when properties are changed during threshold calculation
	private boolean temporarilyDisableListeners = false;

	public SwitchSimulation()
	{
		MASSES = new ArrayList<>();
		PointMassOnSwitch switchMass = new PointMassOnSwitch(0, -Constants.SWITCH_COM_PIVOT_DISTANCE, Constants.SWITCH_WEIGHT, SWITCH_ANGLE);
		addMass(switchMass);

		NET_TORQUE = switchMass.TORQUE;

		for (int i = 0; i < 3; i++)
		{
			PointMassOnSwitch robot = new PointMassOnSwitch(0, -Constants.SWITCH_RUNG_PIVOT_DISTANCE, 0, SWITCH_ANGLE);
			addMass(robot);

			// recalculate state whenever robot position or mass are changed
			robot.SWITCH_RELATIVE_POSITION.X.addListener(observable -> recalculate());
			robot.SWITCH_RELATIVE_POSITION.Y.addListener(observable -> recalculate());
			robot.MASS.addListener(observable -> recalculate());

			NET_TORQUE = NET_TORQUE.add(robot.TORQUE);
		}

		COM_X = COM_X.divide(TOTAL_MASS);
		COM_Y = COM_Y.divide(TOTAL_MASS);
	}

	private void addMass(PointMassOnSwitch pointMass)
	{
		MASSES.add(pointMass);
		COM_X = COM_X.add(pointMass.MASS.multiply(pointMass.POSITION.X));
		COM_Y = COM_Y.add(pointMass.MASS.multiply(pointMass.POSITION.Y));
		TOTAL_MASS = TOTAL_MASS.add(pointMass.MASS);
	}

	private void findEquilibrium()
	{
		double angle = Math.atan2(COM_X.get(), COM_Y.get());
		angle = Math.max(-Constants.SWITCH_MAX_ANGLE, Math.min(Constants.SWITCH_MAX_ANGLE, angle));
		SWITCH_ANGLE.set(angle);
	}

	private void recalculate()
	{
		// prevent this method from recursively calling itself through listener events
		if (!temporarilyDisableListeners)
		{
			temporarilyDisableListeners = true;
		}
		else
		{
			return;
		}

		// clear previously calculated values
		TOLERANCES_MINUS.clear();
		TOLERANCES_PLUS.clear();

		findEquilibrium();

		// don't compute tolerances if the switch isn't level to begin with
		if (!IS_LEVEL.get())
		{
			temporarilyDisableListeners = false;
			return;
		}

		for (int i = 1; i < MASSES.size(); i++)
		{
			PointMassOnSwitch pointMass = MASSES.get(i);

			double original = pointMass.SWITCH_RELATIVE_POSITION.X.get();

			// find minimum
			double min = -Constants.SWITCH_HANDLE_LENGTH / 2;
			double max = original;

			while (max - min > SOLVE_PRECISION.get())
			{
				pointMass.SWITCH_RELATIVE_POSITION.X.set((min + max) / 2);
				findEquilibrium();

				if (IS_LEVEL.get())
				{
					max = pointMass.SWITCH_RELATIVE_POSITION.X.get();
				}
				else
				{
					min = pointMass.SWITCH_RELATIVE_POSITION.X.get();
				}
			}

			TOLERANCES_MINUS.add(original - pointMass.SWITCH_RELATIVE_POSITION.X.get());

			// find maximum
			min = original;
			max = Constants.SWITCH_HANDLE_LENGTH / 2;

			while (max - min > SOLVE_PRECISION.get())
			{
				pointMass.SWITCH_RELATIVE_POSITION.X.set((min + max) / 2);
				findEquilibrium();

				if (IS_LEVEL.get())
				{
					min = pointMass.SWITCH_RELATIVE_POSITION.X.get();
				}
				else
				{
					max = pointMass.SWITCH_RELATIVE_POSITION.X.get();
				}
			}

			TOLERANCES_PLUS.add(pointMass.SWITCH_RELATIVE_POSITION.X.get() - original);

			// restore original value
			pointMass.SWITCH_RELATIVE_POSITION.X.set(original);
		}

		// final reset
		findEquilibrium();

		temporarilyDisableListeners = false;
	}
}