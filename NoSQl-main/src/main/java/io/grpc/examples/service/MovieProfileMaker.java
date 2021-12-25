/*
* 用于生成电影画像，并存在redis里
*
* 后期更新了用户电影画像，为其添加了其偏好的年份等信息
* */

package io.grpc.examples.service;

import com.csvreader.CsvReader;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieProfileResponse;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.UserProfileResponse;
import io.grpc.examples.helloworld.UserRatingList;
import redis.clients.jedis.Jedis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MovieProfileMaker {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);

    public static void main(String args[]) throws SQLException, InvalidProtocolBufferException {
        // 连接Redis
        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());

        // 连接mysql数据库读取基础文件
        Connection connection = MysqlConn();
//        Map<Integer,Double> MovieRatings = new HashMap<>();
//
//        MovieRatings = getAllMovieId(connection);
//        HashMap<Integer,List<MovieTag>> allMoviesTags = getAllMovieRelevance(connection,MovieRatings.keySet());
//        MovieRatings = getAllMovieAvgRating(connection,MovieRatings);
//
//        updateAllMovieProfile(connection,MovieRatings,allMoviesTags);

        // 更新用户/电影画像
        Map<String, String> years = movie_years();
//        addMovieYears(years);
//
        Set<String> userIds = getAllUserId(connection);
        addUserYears(years, userIds);
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

    // 得到所有的movieId
    public static Map<Integer, Double> getAllMovieId(Connection conn) throws SQLException {
        Map<Integer, Double> movieRatings = new HashMap<>();
        String sql = "select movieId from movies";

        ResultSet rs = do_sql(conn, sql);
        while (rs.next()) {
            movieRatings.put(Integer.parseInt(rs.getString(1)), 0.0);
        }
        System.out.println("已成功导入" + movieRatings.size() + "条MovieId！");

        return movieRatings;
    }

    // 得到所有的userId
    public static Set<String> getAllUserId(Connection conn) throws SQLException {
        Set<String> userIds = new HashSet<>();
        String sql = "select userId from train";

        ResultSet rs = do_sql(conn, sql);
        while (rs.next()) {
            userIds.add(rs.getString(1));
        }
        System.out.println("已成功导入" + userIds.size() + "条userId！");

        return userIds;
    }

    // 生成一个电影画像
    public static MovieProfileResponse buildMovieProfile(Connection conn, int movieId,
                                                         double rating, List<MovieTag> tags) throws SQLException {
        MovieTag tag;
        MovieProfileResponse movieProfile;
        String title = null, genres = null, tagId = null;
        double relevance = 0;
        HashMap<Integer, Double> tagsRelevances = new HashMap<>();
        ResultSet rs;

        String sql = "select title,genres from movies where movieId = " + movieId;
        rs = do_sql(conn, sql);
        while (rs.next()) {
            title = rs.getString(1);
            genres = rs.getString(2);
        }

        if (tags == null) {
            movieProfile = MovieProfileResponse.newBuilder()
                    .setMovieId(movieId).setRating(rating).setTitle(title).setGenres(genres)
                    .build();
        } else {
            movieProfile = MovieProfileResponse.newBuilder()
                    .setMovieId(movieId).addAllTags(tags).setRating(rating).setTitle(title).setGenres(genres)
                    .build();
        }

        return movieProfile;
    }

    // 将所有用户的画像导入redis
    public static void updateAllMovieProfile(Connection conn, Map<Integer, Double> movieRatings,
                                             Map<Integer, List<MovieTag>> allMovieTags) throws SQLException {
        MovieProfileResponse movieProfileResponse;
        Set<Integer> movieIds = movieRatings.keySet();
        int count = 0;          // 单纯用来看进度
        double rating = 0;
        byte[] bytes;

        for (Integer i : movieIds) {
            if (movieRatings.containsKey(i))
                rating = movieRatings.get(i);
            System.out.println("正在导入电影--- " + i + " ，其平均分为" + rating);
            movieProfileResponse = buildMovieProfile(conn, i, rating, allMovieTags.get(i));
            // 序列化MovieProfileResponse
            bytes = movieProfileResponse.toByteArray();

            /*
             * 因为jedis的hget方法的参数只能是三个string类型的或者三个byte[]类型的，
             * 但由于全是string类型在反序列化后的数据不一致，且会有超出最大大小的问题
             * 这里我们选择第二种，即传递三个byte[]
             * */
            jedis.hset("MovieProfiles".getBytes(), String.valueOf(i).getBytes(), bytes);
            count++;
            System.out.println("已成功导入第" + count + "条电影画像！");
        }

        System.out.println("成功导入全部电影画像！");
    }

    // 算出每个电影的平均分
    public static Map<Integer, Double> getAllMovieAvgRating(Connection conn, Map<Integer, Double> MovieRatings) throws SQLException {
        String sql = "select movieId,avg(rating) from train group by movieId";

        ResultSet rs = do_sql(conn, sql);
        while (rs.next()) {
            MovieRatings.put(Integer.parseInt(rs.getString(1)), rs.getDouble(2));
        }

        System.out.println("已成功导入" + MovieRatings.size() + "条电影评分！");
        return MovieRatings;
    }

    public static HashMap<Integer, List<MovieTag>> getAllMovieRelevance(Connection conn, Set<Integer> movieIds) throws SQLException {
        ResultSet rs = do_sql(conn, "select * from genome_scores");
        HashMap<Integer, List<MovieTag>> allMovieRelevance = new HashMap<>();
        List<MovieTag> tags = new ArrayList<>();
        MovieTag tag;

        // 先将所有的movieId对应的tagSet初始化null，因为只有部分电影有relevance信息
        for (Integer i : movieIds) {
            allMovieRelevance.put(i, null);
        }

        rs.next();
        while (true) {
            int movieId = Integer.parseInt(rs.getString(1));
            for (int i = 1; i <= 1128; i++) {
                tag = MovieTag.newBuilder().setTagId(Integer.parseInt(rs.getString(2))).setRelevance(rs.getDouble(3)).build();
                tags.add(tag);

                if (!rs.next()) {
                    allMovieRelevance.put(movieId, tags);
                    return allMovieRelevance;
                }
            }
            allMovieRelevance.put(movieId, new ArrayList<>(tags));
            tags.clear();
        }
    }

    // 获取所有电影发行的年份
    public static Map<String, String> movie_years() {
        String movieId, year, title;
        String filepath = "/Users/mhm1ng/study/2021-Nosql/ml-25m/movies.csv";
        Map<String, String> years = new HashMap<>();

        try {
            CsvReader reader = new CsvReader(filepath);
            reader.readHeaders();

            while (reader.readRecord()) {
                movieId = reader.get("movieId");
                title = reader.get("title");
                for (int i = title.length() - 1; i >= 0; i--) {
                    if (title.charAt(i) == ' ') {
                        title = title.substring(0, i);
                    } else {
                        break;
                    }
                }

                years.put(movieId, title);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("成功导入所有电影年份！");

        return years;
    }

    // 更新电影画像 --- 添加电影发行年份
    public static void addMovieYears(Map<String, String> yearMap) throws InvalidProtocolBufferException {
        Set<String> movieLists = yearMap.keySet();
        MovieProfileResponse response;
        MovieProfileResponse new_response;
        byte[] bytes;

        System.out.println("开始更新电影画像数据 --- 添加电影的年份");
        for (String s : movieLists) {
            String title = yearMap.get(s);
            String year;

            if (title.charAt(title.length() - 1) != ')' || title.charAt(title.length() - 2) > '9' ||
                    title.charAt(title.length() - 3) > '9' || title.charAt(title.length() - 4) > '9' ||
                    title.charAt(title.length() - 5) > '9' || title.charAt(title.length() - 6) != '(')
                year = "";
            else {
                year = title.substring(title.length() - 5, title.length() - 1);
                title = title.substring(0, title.length() - 6);
            }

            bytes = jedis.hget("MovieProfiles".getBytes(), s.getBytes());
            response = MovieProfileResponse.parseFrom(bytes);
            new_response = MovieProfileResponse.newBuilder().setMovieId(Integer.parseInt(s))
                    .setRating(response.getRating())
                    .setGenres(response.getGenres())
                    .setTitle(title)
                    .setPreferYear(year)
                    .addAllTags(response.getTagsList())
                    .addAllSortedTags(getMovieSortedTag(s))
                    .build();
            jedis.hset("MovieProfiles".getBytes(), s.getBytes(), new_response.toByteArray());
        }
        System.out.println("已成功添加所有的电影年份,添加了排序后的tags！");
    }

    // 更新用户画像 --- 添加电影年份
    public static void addUserYears(Map<String, String> yearMap, Set<String> userLists) throws InvalidProtocolBufferException {
        byte[] bytes,movieBytes;
        int actionListSize;
        int count = 0;
        String userYear = "";
        List<UserRatingList> actionList;
        UserProfileResponse response, new_response;
        MovieProfileResponse movieResponse;
        Map<String,Double> movieMaps = new HashMap<>();

        System.out.println("开始更新用户画像数据 --- 添加喜欢的电影的平均年份");
        for (String userId : userLists) {
            bytes = jedis.hget("userProfile".getBytes(), userId.getBytes());
            try {
                response = UserProfileResponse.parseFrom(bytes);
            } catch (NullPointerException e) {
                System.out.println(userId + "没有对应的用户画像...");
                continue;
            }

            actionList = response.getRatingListList();
            actionListSize = actionList.size();
            if (actionListSize != 0) {
                for (UserRatingList ulist : actionList) {
                    movieBytes = jedis.hget("MovieProfiles".getBytes(),String.valueOf(ulist.getMovieId()).getBytes());
                    movieResponse = MovieProfileResponse.parseFrom(movieBytes);

                    movieMaps.put(movieResponse.getPreferYear(),movieResponse.getRating());
                }
                userYear = getUserPreYear(movieMaps);
            }

            new_response = UserProfileResponse.newBuilder().setUserId(Long.parseLong(userId))
                    .setPreferYear(userYear)
                    .addAllTags(response.getTagsList())
                    .addAllRatingList(response.getRatingListList())
                    .setAverageRating(response.getAverageRating())
                    .addAllSortedTags(getUserSortedTag(userId))
                    .build();
            jedis.hset("userProfile".getBytes(), userId.getBytes(), new_response.toByteArray());
            count++;
            System.out.println(count + " / " + actionListSize);
        }
        System.out.println("已成功添加所有的用户喜欢的电影的平均年份,添加了排序后的tags！");
    }

    public static List<MovieTag> getUserSortedTag(String userId) throws InvalidProtocolBufferException {
        byte[] bytes = jedis.hget("userProfile".getBytes(), userId.getBytes());
        UserProfileResponse response = UserProfileResponse.parseFrom(bytes);
        List<MovieTag> tags = response.getTagsList();

        return sortTags(tags);
    }

    public static List<MovieTag> getMovieSortedTag(String movieId) throws InvalidProtocolBufferException {
        byte[] bytes = jedis.hget("MovieProfiles".getBytes(), movieId.getBytes());
        MovieProfileResponse response = MovieProfileResponse.parseFrom(bytes);
        List<MovieTag> tags = response.getTagsList();

        return sortTags(tags);
    }

    public static List<MovieTag> sortTags(List<MovieTag> tags) {
        List<MovieTag> ans = new ArrayList<>();
        HashMap<Long, Double> tagMaps = new HashMap<>();
        MovieTag movieTag;

        for (MovieTag tag : tags) {
            tagMaps.put(tag.getTagId(), tag.getRelevance());
        }
        List<Map.Entry<Long, Double>> en_map_list = new ArrayList<>(tagMaps.entrySet());

        Collections.sort(en_map_list, new Comparator<Map.Entry<Long, Double>>() {
            @Override
            public int compare(Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        for (Map.Entry<Long, Double> tag : en_map_list) {
            movieTag = MovieTag.newBuilder().setTagId(tag.getKey()).setRelevance(tag.getValue()).build();
            ans.add(movieTag);
        }

        return ans;
    }

    // 获取用户的年代偏好
    public static String getUserPreYear(Map<String, Double> years) {
        Set<String> perYears = years.keySet();
        Iterator it = perYears.iterator();
        String ans;
        if(!it.hasNext())
            return "";

        ans = it.next().toString();
        while(it.hasNext()){
            String tmp = it.next().toString();
            if(years.get(ans) < years.get(tmp)){
                ans = tmp;
            }
        }

        return ans;
    }
}
