/*
* 该文件用于生成模型的训练集和测试集
* */


package io.grpc.examples.service;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieProfileResponse;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.UserProfileResponse;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class createModelCsv {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);

    public static void main(String[] args) throws InvalidProtocolBufferException {
        // 连接Redis
        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());

        String[] headers = {"label", "userId", "movieId", "userAvgRating", "userPreferYear",
                "userRelTag1", "userRelTag2", "userRelTag3", "movieAvgRating", "movieYear",
                "movieRelTag1", "movieRelTag2", "movieRelTag3"};

        create_csv(headers);
    }

    public static String[] create_content(String userId, String movieId, Double rating) throws InvalidProtocolBufferException {
        String[] content = new String[13];

        try {
            byte[] bytes = jedis.hget("MovieProfiles".getBytes(), movieId.getBytes());
            MovieProfileResponse movieResponse = MovieProfileResponse.parseFrom(bytes);
            bytes = jedis.hget("userProfile".getBytes(), userId.getBytes());
            UserProfileResponse userResponse = UserProfileResponse.parseFrom(bytes);
            List<MovieTag> user_tags = userResponse.getSortedTagsList();
            List<MovieTag> movie_tags = movieResponse.getSortedTagsList();

            if (rating >= 4)
                content[0] = "1";
            else
                content[0] = "0";

            content[1] = userId;
            content[2] = movieId;
            content[3] = String.valueOf(userResponse.getAverageRating());
            content[4] = userResponse.getPreferYear();
            content[8] = String.valueOf(movieResponse.getRating());
            content[9] = movieResponse.getPreferYear();

            // 存在用户或电影没有相似标签的情况
            if (user_tags.isEmpty()) {
                content[5] = "";
                content[6] = "";
                content[7] = "";
            } else {
                content[5] = String.valueOf(user_tags.get(0).getTagId());
                content[6] = String.valueOf(user_tags.get(1).getTagId());
                content[7] = String.valueOf(user_tags.get(2).getTagId());
            }

            if (movie_tags.isEmpty()) {
                content[10] = "";
                content[11] = "";
                content[12] = "";
            } else {
                content[10] = String.valueOf(movie_tags.get(0).getTagId());
                content[11] = String.valueOf(movie_tags.get(1).getTagId());
                content[12] = String.valueOf(movie_tags.get(2).getTagId());
            }
        }catch (NullPointerException e){
            System.out.println(userId + "没有用户画像...");
        }

        return content;
    }

    public static void create_csv(String[] header) {
        String filePath = "/Users/mhm1ng/study/2021-Nosql/ml-25m/new_train.csv";
        String ori_path = "/Users/mhm1ng/study/2021-Nosql/ml-25m/train.csv";
        String[] content;
        int count = 0;

        System.out.println("正在生成用于训练模型的csv...");
        try {
            CsvWriter writer = new CsvWriter(filePath, ',', Charset.forName("GBK"));
            CsvReader reader = new CsvReader(ori_path);
            writer.writeRecord(header);

            reader.readHeaders();
            while (reader.readRecord()) {
                content = create_content(reader.get("userId"), reader.get("movieId"),
                        Double.parseDouble(reader.get("rating")));
                writer.writeRecord(content);
                count++;
                System.out.println(count + "/20000076");
            }
            reader.close();
            writer.close();

            System.out.println("用于训练模型的csv已生成！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
