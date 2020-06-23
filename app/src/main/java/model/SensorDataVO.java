package model;

import java.io.Serializable;

public  class SensorDataVO implements Serializable{
    String mode="OFF"; // 모드
    String temp = "0"; // 실내 온도
    String dust25 = "0";
    String dust10 = "0";
    String gasStatus = "0";
    String windowStatus = "0"; // 창문 onoff
    String airpurifierStatus = "0"; // 공기청정기 onoff
    String airconditionerStatus = "0"; // 에어컨 onoff
    String airconditionerMode = "COLD"; //운전모드
    String airconditionerTemp = "0"; //희망온도
    String airconditionerSpeed = "SPEED1"; //풍속 강풍 / 중풍 / 약풍
    String lightStatus = "0"; //전등

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getDust25() {
        return dust25;
    }

    public void setDust25(String dust25) {
        this.dust25 = dust25;
    }

    public String getDust10() {
        return dust10;
    }

    public void setDust10(String dust10) {
        this.dust10 = dust10;
    }

    public String getGasStatus() {
        return gasStatus;
    }

    public void setGasStatus(String gasStatus) {
        this.gasStatus = gasStatus;
    }

    public String getWindowStatus() {
        return windowStatus;
    }

    public void setWindowStatus(String windowStatus) {
        this.windowStatus = windowStatus;
    }

    public String getAirpurifierStatus() {
        return airpurifierStatus;
    }

    public void setAirpurifierStatus(String airpurifierStatus) {
        this.airpurifierStatus = airpurifierStatus;
    }

    public String getAirconditionerStatus() {
        return airconditionerStatus;
    }

    public void setAirconditionerStatus(String airconditionerStatus) {
        this.airconditionerStatus = airconditionerStatus;
    }

    public String getAirconditionerMode() {
        return airconditionerMode;
    }

    public void setAirconditionerMode(String airconditionerMode) {
        this.airconditionerMode = airconditionerMode;
    }

    public String getAirconditionerTemp() {
        return airconditionerTemp;
    }

    public void setAirconditionerTemp(String airconditionerTemp) {
        this.airconditionerTemp = airconditionerTemp;
    }

    public String getAirconditionerSpeed() {
        return airconditionerSpeed;
    }

    public void setAirconditionerSpeed(String airconditionerSpeed) {
        this.airconditionerSpeed = airconditionerSpeed;
    }

    public String getLightStatus() {
        return lightStatus;
    }

    public void setLightStatus(String lightStatus) {
        this.lightStatus = lightStatus;
    }

    @Override
    public String toString() {
        return "SensorDataVO{" +
                "mode='" + mode + '\'' +
                ", temp='" + temp + '\'' +
                ", dust25='" + dust25 + '\'' +
                ", dust10='" + dust10 + '\'' +
                ", gasStatus='" + gasStatus + '\'' +
                ", windowStatus='" + windowStatus + '\'' +
                ", airpurifierStatus='" + airpurifierStatus + '\'' +
                ", airconditionerStatus='" + airconditionerStatus + '\'' +
                ", airconditionerMode='" + airconditionerMode + '\'' +
                ", airconditionerTemp='" + airconditionerTemp + '\'' +
                ", airconditionerSpeed='" + airconditionerSpeed + '\'' +
                ", lightStatus='" + lightStatus + '\'' +
                '}';
    }
}