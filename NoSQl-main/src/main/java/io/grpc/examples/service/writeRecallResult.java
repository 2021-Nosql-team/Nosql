/*
* 将召回结果存在redis里，供系统调用时使用
*
* */

package io.grpc.examples.service;

import com.csvreader.CsvReader;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.RecallResult;
import io.grpc.examples.helloworld.UserProfileResponse;
import io.grpc.examples.helloworld.UserRatingList;
import org.python.antlr.ast.Str;
import redis.clients.jedis.Jedis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class writeRecallResult {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);

    public static void main(String[] args) throws InvalidProtocolBufferException {
        // 连接Redis
        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());

        String ucf_path = "/Users/mhm1ng/study/2021-Nosql/recalls/ucf.csv";
        String icf_path = "/Users/mhm1ng/study/2021-Nosql/recalls/icf.csv";
        String matrix_path = "/Users/mhm1ng/study/2021-Nosql/recalls/matrix.csv";
        String tags_path = "/Users/mhm1ng/study/2021-Nosql/recalls/tags_Movie.csv";

//        loadRecalls(ucf_path);
//        getRecallNum();
//        loadRecalls(matrix_path);
//        getRecallNum();
//        loadRecalls(icf_path);
//        getRecallNum();
//        loadRecalls(tags_path);
        getRecallNum();
        removeTheSame();
        getRecallNum();
    }

    public static void loadRecalls(String filePath) {
        RecallResult recall, new_recall;
        String userId;
        String newMovieId;
        byte[] bytes;
        boolean repeat = false;
        List<String> newRecallMovie;
        List<String> oldRecallMovie;
        List<String> aimMovie;

        try {
            CsvReader reader = new CsvReader(filePath);
            reader.readHeaders();

            System.out.println("开始导入召回结果...");
            while (reader.readRecord()) {
                userId = reader.get("userId");
                newRecallMovie = new ArrayList<>();
                aimMovie = new ArrayList<>();
                newRecallMovie.add(reader.get("movie_1"));
                newRecallMovie.add(reader.get("movie_2"));
                newRecallMovie.add(reader.get("movie_3"));
                newRecallMovie.add(reader.get("movie_4"));
                newRecallMovie.add(reader.get("movie_5"));
                bytes = jedis.hget("RecallResult".getBytes(), userId.getBytes());

                // 之前没有对应user的召回结果
                if (bytes == null) {
                    new_recall = RecallResult.newBuilder().setUserId(reader.get("userId"))
                            .addAllMovieId(newRecallMovie).build();
                    jedis.hset("RecallResult".getBytes(), userId.getBytes(), new_recall.toByteArray());
                } else {    // 之前已经有召回结果
                    recall = RecallResult.parseFrom(bytes);
                    oldRecallMovie = recall.getMovieIdList();
                    for (int i = 0; i < 5; i++) {
                        for (String movieId : oldRecallMovie) {
                            newMovieId = newRecallMovie.get(i);
                            if (Objects.equals(movieId, newMovieId)) {
                                repeat = true;
                                break;
                            }
                        }
                        if (!repeat) {
                            aimMovie.add(newRecallMovie.get(i));
                        }
                        repeat = false;
                    }
                    new_recall = RecallResult.newBuilder().setUserId(reader.get("userId"))
                            .addAllMovieId(oldRecallMovie).addAllMovieId(aimMovie).build();
                    jedis.hset("RecallResult".getBytes(), userId.getBytes(), new_recall.toByteArray());
                }
            }
            System.out.println("成功导入召回结果！");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从召回结果里删去用户看过的电影
    public static void removeTheSame() throws InvalidProtocolBufferException {
        Set<byte[]> allRecallUsers = jedis.hkeys("RecallResult".getBytes());
        Iterator<byte[]> it = allRecallUsers.iterator();
        byte[] userBytes, recallBytes, userId;
        UserProfileResponse userProfile;
        RecallResult rr,new_rr;
        List<UserRatingList> oldMovieList;
        List<String> recallMovieList;
        List<String> aimMovie;
        boolean repeat = false;

        while (it.hasNext()) {
            userId = it.next();
            aimMovie = new ArrayList<>();
            userBytes = jedis.hget("userProfile".getBytes(), userId);
            userProfile = UserProfileResponse.parseFrom(userBytes);
            oldMovieList = userProfile.getRatingListList();

            recallBytes = jedis.hget("RecallResult".getBytes(), userId);
            rr = RecallResult.parseFrom(recallBytes);
            recallMovieList = rr.getMovieIdList();

            for (String rcMovie : recallMovieList) {
                for(UserRatingList oldMovie: oldMovieList){
                    if(Objects.equals(rcMovie,oldMovie.getMovieId())){
                        System.out.println(rr.getUserId());
                        repeat = true;
                        break;
                    }
                }
                if(!repeat){
                    aimMovie.add(rcMovie);
                }
                repeat = false;
            }

            new_rr = RecallResult.newBuilder().setUserId(rr.getUserId()).addAllMovieId(aimMovie).build();
            jedis.hset("RecallResult".getBytes(),userId,new_rr.toByteArray());
        }
    }

    // 得到去重之后用户的召回电影的数量分布
    public static void getRecallNum() throws InvalidProtocolBufferException {
        Set<byte[]> allRecallUsers = jedis.hkeys("RecallResult".getBytes());
        byte[] userId;
        byte[] bytes;
        RecallResult rr;
        int min = 10;
        int max = 5;
        int[] count = new int[16];

        Iterator<byte[]> it = allRecallUsers.iterator();
        while (it.hasNext()) {
            userId = it.next();
//            jedis.hdel("RecallResult".getBytes(), userId);
            bytes = jedis.hget("RecallResult".getBytes(), userId);
            rr = RecallResult.parseFrom(bytes);
            count[rr.getMovieIdCount() - 5]++;
        }

        for (int i = 0; i < 16; i++) {
            System.out.println(i + 5 + " --- " + count[i]);
        }
    }
}
