package com.app.schat;

import android.graphics.Bitmap;

public class ChatEntity
{
	public int userImage;
    public String content;
    public String chatTime;
    public String userName;
    private boolean isComeMsg;
    public long uid;
    public long msg_id;
    public int chat_type;
    public String file_name;
    public Bitmap bmp_content;
    public long chat_flag;
    public long video_time;
    public boolean isComeMsg() {
        return isComeMsg;
    }
    public void setComeMsg(boolean isComeMsg) {
        this.isComeMsg = isComeMsg;
    }

    /*
    public int getUserImage() {  
        return userImage;  
    }  
    public void setUserImage(int userImage) {  
        this.userImage = userImage;  
    }
    public String getUserName() {return userName; }
    public void setUserName(String name) {this.userName = name;}
    public String getContent() {  
        return content;  
    }  
    public void setContent(String content) {  
        this.content = content;  
    }  
    public String getChatTime() {  
        return chatTime;  
    }  
    public void setChatTime(String chatTime) {  
        this.chatTime = chatTime;  
    }  
    public boolean isComeMsg() {  
        return isComeMsg;  
    }  
    public void setComeMsg(boolean isComeMsg) {  
        this.isComeMsg = isComeMsg;  
    } */

    public ChatEntity() {
        this.content = this.chatTime = this.userName = this.file_name = "";
    }


}
