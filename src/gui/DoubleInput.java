package gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TextField;

public class DoubleInput extends TextField
{
	private static final int PRECISION_DIGITS = 1;
	private static final double PRECISION_AMOUNT = Math.pow(10, -PRECISION_DIGITS);
	private DoubleProperty value = new SimpleDoubleProperty();

	public DoubleInput(DoubleProperty property)
	{
		this();
		value.bindBidirectional(property);
	}

	public DoubleInput()
	{
		// update value if current text is valid
		textProperty().addListener(o ->
		{
			try
			{
				value.set(parseValue());
			}
			catch (NumberFormatException e)
			{
				setText(Double.toString(value.doubleValue()));
			}
		});

		// update text if value is changed
		value.addListener((o, s1, s2) ->
		{
			try
			{
				// if values are close enough, don't update
				if (Math.abs(s2.doubleValue() - parseValue()) > (PRECISION_AMOUNT / 10))
				{
					updateTextToMatchValue();
				}
			}
			catch (NumberFormatException e)
			{
				// if text is not even a valid numerical value, update
				updateTextToMatchValue();
			}
		});

		// initialize text
		updateTextToMatchValue();

		// reduce width
		setMaxWidth(60);
	}

	// helper method to parse current text field text as a double
	private double parseValue() throws NumberFormatException
	{
		// if box is empty or a single negative sign, return zero
		// either of these cases probably mean the user is in the process of typing something
		if (getText().equals("") || getText().equals("-"))
		{
			return 0;
		}

		return Double.parseDouble(getText());
	}

	private void updateTextToMatchValue()
	{
		textProperty().set(String.format(("%." + PRECISION_DIGITS + "f"), value.getValue()));
	}

	public DoubleProperty valueProperty()
	{
		return value;
	}
}