package gui;

import geometry.Vector2D;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import simulation.Constants;
import simulation.PointMassOnSwitch;
import simulation.SwitchSimulation;

import static gui.FXHelper.*;

public class GUI extends Application
{
	private SwitchSimulation simulation = new SwitchSimulation();
	private DoubleProperty inchToPixel = new SimpleDoubleProperty(6);
	private DoubleProperty windowWidthInches = new SimpleDoubleProperty(150);
	private DoubleProperty windowHeightInches = new SimpleDoubleProperty(150);

	@Override
	public void start(Stage stage)
	{
		stage.setTitle("FRC 2020 Generator Switch Simulation (by Justin Yao Du, 2473 Goldstrikers)");
		stage.setScene(new Scene(buildRoot()));
		stage.sizeToScene();
		stage.show();
	}

	private Parent buildRoot()
	{
		return new BorderPane(buildDisplay(), null, null, null, buildSidebar());
	}

	private Parent buildSidebar()
	{
		return newVbox(buildRobotParameters());
	}

	private Parent buildRobotParameters()
	{
		VBox parent = newVbox();

		// skip first element, since that's the COM of the switch
		for (int i = 1; i < simulation.MASSES.size(); i++)
		{
			PointMassOnSwitch pointMass = simulation.MASSES.get(i);

			GridPane controls = newGridPane();
			int row = 0;

			Slider positionSlider = buildSlider(-Constants.SWITCH_HANDLE_LENGTH / 2, Constants.SWITCH_HANDLE_LENGTH / 2,
					0, pointMass.SWITCH_RELATIVE_POSITION.X);
			//sliderTicks(positionSlider, 6, 0);
			DoubleInput positionInput = new DoubleInput(pointMass.SWITCH_RELATIVE_POSITION.X);
			controls.addRow(row++, new Text("Position along handle (in)"), positionSlider, positionInput);


			Slider massSlider = buildSlider(0, Constants.ROBOT_MAX_WEIGHT, 100, pointMass.MASS);
			//sliderTicks(massSlider, 10, 4);
			DoubleInput massInput = new DoubleInput(pointMass.MASS);
			controls.addRow(row++, new Text("Robot weight (lbs)"), massSlider, massInput);

			parent.getChildren().add(titledPane("Robot " + i, controls));
		}

		return parent;
	}

	private Parent buildDisplay()
	{
		Pane pane = new Pane();
		pane.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		pane.prefWidthProperty().bind(Bindings.multiply(windowWidthInches, inchToPixel));
		pane.prefHeightProperty().bind(Bindings.multiply(windowHeightInches, inchToPixel));

		// add robots
		for (int i = 1; i < simulation.MASSES.size(); i++)
		{
			pane.getChildren().addAll(buildRobot(i));
		}

		// add switch
		pane.getChildren().addAll(buildSwitch());

		// add switch text
		pane.getChildren().add(buildSwitchText());

		// add COM
		Circle com = new Circle(5, Color.BLACK);
		bindNodeX(com.layoutXProperty(), simulation.COM_X);
		bindNodeY(com.layoutYProperty(), simulation.COM_Y);
		pane.getChildren().add(com);

		ScrollPane scrollPane = new ScrollPane(pane);
		scrollPane.setPannable(true);

		return scrollPane;
	}

	private Node[] buildRobot(int index)
	{
		Color[] colors = new Color[]{Color.BLACK, Color.RED, Color.GREEN, Color.BLUE};

		PointMassOnSwitch pointMass = simulation.MASSES.get(index);

		Group robot = new Group();
		bindNodePosition(robot, pointMass.POSITION);
		robot.visibleProperty().bind(Bindings.or(Bindings.greaterThan(pointMass.MASS, 0),
				Bindings.notEqual(pointMass.SWITCH_RELATIVE_POSITION.X, 0)));

		Line line = new Line();
		line.setStroke(colors[index]);
		line.endYProperty().bind(pointMass.MASS.multiply(0.5));
		robot.getChildren().add(line);

		StringBinding tolerances = new StringBinding() {
			{
				super.bind(simulation.TOLERANCES_PLUS, simulation.TOLERANCES_MINUS);
			}

			@Override
			protected String computeValue()
			{
				try
				{
					return String.format(" +%.1f -%.1f",
							simulation.TOLERANCES_PLUS.get(index - 1), simulation.TOLERANCES_MINUS.get(index - 1));
				}
				catch (IndexOutOfBoundsException e)
				{
					return "";
				}
			}
		};

		PointMassOnSwitch minEndpoint = new PointMassOnSwitch(0, 0, 0, simulation.SWITCH_ANGLE);
		minEndpoint.SWITCH_RELATIVE_POSITION.X.bind(pointMass.SWITCH_RELATIVE_POSITION.X.subtract(new DoubleBinding() {
			{
				super.bind(simulation.TOLERANCES_MINUS);
			}

			@Override
			protected double computeValue()
			{
				try
				{
					return simulation.TOLERANCES_MINUS.get(index - 1);
				}
				catch (IndexOutOfBoundsException e)
				{
					return 0;
				}
			}
		}));
		minEndpoint.SWITCH_RELATIVE_POSITION.Y.bind(pointMass.SWITCH_RELATIVE_POSITION.Y.subtract(1));
		Line toleranceMin = buildBoundLine(pointMass.POSITION.X, pointMass.POSITION.Y, minEndpoint.POSITION.X, minEndpoint.POSITION.Y, colors[index % colors.length]);

		PointMassOnSwitch maxEndpoint = new PointMassOnSwitch(0, 0, 0, simulation.SWITCH_ANGLE);
		maxEndpoint.SWITCH_RELATIVE_POSITION.X.bind(pointMass.SWITCH_RELATIVE_POSITION.X.add(new DoubleBinding() {
			{
				super.bind(simulation.TOLERANCES_PLUS);
			}

			@Override
			protected double computeValue()
			{
				try
				{
					return simulation.TOLERANCES_PLUS.get(index - 1);
				}
				catch (IndexOutOfBoundsException e)
				{
					return 0;
				}
			}
		}));
		maxEndpoint.SWITCH_RELATIVE_POSITION.Y.bind(pointMass.SWITCH_RELATIVE_POSITION.Y.subtract(1));
		Line toleranceMax = buildBoundLine(pointMass.POSITION.X, pointMass.POSITION.Y, maxEndpoint.POSITION.X, maxEndpoint.POSITION.Y, colors[index % colors.length]);

		toleranceMin.visibleProperty().bind(robot.visibleProperty());
		toleranceMax.visibleProperty().bind(robot.visibleProperty());

		Text label = new Text();
		label.textProperty().bind(Bindings.format("%.1f in%s\n%.1f lbs\n%.1f in-lbs", pointMass.SWITCH_RELATIVE_POSITION.X, tolerances, pointMass.MASS, pointMass.TORQUE));
		label.layoutYProperty().bind(line.endYProperty());
		robot.getChildren().add(label);

		return new Node[] {robot, toleranceMin, toleranceMax};
	}

	private Line buildBoundLine(ObservableDoubleValue startX, ObservableDoubleValue startY, ObservableDoubleValue endX, ObservableDoubleValue endY, Color color)
	{
		Line line = new Line();
		line.setStroke(color);
		bindNodeX(line.startXProperty(), startX);
		bindNodeY(line.startYProperty(), startY);
		bindNodeX(line.endXProperty(), endX);
		bindNodeY(line.endYProperty(), endY);
		return line;
	}

	private Node[] buildSwitch()
	{
		// relative coordinates
		Vector2D[] vertices = new Vector2D[]{new Vector2D(0, 0),
				new Vector2D(-Constants.SWITCH_HANDLE_LENGTH / 2, -Constants.SWITCH_RUNG_PIVOT_DISTANCE),
				new Vector2D(Constants.SWITCH_HANDLE_LENGTH / 2, -Constants.SWITCH_RUNG_PIVOT_DISTANCE)};

		// convert to absolute coordinates
		for (int i = 0; i < vertices.length; i++)
		{
			vertices[i] = new PointMassOnSwitch(vertices[i].X.get(), vertices[i].Y.get(), 0, simulation.SWITCH_ANGLE).POSITION;
		}

		// create nodes for each vertex
		Node[] nodes = new Node[vertices.length];
		for (int i = 0; i < nodes.length; i++)
		{
			nodes[i] = new Circle(0);
			bindNodePosition(nodes[i], vertices[i]);
		}

		// add line segments between vertices
		Line[] lines = new Line[nodes.length];
		for (int i = 0; i < nodes.length; i++)
		{
			lines[i] = new Line();
			lines[i].startXProperty().bind(nodes[i].layoutXProperty());
			lines[i].startYProperty().bind(nodes[i].layoutYProperty());
			lines[i].endXProperty().bind(nodes[(i + 1) % nodes.length].layoutXProperty());
			lines[i].endYProperty().bind(nodes[(i + 1) % nodes.length].layoutYProperty());
		}

		return lines;
	}

	private Node buildSwitchText()
	{
		Text text = new Text();
		bindNodePosition(text, new PointMassOnSwitch(0, 3, 0, simulation.SWITCH_ANGLE).POSITION);

		StringExpression angleString = Bindings.format("%.1f°", simulation.SWITCH_ANGLE.multiply(180 / Math.PI));
		StringExpression levelString = new StringBinding() {
			{
				super.bind(simulation.IS_LEVEL);
			}
			@Override
			protected String computeValue()
			{
				return simulation.IS_LEVEL.get() ? "level" : "not level";
			}
		};

		text.textProperty().bind(Bindings.format("%s (%s)\n%.1f in-lb", angleString, levelString, simulation.NET_TORQUE));

		return text;
	}

	private void bindNodeX(DoubleProperty xProperty, ObservableDoubleValue posX)
	{
		xProperty.bind(new DoubleBinding() {
			{
				super.bind(posX, windowWidthInches, inchToPixel);
			}

			@Override
			protected double computeValue()
			{
				double x = (posX.get() + windowWidthInches.get() / 2) * inchToPixel.get();
				// System.err.printf("screen pos x is %f\n", x);
				return x;
			}
		});
	}

	private void bindNodeY(DoubleProperty yProperty, ObservableDoubleValue posY)
	{
		yProperty.bind(new DoubleBinding()
		{
			{
				super.bind(posY, windowHeightInches, inchToPixel);
			}

			@Override
			protected double computeValue()
			{
				double y = (windowHeightInches.get() - posY.get()) * inchToPixel.get();
				// System.err.printf("screen pos y is %f\n", y);
				return y;
			}
		});
	}

	private void bindNodePosition(Node node, Vector2D position)
	{
		bindNodeX(node.layoutXProperty(), position.X);
		bindNodeY(node.layoutYProperty(), position.Y);
	}
}
