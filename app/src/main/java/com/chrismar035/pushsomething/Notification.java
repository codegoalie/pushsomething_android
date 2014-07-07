package com.chrismar035.pushsomething;

/**
 * Created by chrismar035 on 10/26/13.
 */
public class Notification {
    private long id;
    private String title;
    private String body;
    private long createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Notification() {}

    public Notification(String title, String body) {
        this.title = title;
        this.body = body;
        this.createdAt = System.currentTimeMillis()/1000;
    }

    @Override
    public String toString() {
        return title;
    }


}
