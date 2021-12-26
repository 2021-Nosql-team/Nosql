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

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /**
     * Construct client for accessing HelloWorld server using the existing channel.
     */
    public HelloWorldClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void getMovieProfile(String name, long movieId) {
        logger.info("Will try to get MovieProfile with id = " + movieId + "...");
        MovieProfileRequest request = MovieProfileRequest.newBuilder().setSessionId(name).setMovieId(movieId).build();
        MovieProfileResponse response;

        try {
            response = blockingStub.getMovieProfile(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        logger.info("--- Movie Profile ---");
        logger.info("Movie id: " + movieId);
        logger.info("Movie title: " + response.getTitle());
        logger.info("Movie genres: " + response.getGenres());
        logger.info("Movie's average rating: " + response.getRating());
        logger.info("Movie's tags' size: " + response.getTagsList().size());
        logger.info("----------------------");
    }

    public void getRecommendMovies(String userId) {
        logger.info("Will try to get the Recommend movies for user with id = " + userId + "...");
        RecommendRequest request = RecommendRequest.newBuilder().setUserId(userId).build();
        RecommendResponse response;

        try {
            response = blockingStub.getRecommendMovies(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }

        List<String> movies = response.getMovieIdList();
        List<String> possibility = response.getPossibilityList();
        System.out.println("--- The recommend movies for user " + userId + " ---");
        for (int i = 1; i <= 5; i++) {
            System.out.println("Movie_" + i + " : " + movies.get(i - 1) + "   Possibility : " + possibility.get(i - 1));
        }
        System.out.println("---------------------------------------");
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting. The second argument is the target server.
     */
    public static void main(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        String target = "localhost:50051";
        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [name [target]]");
                System.err.println("");
                System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
                System.err.println("  target  The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 1) {
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
//            client.getMovieProfile("1", 131070);
            Scanner scan = new Scanner(System.in);
            logger.info("请输入userId：");
            String userId = scan.nextLine();
            client.getRecommendMovies(userId);

        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
