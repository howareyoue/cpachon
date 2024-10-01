package com.example.capchon;

public class Comment {
    private String username;
    private String commentText;

    // Firebase에 데이터를 저장하기 위해 기본 생성자가 필요합니다.
    public Comment() {}

    public Comment(String username, String commentText) {
        this.username = username;
        this.commentText = commentText;
    }

    public String getUsername() {
        return username;
    }

    public String getCommentText() {
        return commentText;
    }
}
