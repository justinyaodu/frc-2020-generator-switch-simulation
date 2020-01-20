package gui;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

class FXHelper
{
	private static final double SPACING = 5;
	private static final Insets PADDING = new Insets(SPACING);

	static GridPane newGridPane()
	{
		GridPane gridPane = new GridPane();
		gridPane.setHgap(SPACING * 2);
		gridPane.setVgap(SPACING);
		gridPane.setPadding(PADDING);
		return gridPane;
	}

	static VBox newVbox(Node... nodes)
	{
		VBox vBox = new VBox(nodes);
		vBox.setSpacing(SPACING);
		vBox.setPadding(PADDING);
		return vBox;
	}

	static BorderPane titledPane(String title, Node content)
	{
		return new BorderPane(content, boldText(title), null, null, null);
	}

	static Text boldText(String string)
	{
		Text text = new Text(string);
		text.setStyle("-fx-font-weight:bold");
		return text;
	}

	static Slider buildSlider(double min, double max, double initial, DoubleProperty property)
	{
		Slider slider = new Slider(min, max, initial);
		if (property != null)
		{
			slider.valueProperty().bindBidirectional(property);
		}
		return slider;
	}

	static void sliderTicks(Slider slider, double majorTickUnit, int minorTickCount)
	{
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(majorTickUnit);
		slider.setMinorTickCount(minorTickCount);
	}
}
