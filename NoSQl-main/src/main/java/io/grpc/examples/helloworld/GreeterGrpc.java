package io.grpc.examples.helloworld;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 * <pre>
 * The greeting service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.2.0)",
    comments = "Source: helloworld.proto")
public final class GreeterGrpc {

  private GreeterGrpc() {}

  public static final String SERVICE_NAME = "helloworld.Greeter";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<MovieProfileRequest,
      MovieProfileResponse> METHOD_GET_MOVIE_PROFILE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "helloworld.Greeter", "GetMovieProfile"),
          io.grpc.protobuf.ProtoUtils.marshaller(MovieProfileRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(MovieProfileResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<UserProfileRequest,
      UserProfileResponse> METHOD_GET_USER_PROFILE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "helloworld.Greeter", "GetUserProfile"),
          io.grpc.protobuf.ProtoUtils.marshaller(UserProfileRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(UserProfileResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<RecommendRequest,
      RecommendResponse> METHOD_GET_RECOMMEND_MOVIES =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "helloworld.Greeter", "GetRecommendMovies"),
          io.grpc.protobuf.ProtoUtils.marshaller(RecommendRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(RecommendResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GreeterStub newStub(io.grpc.Channel channel) {
    return new GreeterStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GreeterBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new GreeterBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static GreeterFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new GreeterFutureStub(channel);
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static abstract class GreeterImplBase implements io.grpc.BindableService {

    /**
     */
    public void getMovieProfile(MovieProfileRequest request,
                                io.grpc.stub.StreamObserver<MovieProfileResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_MOVIE_PROFILE, responseObserver);
    }

    /**
     */
    public void getUserProfile(UserProfileRequest request,
                               io.grpc.stub.StreamObserver<UserProfileResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_USER_PROFILE, responseObserver);
    }

    /**
     */
    public void getRecommendMovies(RecommendRequest request,
                                   io.grpc.stub.StreamObserver<RecommendResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_RECOMMEND_MOVIES, responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_GET_MOVIE_PROFILE,
            asyncUnaryCall(
              new MethodHandlers<
                MovieProfileRequest,
                MovieProfileResponse>(
                  this, METHODID_GET_MOVIE_PROFILE)))
          .addMethod(
            METHOD_GET_USER_PROFILE,
            asyncUnaryCall(
              new MethodHandlers<
                UserProfileRequest,
                UserProfileResponse>(
                  this, METHODID_GET_USER_PROFILE)))
          .addMethod(
            METHOD_GET_RECOMMEND_MOVIES,
            asyncUnaryCall(
              new MethodHandlers<
                RecommendRequest,
                RecommendResponse>(
                  this, METHODID_GET_RECOMMEND_MOVIES)))
          .build();
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class GreeterStub extends io.grpc.stub.AbstractStub<GreeterStub> {
    private GreeterStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterStub(channel, callOptions);
    }

    /**
     */
    public void getMovieProfile(MovieProfileRequest request,
                                io.grpc.stub.StreamObserver<MovieProfileResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_MOVIE_PROFILE, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getUserProfile(UserProfileRequest request,
                               io.grpc.stub.StreamObserver<UserProfileResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_USER_PROFILE, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getRecommendMovies(RecommendRequest request,
                                   io.grpc.stub.StreamObserver<RecommendResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_RECOMMEND_MOVIES, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class GreeterBlockingStub extends io.grpc.stub.AbstractStub<GreeterBlockingStub> {
    private GreeterBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterBlockingStub(channel, callOptions);
    }

    /**
     */
    public MovieProfileResponse getMovieProfile(MovieProfileRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_MOVIE_PROFILE, getCallOptions(), request);
    }

    /**
     */
    public UserProfileResponse getUserProfile(UserProfileRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_USER_PROFILE, getCallOptions(), request);
    }

    /**
     */
    public RecommendResponse getRecommendMovies(RecommendRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_RECOMMEND_MOVIES, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The greeting service definition.
   * </pre>
   */
  public static final class GreeterFutureStub extends io.grpc.stub.AbstractStub<GreeterFutureStub> {
    private GreeterFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GreeterFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected GreeterFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GreeterFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<MovieProfileResponse> getMovieProfile(
        MovieProfileRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_MOVIE_PROFILE, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<UserProfileResponse> getUserProfile(
        UserProfileRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_USER_PROFILE, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<RecommendResponse> getRecommendMovies(
        RecommendRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_RECOMMEND_MOVIES, getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_MOVIE_PROFILE = 0;
  private static final int METHODID_GET_USER_PROFILE = 1;
  private static final int METHODID_GET_RECOMMEND_MOVIES = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GreeterImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GreeterImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_MOVIE_PROFILE:
          serviceImpl.getMovieProfile((MovieProfileRequest) request,
              (io.grpc.stub.StreamObserver<MovieProfileResponse>) responseObserver);
          break;
        case METHODID_GET_USER_PROFILE:
          serviceImpl.getUserProfile((UserProfileRequest) request,
              (io.grpc.stub.StreamObserver<UserProfileResponse>) responseObserver);
          break;
        case METHODID_GET_RECOMMEND_MOVIES:
          serviceImpl.getRecommendMovies((RecommendRequest) request,
              (io.grpc.stub.StreamObserver<RecommendResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class GreeterDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return HelloWorldProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GreeterGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GreeterDescriptorSupplier())
              .addMethod(METHOD_GET_MOVIE_PROFILE)
              .addMethod(METHOD_GET_USER_PROFILE)
              .addMethod(METHOD_GET_RECOMMEND_MOVIES)
              .build();
        }
      }
    }
    return result;
  }
}
