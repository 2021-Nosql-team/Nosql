package io.grpc.examples.service;

import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.UserProfileResponse;
import io.grpc.examples.helloworld.UserRatingList;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SaveMessage {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);
    UserProfileResponse userProfileMessage;

    public SaveMessage(){
        System.out.println("pong:" + jedis.ping());//测试数据库是否连接成功
    }



    public void serializeMessage(String user_id, Map<String,String> preference, List<UserToMovie> userToMovie, String avg_rating) throws IOException {
        List<MovieTag> tags = new ArrayList<>();
        List<UserRatingList> userActionLists = new ArrayList<>();
        MovieTag tag;
        UserRatingList actionList;
        for (Map.Entry<String, String> entry : preference.entrySet()) {
            tag = MovieTag.newBuilder().setTagId(Integer.parseInt(entry.getKey()))
                    .setRelevance(Double.parseDouble(entry.getValue())).build();
            tags.add(tag);
        }

        for(UserToMovie userToMovie1:userToMovie){
            actionList = UserRatingList.newBuilder().setMovieId(Integer.parseInt(userToMovie1.movieId))
                            .setRating(Double.parseDouble(userToMovie1.rating))
                            .setTimestamp(Integer.parseInt(userToMovie1.timestamp)).build();
            userActionLists.add(actionList);
        }

        userProfileMessage = UserProfileResponse.newBuilder().addAllTags(tags).addAllRatingList(userActionLists)
                        .setAverageRating(Double.parseDouble(avg_rating)).build();

        saveToRedis(user_id);
    }

    private void saveToRedis(String user_id) throws IOException {
        byte[] bytes = userProfileMessage.toByteArray();
        jedis.hset("userProfile".getBytes(),user_id.getBytes(),bytes);
        System.out.println(user_id + "--ok!");
    }
}
