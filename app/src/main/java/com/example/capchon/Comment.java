package com.example.capchon;

public class Comment {
    public String username;
    public String comment;

    public  Comment() {

    }

    public Comment(String username, String comment) {
        this.username = username;
        this.comment = comment;
    }

    public String getUsername() {
        return username;
    }

    public String getComment() {
        return comment;
    }
}
