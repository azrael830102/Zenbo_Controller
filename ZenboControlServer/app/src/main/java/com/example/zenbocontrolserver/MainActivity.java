package com.example.zenbocontrolserver;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.Callable;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.WheelLights;


public class MainActivity extends RobotActivity {

    static final int port = 20001;

    Thread netThread;
    Thread listeningThread;
    Thread receivingThread;
    TextView hostInfo;
    ServerSocket serverSocket = null;

    Socket controllerSocket;
    Socket sensorSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hostInfo = findViewById(R.id.hostInfo);

        netThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    hostInfo.setText(getLocalIpAddress() + " : " + port);
                    Log.d("Host ", getLocalIpAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                listeningThread = new Thread(new Listening());
                listeningThread.start();
            }
        });
        netThread.start();
    }

    private static JSONObject receiveFromClient(Socket client) throws Exception {
        InputStream in = client.getInputStream();
        RunnableCallable rct = new RunnableCallable(in);
        Thread parsThread = new Thread(rct);
        parsThread.start();
        while (parsThread.isAlive()) {
            //Main Thread do nothing wait for internet thread
        }
        String receiveStr = rct.call();
        return new JSONObject(receiveStr);
    }

    private static CommandJsonFormatObj jsonObjToMsgObj(JSONObject jsonObj) throws JSONException {
        String type = jsonObj.getString("Command_type");
        String value = jsonObj.getString("Command_value");
        return new CommandJsonFormatObj(type, value);
    }

//    public static String getLocalIpAddress() {
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
//                        return inetAddress.getHostAddress();
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            Log.e("WifiPreference IpAddress", ex.toString());
//        }
//        return null;
//    }
public String getLocalIpAddress() {
    try {
        for (Enumeration<NetworkInterface> en = NetworkInterface
                .getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf
                    .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                System.out.println("ip1--:" + inetAddress);
                System.out.println("ip2--:" + inetAddress.getHostAddress());

                // for getting IPV4 format
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
    } catch (Exception ex) {
        Log.e("IP Address", ex.toString());
    }
    return null;
}
    class Listening extends Thread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    Log.d("New Client Accept", "new client accept, IP:" + client.getRemoteSocketAddress().toString());
                    Thread.sleep(1000);
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
                    receivingThread = new Thread(new Receiving(client, robotAPI));
                    receivingThread.start();
                } catch (Exception ex) {
                    System.out.println("Error: " + ex.getMessage());
                    break;
                }
            }
        }
    }

    class Receiving extends Thread implements Runnable {
        Socket client;
        RobotAPI robotAPI;

        public Receiving(Socket _client, RobotAPI _robotAPI) {
            client = _client;
            robotAPI = _robotAPI;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        JSONObject jason_obj = receiveFromClient(client);
                        if(jason_obj.has("Command_type")){
                            CommandJsonFormatObj command = jsonObjToMsgObj(jason_obj);
                            if (command != null) {
                                doCommand(command, client);
                            }
                        }else if(jason_obj.has("isController")){
                            boolean isController = (boolean) jason_obj.get("isController");
                            if(isController){
                                controllerSocket = client;
                            }else{
                                sensorSocket = client;
                            }
                        }else if(jason_obj.has("temperature")){
                            String temperature = (String) jason_obj.get("temperature");
                            Log.d("sensor value", temperature);
                            robotAPI.robot.speak("it's " + temperature + "degrees now");
                        }


                    } catch (Exception ex) {
                        System.out.println("Error: " + ex.getMessage());
                        break;
                    }
                }
                client.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        private void doCommand(CommandJsonFormatObj command, Socket client) throws IOException {
            Log.d("do Command", "Command Type : " + command.get_command_type() + " ; Command Value : " + command.get_command_value());
            int cmdType = Integer.parseInt(command.get_command_type());
            switch (cmdType) {
                case 0:// disconnect
                    Log.d("doCommand",0 + " disconnect " + command.get_command_value());
                    robotAPI.robot.setExpression(RobotFace.LAZY);
                    client.close();
                    break;
                case 1:// move
                    robotAPI.robot.setExpression(RobotFace.SHOCKED);
                    Log.d("doCommand",1 + " move: " + command.get_command_value());
                    switch(command.get_command_value()) {
                        case CommandJsonFormatObj.MOV_FORWARD:
                            robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.FORWARD);
                            break;
                        case CommandJsonFormatObj.MOV_BACKWARD:
                            robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.BACKWARD);
                            break;
                        case CommandJsonFormatObj.MOV_LEFT:
                            robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_LEFT);
                            break;
                        case CommandJsonFormatObj.MOV_RIGHT:
                            robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_RIGHT);
                            break;
                        case CommandJsonFormatObj.MOV_STOP:
                            robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.STOP);
                            break;
                    }
                    break;
                case 2:// speak
                    Log.d("doCommand",2 + " speak: " + command.get_command_value());
                    robotAPI.robot.setExpression(RobotFace.ACTIVE);
                    robotAPI.robot.speak(command.get_command_value());
                    break;
                case 3:// wheel light
                    Log.d("doCommand",3 + " wheel light: " + command.get_command_value());
                    if (command.get_command_value().equals(CommandJsonFormatObj.LIGHT_BREATH)) {
                        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
                        robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0x00D031);
                        robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
                        robotAPI.wheelLights.startBreathing(WheelLights.Lights.SYNC_BOTH, 0xff, 20, 10, 0);
                    } else if (command.get_command_value().equals(CommandJsonFormatObj.LIGHT_BLINK)) {
                        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
                        robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0x007F7F);
                        robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
                        robotAPI.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xff, 30, 10, 5);
                    } else if (command.get_command_value().equals(CommandJsonFormatObj.LIGHT_CHARGE)) {
                        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
                        robotAPI.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0xFF9000);
                        robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
                        robotAPI.wheelLights.startCharging(WheelLights.Lights.SYNC_BOTH, 0, 1, WheelLights.Direction.DIRECTION_FORWARD, 20);
                    } else if (command.get_command_value().equals(CommandJsonFormatObj.LIGHT_MARQUEE)) {
                        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
                        robotAPI.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 20);
                        robotAPI.wheelLights.startMarquee(WheelLights.Lights.SYNC_BOTH, WheelLights.Direction.DIRECTION_FORWARD, 40, 20, 3);
                    }
                    break;
                case 4:// read sensor
                    Log.d("doCommand",4 + " read sensor");
                    if(sensorSocket!=null && sensorSocket.isConnected()){
                        Log.d("read sensor",4 + "ask sensor socket for info");
                        DataOutputStream out = new DataOutputStream(sensorSocket.getOutputStream());
                        byte[] bytes = ("r\n").getBytes();
                        out.write(bytes);
                        out.flush();
                    }else{
                        Log.d("read sensor",4 + "no sensor socket");
                        robotAPI.robot.speak("There is no sensor device.");
                    }
                    break;
            }
        }
    }

    static class RunnableCallable implements Callable<String>, Runnable {
        InputStream _in;
        String resultStr;

        @Override
        public void run() {
            resultStr = parseInfo(_in);
        }

        public RunnableCallable(InputStream in) {
            _in = in;
        }

        @Override
        public String call() {
            return resultStr;
        }

        private static String parseInfo(InputStream in) {
            String str = "";
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                str = br.readLine();
                Log.d("parseInfo() : ", str);
            } catch (IOException e) {
                Log.e("parseInfo Error", e.getMessage());
                e.printStackTrace();
            }
            return str;
        }
    }


    public MainActivity() {
        super(robotCallback, robotListenCallback);
    }

    public static RobotCallback robotCallback = new RobotCallback() {
        @Override
        public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
            super.onResult(cmd, serial, err_code, result);
        }

        @Override
        public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
            super.onStateChange(cmd, serial, err_code, state);
        }

        @Override
        public void initComplete() {
            super.initComplete();
        }
    };

    public static RobotCallback.Listen robotListenCallback = new RobotCallback.Listen() {
        @Override
        public void onFinishRegister() {
        }

        @Override
        public void onVoiceDetect(JSONObject jsonObject) {
        }

        @Override
        public void onSpeakComplete(String s, String s1) {
        }

        @Override
        public void onEventUserUtterance(JSONObject jsonObject) {
        }

        @Override
        public void onResult(JSONObject jsonObject) {
        }

        @Override
        public void onRetry(JSONObject jsonObject) {
        }
    };

}