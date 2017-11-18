package com.crystrom.devicetracker;

import java.util.ArrayList;

/**
 * Created by Marcus Khan on 5/17/2017.
 */

public class UserNode {

    private String userName;
    private String password;
    private ArrayList<Device> devices = new ArrayList<Device>();


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public void setDevices(ArrayList<Device> devices) {
        this.devices = devices;
    }



    public UserNode(String userName, String password, ArrayList<Device> devices) {
        this.userName = userName;
        this.password = password;
        this.devices = devices;
    }

    public boolean equalsTo(UserNode user1){
        boolean b = true;

        if(!this.userName.equals(user1.getUserName())){
            b = false;
        }
        if(!this.password.equals(user1.getPassword())){
            b = false;
        }
        if(!this.devices.equals(user1.getDevices())){
            b = false;
        }



        return b;
    }

    public UserNode(){}
}
