// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: helloworld.proto

package io.grpc.examples.helloworld;

public interface UserProfileRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:helloworld.UserProfileRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional string session_id = 1;</code>
   */
  String getSessionId();
  /**
   * <code>optional string session_id = 1;</code>
   */
  com.google.protobuf.ByteString
      getSessionIdBytes();

  /**
   * <code>optional uint64 user_id = 2;</code>
   */
  long getUserId();
}
