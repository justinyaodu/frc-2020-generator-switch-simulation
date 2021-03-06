package simulation;

public class Constants
{
    // all units are inches, pounds, radians
    public static double SWITCH_PIVOT_HEIGHT = 111.375;
    public static double SWITCH_RUNG_PIVOT_DISTANCE = SWITCH_PIVOT_HEIGHT - 63;
    public static double SWITCH_COM_PIVOT_DISTANCE = 26;
    public static double SWITCH_WEIGHT = 93;
    public static double SWITCH_LEVEL_THRESHOLD = 8 * Math.PI / 180;
    public static double SWITCH_MAX_ANGLE = 14.5 * Math.PI / 180;
    public static double SWITCH_HANDLE_LENGTH = 114.25;

    public static double ROBOT_MAX_WEIGHT = 150;
}
