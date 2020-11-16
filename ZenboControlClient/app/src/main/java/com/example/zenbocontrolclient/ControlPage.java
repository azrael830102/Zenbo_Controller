package com.example.zenbocontrolclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.net.SocketFactory;

public class ControlPage extends AppCompatActivity {

    private String server_ip;
    private int port = 20001;

    Thread netThread;

    private Button btn_disconnect;
    private Button btn_read_sensor;

    private Button btn_forward;
    private Button btn_backward;
    private Button btn_turn_left;
    private Button btn_turn_right;
    private Button btn_stop;

    private Button btn_light_breath;
    private Button btn_light_blink;
    private Button btn_light_charge;
    private Button btn_light_marquee;

    private Button btn_speak;
    private EditText et_speak_content;

    Socket serverSocket = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_page);
        initViewElement();

        Intent it = this.getIntent();
        Bundle bundle = it.getExtras();
        server_ip = bundle.getString("ip");

        netThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("Connect", "Waiting to connect......");
                    serverSocket = SocketFactory.getDefault().createSocket();
                    SocketAddress remote_addr = new InetSocketAddress(server_ip, port);
                    try {
                        serverSocket.connect(remote_addr, 5000);
                    } catch (SocketTimeoutException | UnknownHostException se) {
                        Intent it = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("errMsg", "Connected failed");
                        it.putExtras(bundle);
                        it.setClass(ControlPage.this, MainActivity.class);
                        Log.d("Connect", "Not Connected");
                        startActivity(it);
                    }
                    if (serverSocket.isConnected()) {
                        Log.d("Connect", "Connected");
                        JSONObject json_obj = new JSONObject();
                        json_obj.put("isController", true);
                        byte[] jsonByte = (json_obj.toString() + "\n").getBytes();
                        DataOutputStream outputStream = new DataOutputStream(serverSocket.getOutputStream());
                        outputStream.write(jsonByte);
                        outputStream.flush();
                    }
                } catch (IOException | JSONException e) {
                    Intent it = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("errMsg", e.getMessage());
                    it.putExtras(bundle);
                    it.setClass(ControlPage.this, MainActivity.class);
                    startActivity(it);
                    e.printStackTrace();
                }
            }
        });
        netThread.start();
        while (netThread.isAlive()) {

        }
        setBtnOnClickListener();
    }

    private void initViewElement() {
        btn_read_sensor = findViewById(R.id.btn_read_sensor);
        btn_disconnect = findViewById(R.id.btn_disconnect);

        btn_forward = findViewById(R.id.btn_forward);
        btn_backward = findViewById(R.id.btn_backward);
        btn_turn_left = findViewById(R.id.btn_turn_left);
        btn_turn_right = findViewById(R.id.btn_turn_right);
        btn_stop = findViewById(R.id.btn_stop);

        btn_light_breath = findViewById(R.id.btn_light_breath);
        btn_light_blink = findViewById(R.id.btn_light_blink);
        btn_light_charge = findViewById(R.id.btn_light_charge);
        btn_light_marquee = findViewById(R.id.btn_light_marquee);

        btn_speak = findViewById(R.id.btn_speak);
        et_speak_content = findViewById(R.id.et_speak_content);
    }

    private void setBtnOnClickListener() {
        if (serverSocket != null && serverSocket.isConnected()) {
            BtnClick click = new BtnClick(serverSocket);
            btn_read_sensor.setOnClickListener(click);
            btn_disconnect.setOnClickListener(click);
            btn_forward.setOnClickListener(click);
            btn_backward.setOnClickListener(click);
            btn_turn_left.setOnClickListener(click);
            btn_turn_right.setOnClickListener(click);

            btn_light_breath.setOnClickListener(click);
            btn_light_blink.setOnClickListener(click);
            btn_light_charge.setOnClickListener(click);
            btn_light_marquee.setOnClickListener(click);

            btn_stop.setOnClickListener(click);
            btn_speak.setOnClickListener(click);
        }
    }


    private static void SendingMsg(Socket socket, CommandJsonFormatObj msgObj) {
        try {
            JSONObject json = new JSONObject(msgObjToMap(msgObj));
            byte[] jsonByte = (json.toString() + "\n").getBytes();
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(jsonByte);
            outputStream.flush();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    private static HashMap<String, String> msgObjToMap(CommandJsonFormatObj msgObj) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Command_type", msgObj.get_command_type());
        map.put("Command_value", msgObj.get_command_value());
        return map;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket.isConnected()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class BtnClick implements View.OnClickListener {
        private Socket _serverSocket;

        public BtnClick(Socket serverSocket) {
            _serverSocket = serverSocket;
        }

        @Override
        public void onClick(View v) {
            CommandJsonFormatObj msgObj = setCommandObj(v);
            Thread sendThread = new Thread(new Runnable() {
                CommandJsonFormatObj _msgObj;

                @Override
                public void run() {
                    try {
                        SendingMsg(_serverSocket, _msgObj);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                private Runnable init(CommandJsonFormatObj msgObj) {
                    _msgObj = msgObj;
                    return this;
                }
            }.init(msgObj));
            sendThread.start();
            while (sendThread.isAlive()) {
            }
            sendThread.interrupt();

            if (msgObj.get_command_type().equals("0")) {//disconnect
                try {
                    Intent it = new Intent();
                    it.setClass(ControlPage.this, MainActivity.class);
                    startActivity(it);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
            }
        }

        private CommandJsonFormatObj setCommandObj(View v) {
            Button btn = (Button) v;

            switch (btn.getId()) {
                case R.id.btn_disconnect:
                    return new CommandJsonFormatObj("0", "");
                case R.id.btn_forward:
                    return new CommandJsonFormatObj("1", CommandJsonFormatObj.MOV_FORWARD);
                case R.id.btn_backward:
                    return new CommandJsonFormatObj("1", CommandJsonFormatObj.MOV_BACKWARD);
                case R.id.btn_turn_left:
                    return new CommandJsonFormatObj("1", CommandJsonFormatObj.MOV_LEFT);
                case R.id.btn_turn_right:
                    return new CommandJsonFormatObj("1", CommandJsonFormatObj.MOV_RIGHT);
                case R.id.btn_stop:
                    return new CommandJsonFormatObj("1", CommandJsonFormatObj.MOV_STOP);
                case R.id.btn_speak:
                    return new CommandJsonFormatObj("2", et_speak_content.getText().toString());
                case R.id.btn_light_breath:
                    return new CommandJsonFormatObj("3", CommandJsonFormatObj.LIGHT_BREATH);
                case R.id.btn_light_blink:
                    return new CommandJsonFormatObj("3", CommandJsonFormatObj.LIGHT_BLINK);
                case R.id.btn_light_charge:
                    return new CommandJsonFormatObj("3", CommandJsonFormatObj.LIGHT_CHARGE);
                case R.id.btn_light_marquee:
                    return new CommandJsonFormatObj("3", CommandJsonFormatObj.LIGHT_MARQUEE);
                case R.id.btn_read_sensor:
                    return new CommandJsonFormatObj("4", "");
                default:
                    return new CommandJsonFormatObj();
            }
        }
    }


}