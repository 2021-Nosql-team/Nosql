// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: helloworld.proto

package io.grpc.examples.helloworld;

public interface RecallResultOrBuilder extends
    // @@protoc_insertion_point(interface_extends:helloworld.RecallResult)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional string userId = 1;</code>
   */
  String getUserId();
  /**
   * <code>optional string userId = 1;</code>
   */
  com.google.protobuf.ByteString
      getUserIdBytes();

  /**
   * <code>repeated string movieId = 2;</code>
   */
  java.util.List<String>
      getMovieIdList();
  /**
   * <code>repeated string movieId = 2;</code>
   */
  int getMovieIdCount();
  /**
   * <code>repeated string movieId = 2;</code>
   */
  String getMovieId(int index);
  /**
   * <code>repeated string movieId = 2;</code>
   */
  com.google.protobuf.ByteString
      getMovieIdBytes(int index);
}