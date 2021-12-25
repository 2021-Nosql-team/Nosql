package io.grpc.examples.service;

public class UserToMovie {
    public String movieId;
    public String rating;
    public String timestamp;
    public String comment;

    public UserToMovie(String movieId){
        this.movieId = movieId;
    }

    public void setRating(String rating){
        this.rating = rating;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public void setComment(String comment){
        this.comment = comment;
    }

}
