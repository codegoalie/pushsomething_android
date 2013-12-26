package com.chrismar035.pushsomething;

/**
 * Created by chrismar035 on 10/26/13.
 */
public class Notification {
    private int id;
    private int server_id;
    private String title;
    private String body;
    private long createdAt;

    public long getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getServerId() { return server_id; }
    public void setServerId(int server_id) { this.server_id = server_id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Notification() {}

    public Notification(int id, int server_id, String title, String body) {
        this.id = id;
        this.server_id = server_id;
        this.title = title;
        this.body = body;
        this.createdAt = System.currentTimeMillis()/1000;
    }

    @Override
    public String toString() {
        return title;
    }
}
