// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: helloworld.proto

package io.grpc.examples.helloworld;

public interface MovieProfile2SimOrBuilder extends
    // @@protoc_insertion_point(interface_extends:helloworld.MovieProfile2Sim)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional .helloworld.MovieProfileResponse movie_profile = 1;</code>
   */
  boolean hasMovieProfile();
  /**
   * <code>optional .helloworld.MovieProfileResponse movie_profile = 1;</code>
   */
  MovieProfileResponse getMovieProfile();
  /**
   * <code>optional .helloworld.MovieProfileResponse movie_profile = 1;</code>
   */
  MovieProfileResponseOrBuilder getMovieProfileOrBuilder();

  /**
   * <code>optional double relevance = 2;</code>
   */
  double getRelevance();
}