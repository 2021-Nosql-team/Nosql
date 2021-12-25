/*
* 根据召回结果生成模型预测所需要的csv文件
* */

package io.grpc.examples.service;

import com.csvreader.CsvWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieProfileResponse;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.RecallResult;
import io.grpc.examples.helloworld.UserProfileResponse;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;

public class RecallToModel {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);

    public static void main(String[] args) throws InvalidProtocolBufferException, SQLException {
        // 连接Redis
        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());

    }


    public static void create_prediction_csv(String userId) throws InvalidProtocolBufferException {
        byte[] bytes;
        RecallResult rr;
        List<String> movies;
        String filePath = "/Users/mhm1ng/study/2021-Nosql/model_toPrediction.csv";
        String[] content;
        String[] headers = {"label","userId", "movieId", "userAvgRating", "userPreferYear",
                "userRelTag1", "userRelTag2", "userRelTag3", "movieAvgRating", "movieYear",
                "movieRelTag1", "movieRelTag2", "movieRelTag3"};

        List<String> hotMovieList = getHotMovies();

        try {
            CsvWriter writer = new CsvWriter(filePath, ',', Charset.forName("GBK"));
            writer.writeRecord(headers);

            bytes = jedis.hget("RecallResult".getBytes(), userId.getBytes());

            // 如果一个用户是新用户
            if (bytes == null) {
                movies = hotMovieList;
            } else {
                rr = RecallResult.parseFrom(bytes);
                movies = rr.getMovieIdList();
            }

            for (String movieId : movies) {
                content = getContent(userId, movieId);
                writer.writeRecord(content);
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getContent(String userId, String movieId) throws InvalidProtocolBufferException {
        byte[] userBytes = jedis.hget("userProfile".getBytes(), userId.getBytes());
        byte[] movieBytes = jedis.hget("MovieProfiles".getBytes(), movieId.getBytes());
        MovieProfileResponse movieProfile = MovieProfileResponse.parseFrom(movieBytes);
        String[] content = new String[13];
        List<MovieTag> movieTags = movieProfile.getSortedTagsList();

        // 如果不是新用户
        if (userBytes != null) {
            UserProfileResponse userProfile = UserProfileResponse.parseFrom(userBytes);
            List<MovieTag> userTags = userProfile.getSortedTagsList();
            content[3] = String.valueOf(userProfile.getAverageRating());
            content[4] = userProfile.getPreferYear();

            if (userTags.isEmpty()) {
                content[5] = "0";
                content[6] = "0";
                content[7] = "0";
            } else {
                content[5] = String.valueOf(userTags.get(0).getTagId());
                content[6] = String.valueOf(userTags.get(1).getTagId());
                content[7] = String.valueOf(userTags.get(2).getTagId());
            }
        } else {
            content[3] = "0";
            content[4] = "0";
            content[5] = "0";
            content[6] = "0";
            content[7] = "0";
        }

        content[0] = "1";
        content[1] = userId;
        content[2] = movieId;
        content[8] = String.valueOf(movieProfile.getRating());
        content[9] = movieProfile.getPreferYear();

        if (movieTags.isEmpty()) {
            content[10] = "0";
            content[11] = "0";
            content[12] = "0";
        } else {
            content[10] = String.valueOf(movieTags.get(0).getTagId());
            content[11] = String.valueOf(movieTags.get(1).getTagId());
            content[12] = String.valueOf(movieTags.get(2).getTagId());
        }

        return content;
    }

    // 获取前五个热门的电影，推给没有行为历史的新用户
    public static List<String> getHotMovies() throws InvalidProtocolBufferException {
        HashMap<String, Double> movieRatings = new HashMap<>();
        List<String> ans = new ArrayList<>();
        MovieProfileResponse movieProfile;
        Set<byte[]> movieByteSet = jedis.hkeys("MovieProfiles".getBytes());
        byte[] movieByte;

        Iterator<byte[]> it = movieByteSet.iterator();
        while (it.hasNext()) {
            movieByte = jedis.hget("MovieProfiles".getBytes(),it.next());
            movieProfile = MovieProfileResponse.parseFrom(movieByte);
            movieRatings.put(String.valueOf(movieProfile.getMovieId()), movieProfile.getRating());
        }

        List<Map.Entry<String, Double>> hotMovieList = new ArrayList<>(movieRatings.entrySet());
        Collections.sort(hotMovieList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        for (Map.Entry<String, Double> hotMovie : hotMovieList) {
            ans.add(hotMovie.getKey());
            if (ans.size() == 20)
                break;
        }

        return ans;
    }

    // 连接mysql
    public static Connection MysqlConn() {
        Connection conn = null;

        try {
            //加载驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");

            //nosql是数据库的名字
            String DB_URL = "jdbc:mysql://localhost:3306/nosql?useSSL=false&serverTimezone=Asia/Shanghai";
            try {
                //填写账号密码
                conn = DriverManager.getConnection(DB_URL, "root", "mao20011002");
                //建立连接
                System.out.println("Connect with the mysql server successfully!");//打印对象，看是否建立成功
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return conn;
    }

    public static ResultSet do_sql(Connection conn, String sql) {
        PreparedStatement statement;
        ResultSet rs = null;

        try {
            // 执行传入的sql语句
            statement = conn.prepareStatement(sql);
            // 得到结果集
            rs = statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rs;
    }

    // 工具类函数，在生成20%用户的推测结果时使用到了
    public static List<String> getAllUserId(Connection conn) throws SQLException {
        List<String> allUserIds = new ArrayList<>();
        String sql = "select distinct userId from test";

        ResultSet rs = do_sql(conn, sql);
        while (rs.next()) {
            allUserIds.add(rs.getString(1));
        }
        System.out.println("已成功导入" + allUserIds.size() + "条userId！");

        return allUserIds;
    }
}
