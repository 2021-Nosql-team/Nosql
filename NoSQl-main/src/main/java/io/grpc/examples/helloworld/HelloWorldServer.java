/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.service.RecallToModel;
import io.grpc.stub.StreamObserver;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());
    static Jedis jedis = new Jedis("localhost", 6379, 10000);
    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    HelloWorldServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("pong:" + jedis.ping());
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void getUserProfile(UserProfileRequest request,
                                   StreamObserver<UserProfileResponse> responseObserver) {
            UserProfileResponse response = UserProfileResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getMovieProfile(MovieProfileRequest request,
                                    StreamObserver<MovieProfileResponse> responseObserver) {
            MovieProfileResponse movieProfileResponse;

            byte[] bytes = jedis.hget("MovieProfiles".getBytes(), String.valueOf(request.getMovieId()).getBytes());
            try {
                movieProfileResponse = MovieProfileResponse.parseFrom(bytes);
                responseObserver.onNext(movieProfileResponse);
                responseObserver.onCompleted();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void getRecommendMovies(RecommendRequest request,
                                       StreamObserver<RecommendResponse> responseObserver) {
            RecommendResponse response;

            try {
                RecallToModel.create_prediction_csv(request.getUserId());

                logger.info("Server has received the request");

                Process process;
                try {
                    process = Runtime.getRuntime().exec("cmd /c  D:\\programming\\Nosql\\1NoSql\\NoSQl-main\\help.bat");// 执行py文件
                    //用输入输出流来截取结果
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line = null;
                    String prediction = null;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("[")) {
                            prediction = line.substring(1,line.length()-1);
                        }
                    }
                    List<String> movieId=new ArrayList<String>();
                    List<String> possibility=new ArrayList<String>();

                    StringTokenizer st=new StringTokenizer(prediction,", ");
                    int switchNum=1;
                    while(st.hasMoreTokens()){
                        String temp = st.nextToken();
                        if(switchNum==1){
                            movieId.add(temp.substring(1));
                            switchNum = 2;
                        }else{
                            possibility.add(temp.substring(1,temp.length()-2));
                            switchNum = 1;
                        }
                    }

                    response = RecommendResponse.newBuilder().setUserId(request.getUserId())
                            .addAllMovieId(movieId).addAllPossibility(possibility).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                    logger.info("Server has sent a response");

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }
}