/*
* 该文件在前期用于储存基础文件
* 后期基本不怎么使用了，因为储存方式不利于读取，后期换为了直接用mysql读取
* */

package io.grpc.examples.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvReader;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.helloworld.MovieProfileRequest;
import io.grpc.examples.helloworld.MovieProfileResponse;
import io.grpc.examples.helloworld.MovieTag;
import io.grpc.examples.helloworld.*;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisUtils {
    static Jedis jedis = new Jedis("localhost",6379,10000);

    public static void main(String[] args) throws IOException {

        System.out.println("OK");
        System.out.println("pong:" + jedis.ping());//测试数据库是否连接成功

        String path = "/Users/mhm1ng/study/2021-Nosql/ml-25m/top100.csv";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));//GBK
            //reader.readLine();//读取文件头
            String line = null;
            while((line=reader.readLine())!=null){
                if(line.equals("")) continue;
                String[] item = line.split(",");//CSV格式文件时候的分割符,我使用的是,号
                String mid = item[0];
                similarMovies.Builder sim_builder = similarMovies.newBuilder().setMovieId(Long.parseLong(mid));
                for(int i = 1;i < item.length; i++){
                    String[] subitem = item[i].split("_");
                    MovieRelevance relevance = MovieRelevance.newBuilder()
                            .setMovieId(Long.parseLong(subitem[0]))
                            .setRelevance(Double.parseDouble(subitem[1])).build();

                    sim_builder.addMovieRelevance(relevance);
                }
                //从文件中分离出相似度

                //Jedis 将relavance写入redis
                String key = "movie_sim_" + mid;
                System.out.println(sim_builder.build());
                jedis.set(mid.getBytes(StandardCharsets.UTF_8),sim_builder.build().toByteArray());
            }//初始化movie_seen
            System.out.println("top100初始化完毕");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<MovieRelevance> getSimMovies(String mid){
        byte[] bytes = jedis.get(mid.getBytes(StandardCharsets.UTF_8));
        ArrayList<MovieRelevance> sims = new ArrayList<>();
        try {
            similarMovies movies = similarMovies.parseFrom(bytes);
            sims.addAll(movies.getMovieRelevanceList());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return sims;
    }

    public static HashMap<String,Double> getMovieTags(String movieId){
        if(!jedis.isConnected()) return null;

        // 获取总的标签数量
        Long tagsNum = jedis.hlen("genomeTags");
        JsonObject json_key;
        List<MovieTag> tags;
        HashMap<String,Double> tagRelevance = new HashMap<String,Double>();

        // 将该电影对应所有标签的相似度放在一个hashmap里,tagId为Key,relevance为Value
        for(int i = 1;i <= tagsNum;i++){
            json_key = new JsonObject();
            json_key.addProperty("movieId",movieId);
            json_key.addProperty("tagId",String.valueOf(i));
            tagRelevance.put(String.valueOf(i),Double.parseDouble(jedis.hget("genomeScores",json_key.toString())));
        }

//        tagRelevance = sortHashmap(tagRelevance);
//        Set<String> keySets = tagRelevance.keySet();
//
//        for(String keySet:keySets){
//            System.out.println(jedis.hget("genomeTags",keySet) + " : " + tagRelevance.get(keySet));
//        }

        return tagRelevance;
    }

    public static void saveMovieTagsIntoRedis(){
        String filePath = "D:\\2021 Nosql\\ml-25m\\genome-scores.csv";
        String movieId = "0";
        int count = 0;
        HashMap<String,Double> map = new HashMap<String ,Double>();

        try {
            // 创建csv读对象
            CsvReader reader = new CsvReader(filePath);

            // 读表头
            reader.readHeaders();
            while(reader.readRecord()){
                if(!Objects.equals(movieId, reader.get("movieId"))){
                    movieId = reader.get("movieId");
                    map = getMovieTags(movieId);

                    // 查看有多少导入成功
                    if(import_movieTags(movieId,map)){
                        count++;
                        System.out.println(count + "--" + movieId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对HashMap进行排序，取出相似度最高的前五名
     *
     * @param myMap 储存了tagId和relevance的HashMap
     * @return true插入成功
     */
    public static HashMap<String,Double> sortHashmap(HashMap<String,Double> myMap){
        List<Map.Entry<String,Double>> list = new ArrayList<Map.Entry<String, Double>>(myMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        myMap.clear();

        for (int i = 0; i < 5; i++) {
            myMap.put(list.get(i).getKey(),list.get(i).getValue());
        }

        return myMap;
    }

    /**
     * 导入用户画像
     *
     * @param userId
     * @param profileMap 用户画像标签
     * @return true插入成功
     */
    public static boolean import_userProfile(String userId, HashMap<String, String> profileMap) {
        if (!jedis.isConnected()) return false;
        jedis.hset("userProfile", userId, JSON.toJSONString(profileMap));
        return true;
    }

    /**
     * 查询用户画像
     *
     * @param userId
     * @param profileMap 用户画像标签
     * @return true 查询成功
     */
    public static boolean get_userProfile(String userId, HashMap<String, String> profileMap) {
        if (!jedis.isConnected()) return false;
        String feature = jedis.hget("userProfile", userId);
        JSONObject json = JSONObject.parseObject(feature);
        if (json != null) {
            Set<String> keys = json.keySet();
            for (String key : keys) {
                String value = json.getString(key);
                profileMap.put(key, value);
            }
            return true;
        }
        return false;
    }

    /**
     * @param movieId    电影ID
     * @param profileMap 电影与各标签关联度
     * @return 返回电影评分 返回-1表示查询失败，
     */
    public static double get_movieProfile(String movieId, HashMap<String, Double> profileMap) {
        if (!jedis.isConnected()) return -1;

        String feature = jedis.hget("movieTags", movieId);
        JSONObject json = JSONObject.parseObject(feature);
        if (json != null) {
            Set<String> keys = json.keySet();
            for (String key : keys) {
                Double value = Double.parseDouble(json.getString(key));
                profileMap.put(key, value);
            }
        }
        double rating;
        try {
            rating = jedis.zscore("movieRatings", movieId);
        } catch (NullPointerException e) {
            rating = -1;
        }

        return rating;
    }

    /**
     * 导入电影与标签关联度，将hashmap转换为JSON,之后取出来也好解析。
     *
     * @param movieId    电影ID
     * @param profileMap 与各标签关联度的表
     * @return true导入成功
     */
    public static boolean import_movieTags(String movieId, HashMap<String, Double> profileMap) {
        if (!jedis.isConnected()) return false;
        jedis.hset("movieTags", movieId, JSON.toJSONString(profileMap));
        return true;
    }

    public static boolean import_movieRating(String movieId, double rating) {
        if (!jedis.isConnected()) return false;
        jedis.zadd("movieRatings", rating, movieId);
        return true;
    }

    public static void import_movies(Jedis jedis, long lastNumber, String movies_path) {
        long linenumber = 0;
        try (FileInputStream inputStream = new FileInputStream(movies_path);

             Scanner sc = new Scanner(inputStream)) {
            /**
             * 读取csv文件
             * 因为文件过大，所以需要用scanner每行读取
             * 用FileInputStream 相当于在内存和文件之见加了一个数据传输管道
             * 然后用Scanner去读取这个流，一行一行的去读这个文件
             */
            //读取头部
            if (sc.hasNextLine()) System.out.println(sc.nextLine());
            String title;
            String movieID;
            String genres;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isEmpty()) continue;
                //文件是CSV文件，CSV文件中的每一列是用","隔开的，这样就可以得到每一列的元素
                linenumber++;
                if (linenumber < lastNumber) continue;
                String[] strArray = line.split(",");
                movieID = strArray[0];
                title = strArray[1];
                genres = strArray[2];
//                将movieID作为键，title和genres作为值
                JsonObject json_value = new JsonObject();
                json_value.addProperty("title", title);
                json_value.addProperty("genres", genres);
                jedis.hset("movies", movieID, json_value.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("将movies_last的改为" + linenumber);
        } finally {
            System.out.println("如果你没看到修改movies_last的提示则表示导入movies成功，可以在main中注释掉import_movies方法");
        }
    }

    public static void import_genomeTags(Jedis jedis, long lastNumber, String genomeTags_path) {
        long linenumber = 0;
        try (FileInputStream inputStream = new FileInputStream(genomeTags_path);
             Scanner sc = new Scanner(inputStream)) {
            /**
             * 读取csv文件
             * 因为文件过大，所以需要用scanner每行读取
             * 用FileInputStream 相当于在内存和文件之见加了一个数据传输管道
             * 然后用Scanner去读取这个流，一行一行的去读这个文件
             */
            //读取头部
            if (sc.hasNextLine()) System.out.println(sc.nextLine());
            String tagID;
            String tag;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isEmpty()) continue;
                //文件是CSV文件，CSV文件中的每一列是用","隔开的，这样就可以得到每一列的元素
                linenumber++;
                if (linenumber < lastNumber) continue;
                String[] strArray = line.split(",");
                tagID = strArray[0];
                tag = strArray[1];
//                将tagID作为键，tag作为值
                jedis.hset("genomeTags", tagID, tag);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("将genomeTags_last的改为" + linenumber);
        } finally {
            System.out.println("如果你没看到修改genomeTags_last的提示则表示导入genomeTags成功，可以在main中注释掉import_genomeTags方法");
        }
    }

    public static void import_genomeScores(Jedis jedis, long lastNumber, String genomeScores_path) {
        long linenumber = 0;
        try (FileInputStream inputStream = new FileInputStream(genomeScores_path);
             Scanner sc = new Scanner(inputStream)) {
            /**
             * 读取csv文件
             * 因为文件过大，所以需要用scanner每行读取
             * 用FileInputStream 相当于在内存和文件之见加了一个数据传输管道
             * 然后用Scanner去读取这个流，一行一行的去读这个文件
             */
            //读取头部
            if (sc.hasNextLine()) System.out.println(sc.nextLine());
            String movieID;
            String tagID;
            String relevance;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isEmpty()) continue;
                //文件是CSV文件，CSV文件中的每一列是用","隔开的，这样就可以得到每一列的元素
                linenumber++;
                if (linenumber < lastNumber) continue;
                String[] strArray = line.split(",");
                movieID = strArray[0];
                tagID = strArray[1];
                relevance = strArray[2];
//                将tagID和movieID作为键，relevance作为值
                JsonObject json_value = new JsonObject();
                JsonObject json_key = new JsonObject();
                json_key.addProperty("movieId", movieID);
                json_key.addProperty("tagId", tagID);
                jedis.hset("genomeScores", json_key.toString(), relevance);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("将genomeScores_last的改为" + linenumber);
        } finally {
            System.out.println("如果你没看到修改genomeScores_last的提示则表示导入genomeScores成功，可以在main中注释掉import_genomeScores方法");
        }
    }

    public static void import_links(Jedis jedis, long lastNumber, String links_path) {
        long linenumber = 0;
        try (FileInputStream inputStream = new FileInputStream(links_path);

             Scanner sc = new Scanner(inputStream)) {
            /**
             * 读取csv文件
             * 因为文件过大，所以需要用scanner每行读取
             * 用FileInputStream 相当于在内存和文件之见加了一个数据传输管道
             * 然后用Scanner去读取这个流，一行一行的去读这个文件
             */
            //读取头部
            if (sc.hasNextLine()) System.out.println(sc.nextLine());
            String movieID;
            String imdbID;
            String tmdbID;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isEmpty()) continue;
                //文件是CSV文件，CSV文件中的每一列是用","隔开的，这样就可以得到每一列的元素
                linenumber++;
                if (linenumber < lastNumber) continue;
                String[] strArray = line.split(",");
                movieID = strArray[0];
                imdbID = strArray[1];
                if (strArray.length < 3) {
                    tmdbID = "";
                } else {
                    tmdbID = strArray[2];
                }
//                将movieID作为键，title和genres作为值
                JsonObject json_value = new JsonObject();
                json_value.addProperty("imdbId", imdbID);
                json_value.addProperty("tmdbId", tmdbID);
                jedis.hset("links", movieID, json_value.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("将links_last的改为" + linenumber);
        } finally {
            System.out.println("如果你没看到修改links_last的提示则表示导入links成功，可以在main中注释掉import_links方法");
        }
    }
}


