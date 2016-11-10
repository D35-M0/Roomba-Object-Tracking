package roomba.acorl.roombaobjecttracker;

import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.support.v4.content.res.TypedArrayUtils;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Cameron on 11/2/2016.
 */
public class RobotInterface {

    public static final int STOP = 0;
    public static final byte FORWARD = 1;
    public static final int VARI_FOR = 2;
    public static final int BACKWARD = 3;
    public static final int VARI_BACK = 4;
    public static final int LEFT_PIVOT = 5;
    public static final int RIGHT_PIVOT = 6;
    public static final int FOR_LEFT = 7;
    public static final int VARI_FOR_LEFT = 8;
    public static final int FOR_RIGHT = 9;
    public static final int VARI_FOR_RIGHT = 10;
    public static final int DIRECTM = 11;
    public static final int POLL_DATA = 12;
    public static final int SERVO = 13;

    public static final int RAD_STRAIGHT = 32768;
    public static final int RAD_CLOCKWISE = -1;
    public static final int RAD_COUNTER_CLOCKWISE = 1;
    public static final int SPEED_DEFUALT = 200;

    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public RobotInterface(BluetoothSocket socket) {
        mSocket = socket;

        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) {

        }
    }

    public void stop() {
        byte[] haltCommand = {0, 0, 0, 0, 0};

        try {
            mOutputStream.write(haltCommand);
        } catch (IOException e) {

        }
    }

    public void drive(int speed, int radius) {
        byte[] driveParams = convertToHexBytes(speed, radius);

        ArrayList<Byte> fullCommand = new ArrayList<>();
        fullCommand.add(FORWARD);

        for (byte com: driveParams) {
            fullCommand.add(com);
        }

        try {
            Byte[] whyyy = fullCommand.toArray(new Byte[fullCommand.size()]);

            byte[] momsSpaghetti = new byte[whyyy.length];

            for (int i = 0; i < momsSpaghetti.length; i++) {
                momsSpaghetti[i] = whyyy[i].byteValue();
            }
            mOutputStream.write(momsSpaghetti);
            mOutputStream.flush();
        } catch (IOException e) {

        }

    }

    private byte[] convertToHexBytes(int speed, int radius) {

        // Converts our integers into hex string
        String rawSpeedHex = Integer.toHexString(speed);
        String rawRadHex = Integer.toHexString(radius);

        // Adds 0's to front as necessary to create 2 bytes worth
        while (rawSpeedHex.length() < 4)
            rawSpeedHex = "0" + rawSpeedHex;

        while (rawRadHex.length() < 4)
            rawRadHex = "0" + rawRadHex;


        // Splits each of the hex string into high and low hex strings
        String speedHexHighS = rawSpeedHex.substring(0, 2);
        String speedHexLowS = rawSpeedHex.substring(2, 4);
        String radHexHighS = rawRadHex.substring(0, 2);
        String radHexLowS = rawRadHex.substring(2, 4);

        // Use Integers as midways points because Java
        byte speedHighB = (byte) Integer.parseInt(speedHexHighS, 16);
        byte speedLowB = (byte) Integer.parseInt(speedHexLowS, 16);
        byte radHighB = (byte) Integer.parseInt(radHexHighS, 16);
        byte radLowB = (byte) Integer.parseInt(radHexLowS, 16);


        byte[] hexValues = {speedHighB, speedLowB, radHighB, radLowB};

        return hexValues;
    }
}
