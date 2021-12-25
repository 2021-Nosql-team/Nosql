package io.grpc.examples.service;

import com.csvreader.CsvWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieProfileResponse;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.UserProfileResponse;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class tagsToTags {
    static Jedis jedis = new Jedis("localhost", 6379, 10000);

    public static void main(String args[]) throws InvalidProtocolBufferException, SQLException {
        // 连接Redis
        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());

        createCsv();
    }

    // 获取所有的电影画像
    public static ArrayList<MovieProfileResponse> getAllMovies() throws SQLException, InvalidProtocolBufferException {
        ArrayList<MovieProfileResponse> movieList = new ArrayList<>();
        MovieProfileResponse response;
        byte[] bytes;
        Connection conn = MovieProfileMaker.MysqlConn();
        ResultSet rs = MovieProfileMaker.do_sql(conn, "select movieId from movies");

        while (rs.next()) {
            bytes = jedis.hget("MovieProfiles".getBytes(), rs.getString(1).getBytes());
            response = MovieProfileResponse.parseFrom(bytes);
            movieList.add(response);
        }

        return movieList;
    }

    // 获取用户相关度最高的五个标签
    public static MovieTag[] user_tags(List<MovieTag> userProfile) {
        MovieTag[] tags = new MovieTag[5];

        for (int i = 0; i < 5; i++) {
            tags[i] = MovieTag.newBuilder().setTagId(-1).setRelevance(0).build();
        }

        for (int i = 0; i < 1128; i++) {
            if (userProfile.get(i).getRelevance() > tags[4].getRelevance()) {
                tags[4] = MovieTag.newBuilder().setTagId(i).setRelevance(userProfile.get(i).getRelevance()).build();
                Arrays.sort(tags, a);
            }
        }

        return tags;
    }

    // 通过用户相关度最高的五个标签筛选电影
    public static ArrayList<Movie> user_movies(MovieTag[] tags, ArrayList<MovieProfileResponse> movies) {
        ArrayList<Movie> rec_movies = new ArrayList<>();

        // 将用户相关度最高的标签与电影的相关度*1.5 + 第二第三的标签*1.25 + 第四第五的标签*1 + 电影平均分/2，选出得分最高的五个电影
        for (MovieProfileResponse movie : movies) {
            double value = 0;
            for (int j = 0; j < tags.length; j++) {
                switch (j) {
                    case 0:
                        if (!movie.getTagsList().isEmpty())
                            value += 1.5 * movie.getTagsList().get((int) tags[j].getTagId()).getRelevance();
                        break;
                    case 1:
                    case 2:
                        if (!movie.getTagsList().isEmpty())
                            value += 1.25 * movie.getTagsList().get((int) tags[j].getTagId()).getRelevance();
                        break;
                    case 3:
                    case 4:
                        if (!movie.getTagsList().isEmpty())
                            value += 1 * movie.getTagsList().get((int) tags[j].getTagId()).getRelevance();
                        break;
                }
            }
            value += movie.getRating() / 2;
            rec_movies.add(new Movie(movie.getMovieId(), value));
        }

        rec_movies.sort(b);
        return rec_movies;
    }

    //重写比较方法将标签按照相关度排序
    private static final Comparator<MovieTag> a = (Comparator<MovieTag>) (t, t1) -> {
        return Double.compare(t.getRelevance(), t1.getRelevance()) * -1;
    };

    private static final Comparator<Movie> b = (Comparator<Movie>) (t, t1) -> {
        return Double.compare(t.value, t1.value) * -1;
    };

    public static void createCsv() throws SQLException, InvalidProtocolBufferException {
        String aim_path = "/Users/mhm1ng/study/2021-Nosql/recalls/tags_Movie.csv";
        String[] header = {"userId", "movie_1", "movie_2", "movie_3", "movie_4", "movie_5"};
        Set<byte[]> allUsers = jedis.hkeys("userProfile".getBytes());
        String[] content;
        int count = 0;

        ArrayList<MovieProfileResponse> allMovies = getAllMovies();

        System.out.println("正在生成用于根据标签召回的csv...");
        try {
            CsvWriter writer = new CsvWriter(aim_path, ',', Charset.forName("GBK"));
            writer.writeRecord(header);

            Iterator<byte[]> it = allUsers.iterator();
            while (it.hasNext()) {
                content = getContent(it.next(), allMovies);
                // 存在有用户没有tag,所以无召回结果
                if (content == null)
                    continue;
                writer.writeRecord(content);
                count++;
                System.out.println(count);
            }
            writer.close();

            System.out.println("根据标签召回的csv已生成！");
            System.out.println(count + " / " + allUsers.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getContent(byte[] userId, ArrayList<MovieProfileResponse> allMovies) throws InvalidProtocolBufferException, SQLException {
        String[] content = new String[6];
        content[0] = new String(userId);

        byte[] bytes = jedis.hget("userProfile".getBytes(), userId);
        UserProfileResponse userProfileResponse = UserProfileResponse.parseFrom(bytes);
        List<MovieTag> relevantMovies = userProfileResponse.getTagsList();

        if (relevantMovies.isEmpty())
            return null;

        ArrayList<Movie> movies_cf = user_movies(user_tags(relevantMovies), allMovies);
        for (int i = 0; i < 5; i++) {
            content[i + 1] = String.valueOf(movies_cf.get(i).moiveId);
        }

        return content;
    }
}
