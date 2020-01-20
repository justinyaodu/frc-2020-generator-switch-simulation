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

	public final DoubleBinding THEORETICAL_ANGLE;

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
		MASSES.add(switchMass);

		NET_TORQUE = switchMass.TORQUE;

		DoubleBinding numerator = new SimpleDoubleProperty(0).add(0);
		DoubleBinding denominator = switchMass.MASS.multiply(Constants.SWITCH_COM_PIVOT_DISTANCE);

		for (int i = 0; i < 3; i++)
		{
			PointMassOnSwitch robot = new PointMassOnSwitch(0, -Constants.SWITCH_RUNG_PIVOT_DISTANCE, 0, SWITCH_ANGLE);
			MASSES.add(robot);

			// recalculate state whenever robot position or mass are changed
			robot.SWITCH_RELATIVE_POSITION.X.addListener(observable -> recalculate());
			robot.SWITCH_RELATIVE_POSITION.Y.addListener(observable -> recalculate());
			robot.MASS.addListener(observable -> recalculate());

			numerator = numerator.add(robot.MASS.multiply(robot.SWITCH_RELATIVE_POSITION.X).multiply(-1));
			denominator = denominator.subtract(robot.MASS.multiply(Constants.SWITCH_RUNG_PIVOT_DISTANCE));

			NET_TORQUE = NET_TORQUE.add(robot.TORQUE);
		}

		DoubleBinding quotient = numerator.divide(denominator);

		THEORETICAL_ANGLE = new DoubleBinding() {
			{
				super.bind(quotient);
			}

			@Override
			protected double computeValue() {
				return Math.atan(quotient.get());
			}
		};
	}

	private void findEquilibrium()
	{
		double min = -Constants.SWITCH_MAX_ANGLE;
		double max = Constants.SWITCH_MAX_ANGLE;
		SWITCH_ANGLE.set(0);

		while (max - min > SOLVE_PRECISION.get())
		{
			if (NET_TORQUE.get() < 0)
			{
				min = SWITCH_ANGLE.get();
			}
			else
			{
				max = SWITCH_ANGLE.get();
			}

			SWITCH_ANGLE.set((min + max) / 2);
		}
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