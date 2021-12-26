/*
 * @author: Haoyang Lee
 * @Function: Read the redis database and read the train.csv get usr profile
 *              and usr-movie map to recommend movies to users
 * @Note: This is just a demo program, the real run program is somewhere different from this file
 */
package io.grpc.examples.service;

import com.csvreader.CsvReader;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.examples.pb.MovieTag;
import io.grpc.examples.pb.UserProfileResponse;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;

public class tmp {
    static Jedis jedis = new Jedis("localhost");

    public static void main(String args[]) throws InvalidProtocolBufferException {
        // 首先从redis里面读一下用户画像信息，对每一个用户形成一个字符串予以描述
        try {
            save_usrProfile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<Integer, String> usrProfile = new HashMap<>();
        Map<String, Map<String, Double>> usr_mv = new HashMap<>();
        System.out.println("建立用户-电影表");
        try{
            CsvReader csvReader = new CsvReader("/Users/haoyangli/Downloads/train-1.csv");
            csvReader.readHeaders();
            while(csvReader.readRecord()) {
                String movie_id = csvReader.get("movieId");
                if(Objects.equals(movie_id, "")) {
                    continue;
                }
                String usr_id = csvReader.get("userId");
                Double rating = Double.parseDouble(csvReader.get("rating"));
                if(usr_mv.containsKey(usr_id)) {
                    Map<String, Double> data = usr_mv.get(usr_id);
                    data.put(movie_id, rating);
                    usr_mv.put(usr_id, data);
                }else{
                    Map<String, Double> data = new HashMap<>();
                    data.put(movie_id, rating);
                    usr_mv.put(usr_id, data);
                }
            }
        }catch(IOException e){}
        System.out.println("建立用户画像");
        for(int i=0; i<162541; i++) {
            usrProfile.put(i+1, getUserProfile(i+1));
        }
        System.out.println("计算单个用户电影推荐用时");
        long startTime = System.currentTimeMillis();
        long finishTime = 0;
        Map<Integer, Double> similarity = new HashMap<>();
        for(int i=0; i<162541; i++) {
            similarity.put(i+1, getUserSimilarity(usrProfile.get(1), usrProfile.get(i+1)));
        }
        finishTime = System.currentTimeMillis() - startTime;
        Map<Integer, Double> result = sortMap(similarity);
        Map<String, Double> rank = new HashMap<>();
        int count = 0;
        for(Map.Entry<Integer, Double> entry: result.entrySet()) {
            if(count == 3) {
                break;
            }
            String usr = Integer.toString(entry.getKey());
            Double usr_point = entry.getValue();
            Map<String, Double> movies = usr_mv.get(usr);
            for(Map.Entry<String, Double> inner: movies.entrySet()) {
                rank.put(inner.getKey(), inner.getValue() * usr_point);
            }
        }
        rank = sortMap_s(rank);
        for(Map.Entry<String, Double> entry: rank.entrySet()) {
            if(count == -2) {
                break;
            }
            System.out.println("Movie:" + entry.getKey() + "\tScore:" + entry.getValue());
            count-=1;
        }
        System.out.println("单个用户相似度计算成功");
        System.out.println("计算用时：" + finishTime + "ms");
    }
    //从Redis数据库读取用户画像信息，输入用户ID返回用户1128位用户画像
    public static String getUserProfile(int userId) throws InvalidProtocolBufferException {
        String usr_profile_str = "";
        byte[] bytes = jedis.hget("userProfile".getBytes(),String.valueOf(userId).getBytes());
        try {
            UserProfileResponse response = UserProfileResponse.parseFrom(bytes);
            List<MovieTag> tag = response.getTagsList();
            for(int i=0; i< tag.size(); i++) {
                if(tag.get(i).getRelevance()>10) {
                    usr_profile_str += "1";
                }else{
                    usr_profile_str += "0";
                }
            }
        }catch (NullPointerException e) {
            System.out.println("Bytes:\t" + Arrays.toString(bytes));
            System.out.println("UserId:\t" + userId);
            for(int i=0; i<1128; i++) {
                usr_profile_str += "0";
            }
        }
        return usr_profile_str;
    }
    //输入两个用户等用户画像返回相似度
    public static double getUserSimilarity(String usr_a, String usr_b) {
        int count = 0;
        char [] usr_1 = usr_a.toCharArray();
        char [] usr_2 = usr_b.toCharArray();
        try{
        for(int i=0; i<1128; i++) {
            count += Math.abs(usr_1[i]-usr_2[i]);
        }}catch(ArrayIndexOutOfBoundsException e) {
            return 0;
        }
        return 1- count / 1128.0;
    }
    public static Map<Integer, Double> sortMap(Map<Integer, Double> oldMap) {
        ArrayList<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(oldMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {

            @Override
            public int compare(Map.Entry<Integer, Double> arg0,
                               Map.Entry<Integer, Double> arg1) {
                return (int) (arg1.getValue() - arg0.getValue());
            }
        });
        Map<Integer, Double> newMap = new LinkedHashMap<Integer, Double>();
        for (int i = 0; i < list.size(); i++) {
            newMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return newMap;
    }

    public static Map<String, Double> sortMap_s(Map<String, Double> oldMap) {
        ArrayList<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(oldMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {

            @Override
            public int compare(Map.Entry<String, Double> arg0,
                               Map.Entry<String, Double> arg1) {
                return (int) (arg1.getValue() - arg0.getValue());
            }
        });
        Map<String, Double> newMap = new LinkedHashMap<String, Double>();
        for (int i = 0; i < list.size(); i++) {
            newMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return newMap;
    }

    public static void save_usr_movie() {
        try{
            FileOutputStream fout = new FileOutputStream("/Users/haoyangli/usr_movie.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            Map<String, Map<String, Double>> usr_mv = new HashMap<>();
            CsvReader csvReader = new CsvReader("/Users/haoyangli/Downloads/train-1.csv");
            csvReader.readHeaders();
            while(csvReader.readRecord()) {
                String movie_id = csvReader.get("movieId");
                if(Objects.equals(movie_id, "")) {
                    continue;
                }
                String usr_id = csvReader.get("userId");
                Double rating = Double.parseDouble(csvReader.get("rating"));
                if(usr_mv.containsKey(usr_id)) {
                    Map<String, Double> data = usr_mv.get(usr_id);
                    data.put(movie_id, rating);
                    usr_mv.put(usr_id, data);
                }else{
                    Map<String, Double> data = new HashMap<>();
                    data.put(movie_id, rating);
                    usr_mv.put(usr_id, data);
                }
            }
            oos.writeObject(usr_mv);
            System.out.println("Done!");
        }catch(IOException e){
            //null
        }
    }

    public static Map<String, Double> get_usr_movie() {         // 读取用户-电影表的序列化文件
        FileInputStream f_in = null;
        ObjectInputStream o_in = null;
        Map<String, Double> result = null;
        try{
            f_in = new FileInputStream("/Users/haoyangli/usr_movie.ser");
            o_in = new ObjectInputStream(f_in);
            result = (Map<String, Double>) o_in.readObject();
            f_in.close();
            o_in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void save_usrProfile() throws IOException {       // 获取所有的用户画像并将之序列化到文件中便于后期使用
        Map<Integer, String> usrProfile = new HashMap<>();
        System.out.println("建立用户画像");
        for(int i=0; i<162541; i++) {
            usrProfile.put(i+1, getUserProfile(i+1));
        }
        System.out.println("处理完毕，进行存储");
        FileOutputStream fout = new FileOutputStream("/Users/haoyangli/usr_profile.ser");
        ObjectOutputStream oout = new ObjectOutputStream(fout);
        oout.writeObject(usrProfile);
        oout.close();
        fout.close();
    }
}
