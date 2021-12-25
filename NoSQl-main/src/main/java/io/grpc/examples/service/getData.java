package io.grpc.examples.service;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.util.*;

public class getData {
//    static Map<String, List<UserToMovie>> rating_table = new HashMap<>();
    static Map<String, Map<String, String>> genome_scores = new HashMap<>();

    public static void main(String[] args) throws SQLException, IOException {

        Connection conn = null;
        //nosql是数据库的名字
        String DB_URL = "jdbc:mysql://localhost:3306/nosql?useCursorFetch=true&defaultFetchSize=100";
        try {
            //加载驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");

            try {
                //填写账号密码
                conn = DriverManager.getConnection(DB_URL, "root", "mao20011002");
                //建立连接
                System.out.println("connect successfully!");//打印对象，看是否建立成功
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String sql = "select * from genome_scores;";


        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(Integer.MIN_VALUE);

        ResultSet rst = stmt.executeQuery(sql);

        String currentMovieId = "1";

        Map<String, String> map = new HashMap<>();

        while (rst.next()) {
            String movieId = rst.getString(1);
            if (!movieId.equals(currentMovieId)) {
                genome_scores.put(currentMovieId, map);
                map = new HashMap<>();
                currentMovieId = movieId;
            }

            map.put(rst.getString(2), rst.getString(3));
        }

        System.out.println("genome_scores:" + genome_scores.size());

        sql = "select * from train;";

        rst = stmt.executeQuery(sql);

        String currentId = "1";

        Map<String, String> user_profile = new HashMap<>();
        List<UserToMovie> userToMovies = new ArrayList<>();
        double avg_rating = 0;

        SaveMessage saveMessage = new SaveMessage();    //序列化存入redis

        System.out.println("start");

        int count = 1;

        while(rst.next()){
            String userId = rst.getString(1);
            if (userId.equals("99999")){
                double rating = Double.parseDouble(rst.getString(3));
                String movieId = rst.getString(2);

                UserToMovie userToMovie = new UserToMovie(movieId);
                userToMovie.setRating(rst.getString(3));
                userToMovie.setTimestamp(rst.getString(4));
                userToMovies.add(userToMovie);

                get_preference(movieId,rating,user_profile);
                avg_rating = avg_rating + rating;

            }

        }

        avg_rating = avg_rating/userToMovies.size();
        saveMessage.serializeMessage("99999",user_profile,userToMovies,String.valueOf(avg_rating));

        System.out.println("end");


//        System.out.println("end");
//        rst.close();
//        stmt.close();
//        conn.close();

    }


    private static void get_preference(String movieId, Double rating, Map<String, String> user_profile) {
        if(genome_scores.containsKey(movieId)) {
            for (Map.Entry<String, String> entry : genome_scores.get(movieId).entrySet()) {
                String movieTagId = entry.getKey();
                Double relevance = Double.parseDouble(entry.getValue());

                if (user_profile.containsKey(movieTagId)) {
                    user_profile.replace(movieTagId, (Double.parseDouble(user_profile.get(movieTagId)) + rating / 5.0 * relevance) + "");
                } else {
                    user_profile.put(movieTagId, (rating / 5.0 * relevance) + "");
                }
            }
        }
    }

}


