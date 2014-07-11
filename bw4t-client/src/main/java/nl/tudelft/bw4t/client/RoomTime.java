/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.tudelft.bw4t.client;

import java.io.Serializable;

/**
 *
 * @author AK
 */
public class RoomTime implements Serializable{
    private String room;
    private long time;
    
    public RoomTime(String room,long time){
        this.room=room;
        this.time=time;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
    
}
