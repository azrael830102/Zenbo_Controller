package com.example.zenbocontrolclient;

/**
 * ------Type-------|-------------------Value---------------------
 * -----------------|---------------------------------------------
 * 0 = disconnect---|--------------------""-----------------------
 * 1 = movement-----|---Forward ; Backward ; Left ; Right ; Stop--
 * 2 = speak--------|--------------[input content]----------------
 * 3 = wheel light--|----Breath ; Blink ; Charging ; Marquee------
 * 4 = read sensor--|--------------------""-----------------------
 */
public class CommandJsonFormatObj {

    public static final String MOV_FORWARD = "Forward";
    public static final String MOV_BACKWARD = "Backward";
    public static final String MOV_LEFT = "Left";
    public static final String MOV_RIGHT = "Right";
    public static final String MOV_STOP = "Stop";

    public static final String LIGHT_BREATH = "Breath";
    public static final String LIGHT_BLINK = "Blink";
    public static final String LIGHT_CHARGE = "Charging";
    public static final String LIGHT_MARQUEE = "Marquee";

    private String _command_type;
    private String _command_value;

    public CommandJsonFormatObj() {
    }

    public CommandJsonFormatObj(String commandType, String commandValue) {
        this._command_type = commandType;
        this._command_value = commandValue;
    }


    public String get_command_type() {
        return _command_type;
    }

    public void set_command_type(String _command_type) {
        this._command_type = _command_type;
    }

    public String get_command_value() {
        return _command_value;
    }

    public void set_command_value(String _command_value) {
        this._command_value = _command_value;
    }


}
