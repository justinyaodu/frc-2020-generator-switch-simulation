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
        for (int i = 1; i < simulation.getPointMasses().size(); i++)
        {
            PointMassOnSwitch pointMass = simulation.getPointMasses().get(i);

            GridPane controls = newGridPane();
            int row = 0;

            Slider positionSlider = buildSlider(-Constants.SWITCH_HANDLE_LENGTH / 2, Constants.SWITCH_HANDLE_LENGTH / 2,
                    0, pointMass.getSwitchRelativePosition().xProperty());
            //sliderTicks(positionSlider, 6, 0);
            DoubleInput positionInput = new DoubleInput(pointMass.getSwitchRelativePosition().xProperty());
            controls.addRow(row++, new Text("Position along handle (in)"), positionSlider, positionInput);


            Slider massSlider = buildSlider(0, Constants.ROBOT_MAX_WEIGHT, 100, pointMass.massProperty());
            //sliderTicks(massSlider, 10, 4);
            DoubleInput massInput = new DoubleInput(pointMass.massProperty());
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
        for (int i = 1; i < simulation.getPointMasses().size(); i++)
        {
            pane.getChildren().addAll(buildRobot(i));
        }

        // add switch
        pane.getChildren().addAll(buildSwitch());

        // add switch text
        pane.getChildren().add(buildSwitchText());

        // add COM
        Circle com = new Circle(5, Color.BLACK);
        PointMassOnSwitch comPosition = new PointMassOnSwitch(0, 0, 0, simulation.equilibriumAngleProperty());
        comPosition.getSwitchRelativePosition().xProperty().bind(simulation.comXProperty());
        comPosition.getSwitchRelativePosition().yProperty().bind(simulation.comYProperty());
        bindNodePosition(com, comPosition.getPosition());
        pane.getChildren().add(com);

        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setPannable(true);

        return scrollPane;
    }

    private Node[] buildRobot(int index)
    {
        Color[] colors = new Color[]{Color.BLACK, Color.RED, Color.GREEN, Color.BLUE};
        Color color = colors[index % colors.length];

        PointMassOnSwitch robot = simulation.getPointMasses().get(index);

        DoubleBinding levelXMin = simulation.getLevelXMin().get(index);
        DoubleBinding levelXZero = simulation.getLevelXZero().get(index);
        DoubleBinding levelXMax = simulation.getLevelXMax().get(index);

        Group robotGroup = new Group();
        bindNodePosition(robotGroup, robot.getPosition());

        // show robot if mass greater than zero or position is not zero
        robotGroup.visibleProperty().bind(Bindings.or(Bindings.greaterThan(robot.massProperty(), 0),
                Bindings.notEqual(robot.getSwitchRelativePosition().xProperty(), 0)));

        // draw a vertical line with length proportional to robot weight
        Line line = new Line();
        line.setStroke(colors[index]);
        line.setStrokeWidth(2);
        line.endYProperty().bind(robot.massProperty());
        robotGroup.getChildren().add(line);

        StringExpression tolerances = Bindings.format("%+.1f %+.1f",
                levelXMax.subtract(robot.getSwitchRelativePosition().xProperty()),
                levelXMin.subtract(robot.getSwitchRelativePosition().xProperty()));

        StringExpression ideal = Bindings.format("%.1f (%+.1f)", levelXZero,
                levelXZero.subtract(robot.getSwitchRelativePosition().xProperty()));

        PointMassOnSwitch idealPosition = new PointMassOnSwitch(0, 0, 0, simulation.equilibriumAngleProperty());
        idealPosition.getSwitchRelativePosition().xProperty().bind(levelXZero);
        idealPosition.getSwitchRelativePosition().yProperty().bind(robot.getSwitchRelativePosition().yProperty().subtract(index));
        Circle idealIndicator = new Circle(3, color);
        bindNodePosition(idealIndicator, idealPosition.getPosition());

        PointMassOnSwitch minEndpoint = new PointMassOnSwitch(0, 0, 0, simulation.equilibriumAngleProperty());
        minEndpoint.getSwitchRelativePosition().xProperty().bind(levelXMin);
        minEndpoint.getSwitchRelativePosition().yProperty().bind(idealPosition.getSwitchRelativePosition().yProperty());

        PointMassOnSwitch maxEndpoint = new PointMassOnSwitch(0, 0, 0, simulation.equilibriumAngleProperty());
        maxEndpoint.getSwitchRelativePosition().xProperty().bind(levelXMax);
        maxEndpoint.getSwitchRelativePosition().yProperty().bind(idealPosition.getSwitchRelativePosition().yProperty());

        Line tolerance = buildBoundLine(minEndpoint.getPosition().xProperty(), minEndpoint.getPosition().yProperty(),
                maxEndpoint.getPosition().xProperty(), maxEndpoint.getPosition().yProperty(), color.interpolate(Color.TRANSPARENT, 0.5));
        tolerance.setStrokeWidth(2);

        // hide tolerance line if robot is hidden
        tolerance.visibleProperty().bind(robotGroup.visibleProperty());

        Text label = new Text();
        label.textProperty().bind(Bindings.format("\nx: %.1f in\nx tol.: %s\nx ideal: %s\nweight: %.1f lbs\nmoment arm: %.1f in\ntorque: %.1f in-lbs",
                robot.getSwitchRelativePosition().xProperty(), tolerances, ideal, robot.massProperty(), robot.getPosition().xProperty(), robot.torqueProperty()));
        label.layoutYProperty().bind(line.endYProperty());
        robotGroup.getChildren().add(label);

        return new Node[]{robotGroup, idealIndicator, tolerance};
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
            vertices[i] = new PointMassOnSwitch(vertices[i].xProperty().get(), vertices[i].yProperty().get(), 0, simulation.equilibriumAngleProperty()).getPosition();
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
        bindNodePosition(text, new PointMassOnSwitch(0, 3, 0, simulation.equilibriumAngleProperty()).getPosition());

        StringExpression angleString = Bindings.format("%.1f°", simulation.equilibriumAngleProperty().multiply(180).divide(Math.PI));
        StringExpression levelString = new StringBinding()
        {
            {
                super.bind(simulation.isLevelProperty());
            }

            @Override
            protected String computeValue()
            {
                return simulation.isLevelProperty().get() ? "level" : "not level";
            }
        };

        text.textProperty().bind(Bindings.format("%s (%s)", angleString, levelString));

        return text;
    }

    private void bindNodeX(DoubleProperty xProperty, ObservableDoubleValue posX)
    {
        xProperty.bind(new DoubleBinding()
        {
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
        bindNodeX(node.layoutXProperty(), position.xProperty());
        bindNodeY(node.layoutYProperty(), position.yProperty());
    }
}
