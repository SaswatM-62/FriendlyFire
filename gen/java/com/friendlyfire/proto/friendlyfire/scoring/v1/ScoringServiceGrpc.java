package com.friendlyfire.proto.friendlyfire.scoring.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ScoringServiceGrpc {

  private ScoringServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.scoring.v1.ScoringService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> getStartQuizMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartQuiz",
      requestType = com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> getStartQuizMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> getStartQuizMethod;
    if ((getStartQuizMethod = ScoringServiceGrpc.getStartQuizMethod) == null) {
      synchronized (ScoringServiceGrpc.class) {
        if ((getStartQuizMethod = ScoringServiceGrpc.getStartQuizMethod) == null) {
          ScoringServiceGrpc.getStartQuizMethod = getStartQuizMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartQuiz"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScoringServiceMethodDescriptorSupplier("StartQuiz"))
              .build();
        }
      }
    }
    return getStartQuizMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> getRecordRoundMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RecordRound",
      requestType = com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> getRecordRoundMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> getRecordRoundMethod;
    if ((getRecordRoundMethod = ScoringServiceGrpc.getRecordRoundMethod) == null) {
      synchronized (ScoringServiceGrpc.class) {
        if ((getRecordRoundMethod = ScoringServiceGrpc.getRecordRoundMethod) == null) {
          ScoringServiceGrpc.getRecordRoundMethod = getRecordRoundMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RecordRound"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScoringServiceMethodDescriptorSupplier("RecordRound"))
              .build();
        }
      }
    }
    return getRecordRoundMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> getFinalizeQuizMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FinalizeQuiz",
      requestType = com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> getFinalizeQuizMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> getFinalizeQuizMethod;
    if ((getFinalizeQuizMethod = ScoringServiceGrpc.getFinalizeQuizMethod) == null) {
      synchronized (ScoringServiceGrpc.class) {
        if ((getFinalizeQuizMethod = ScoringServiceGrpc.getFinalizeQuizMethod) == null) {
          ScoringServiceGrpc.getFinalizeQuizMethod = getFinalizeQuizMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FinalizeQuiz"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScoringServiceMethodDescriptorSupplier("FinalizeQuiz"))
              .build();
        }
      }
    }
    return getFinalizeQuizMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> getGetQuizResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetQuizResult",
      requestType = com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> getGetQuizResultMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> getGetQuizResultMethod;
    if ((getGetQuizResultMethod = ScoringServiceGrpc.getGetQuizResultMethod) == null) {
      synchronized (ScoringServiceGrpc.class) {
        if ((getGetQuizResultMethod = ScoringServiceGrpc.getGetQuizResultMethod) == null) {
          ScoringServiceGrpc.getGetQuizResultMethod = getGetQuizResultMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetQuizResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScoringServiceMethodDescriptorSupplier("GetQuizResult"))
              .build();
        }
      }
    }
    return getGetQuizResultMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> getGetPlayerHistoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetPlayerHistory",
      requestType = com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest,
      com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> getGetPlayerHistoryMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> getGetPlayerHistoryMethod;
    if ((getGetPlayerHistoryMethod = ScoringServiceGrpc.getGetPlayerHistoryMethod) == null) {
      synchronized (ScoringServiceGrpc.class) {
        if ((getGetPlayerHistoryMethod = ScoringServiceGrpc.getGetPlayerHistoryMethod) == null) {
          ScoringServiceGrpc.getGetPlayerHistoryMethod = getGetPlayerHistoryMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest, com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetPlayerHistory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScoringServiceMethodDescriptorSupplier("GetPlayerHistory"))
              .build();
        }
      }
    }
    return getGetPlayerHistoryMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ScoringServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScoringServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScoringServiceStub>() {
        @java.lang.Override
        public ScoringServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScoringServiceStub(channel, callOptions);
        }
      };
    return ScoringServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ScoringServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScoringServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScoringServiceBlockingV2Stub>() {
        @java.lang.Override
        public ScoringServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScoringServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return ScoringServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ScoringServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScoringServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScoringServiceBlockingStub>() {
        @java.lang.Override
        public ScoringServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScoringServiceBlockingStub(channel, callOptions);
        }
      };
    return ScoringServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ScoringServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScoringServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScoringServiceFutureStub>() {
        @java.lang.Override
        public ScoringServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScoringServiceFutureStub(channel, callOptions);
        }
      };
    return ScoringServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Registers a quiz before its first question. ALREADY_EXISTS if the
     * quiz_id was already registered.
     * </pre>
     */
    default void startQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStartQuizMethod(), responseObserver);
    }

    /**
     * <pre>
     * Scores one finished round and returns the updated scoreboard.
     * Called once per round, after the timer ends, with every player's answer.
     * Safe to retry: sending the same quiz_id + round_number again replaces
     * the earlier result instead of counting it twice.
     * Suggested points: correct = 500 + 500 * (time left / time limit),
     * rounded; wrong or no answer = 0.
     * </pre>
     */
    default void recordRound(com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRecordRoundMethod(), responseObserver);
    }

    /**
     * <pre>
     * Marks a quiz as over and returns the final standings.
     * </pre>
     */
    default void finalizeQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFinalizeQuizMethod(), responseObserver);
    }

    /**
     * <pre>
     * One finished quiz. NOT_FOUND if it doesn't exist.
     * </pre>
     */
    default void getQuizResult(com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetQuizResultMethod(), responseObserver);
    }

    /**
     * <pre>
     * A player's past quizzes, newest first.
     * </pre>
     */
    default void getPlayerHistory(com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetPlayerHistoryMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ScoringService.
   */
  public static abstract class ScoringServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ScoringServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ScoringService.
   */
  public static final class ScoringServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ScoringServiceStub> {
    private ScoringServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScoringServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScoringServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Registers a quiz before its first question. ALREADY_EXISTS if the
     * quiz_id was already registered.
     * </pre>
     */
    public void startQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStartQuizMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Scores one finished round and returns the updated scoreboard.
     * Called once per round, after the timer ends, with every player's answer.
     * Safe to retry: sending the same quiz_id + round_number again replaces
     * the earlier result instead of counting it twice.
     * Suggested points: correct = 500 + 500 * (time left / time limit),
     * rounded; wrong or no answer = 0.
     * </pre>
     */
    public void recordRound(com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRecordRoundMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Marks a quiz as over and returns the final standings.
     * </pre>
     */
    public void finalizeQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFinalizeQuizMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * One finished quiz. NOT_FOUND if it doesn't exist.
     * </pre>
     */
    public void getQuizResult(com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetQuizResultMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * A player's past quizzes, newest first.
     * </pre>
     */
    public void getPlayerHistory(com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetPlayerHistoryMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ScoringService.
   */
  public static final class ScoringServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ScoringServiceBlockingV2Stub> {
    private ScoringServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScoringServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScoringServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Registers a quiz before its first question. ALREADY_EXISTS if the
     * quiz_id was already registered.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse startQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getStartQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Scores one finished round and returns the updated scoreboard.
     * Called once per round, after the timer ends, with every player's answer.
     * Safe to retry: sending the same quiz_id + round_number again replaces
     * the earlier result instead of counting it twice.
     * Suggested points: correct = 500 + 500 * (time left / time limit),
     * rounded; wrong or no answer = 0.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse recordRound(com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRecordRoundMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Marks a quiz as over and returns the final standings.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse finalizeQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getFinalizeQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * One finished quiz. NOT_FOUND if it doesn't exist.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse getQuizResult(com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetQuizResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A player's past quizzes, newest first.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse getPlayerHistory(com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetPlayerHistoryMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ScoringService.
   */
  public static final class ScoringServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ScoringServiceBlockingStub> {
    private ScoringServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScoringServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScoringServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Registers a quiz before its first question. ALREADY_EXISTS if the
     * quiz_id was already registered.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse startQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStartQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Scores one finished round and returns the updated scoreboard.
     * Called once per round, after the timer ends, with every player's answer.
     * Safe to retry: sending the same quiz_id + round_number again replaces
     * the earlier result instead of counting it twice.
     * Suggested points: correct = 500 + 500 * (time left / time limit),
     * rounded; wrong or no answer = 0.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse recordRound(com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRecordRoundMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Marks a quiz as over and returns the final standings.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse finalizeQuiz(com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFinalizeQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * One finished quiz. NOT_FOUND if it doesn't exist.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse getQuizResult(com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetQuizResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A player's past quizzes, newest first.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse getPlayerHistory(com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetPlayerHistoryMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ScoringService.
   */
  public static final class ScoringServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ScoringServiceFutureStub> {
    private ScoringServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScoringServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScoringServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Registers a quiz before its first question. ALREADY_EXISTS if the
     * quiz_id was already registered.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse> startQuiz(
        com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStartQuizMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Scores one finished round and returns the updated scoreboard.
     * Called once per round, after the timer ends, with every player's answer.
     * Safe to retry: sending the same quiz_id + round_number again replaces
     * the earlier result instead of counting it twice.
     * Suggested points: correct = 500 + 500 * (time left / time limit),
     * rounded; wrong or no answer = 0.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse> recordRound(
        com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRecordRoundMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Marks a quiz as over and returns the final standings.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse> finalizeQuiz(
        com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFinalizeQuizMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * One finished quiz. NOT_FOUND if it doesn't exist.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse> getQuizResult(
        com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetQuizResultMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * A player's past quizzes, newest first.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse> getPlayerHistory(
        com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetPlayerHistoryMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_START_QUIZ = 0;
  private static final int METHODID_RECORD_ROUND = 1;
  private static final int METHODID_FINALIZE_QUIZ = 2;
  private static final int METHODID_GET_QUIZ_RESULT = 3;
  private static final int METHODID_GET_PLAYER_HISTORY = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_START_QUIZ:
          serviceImpl.startQuiz((com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse>) responseObserver);
          break;
        case METHODID_RECORD_ROUND:
          serviceImpl.recordRound((com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse>) responseObserver);
          break;
        case METHODID_FINALIZE_QUIZ:
          serviceImpl.finalizeQuiz((com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse>) responseObserver);
          break;
        case METHODID_GET_QUIZ_RESULT:
          serviceImpl.getQuizResult((com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse>) responseObserver);
          break;
        case METHODID_GET_PLAYER_HISTORY:
          serviceImpl.getPlayerHistory((com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getStartQuizMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest,
              com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizResponse>(
                service, METHODID_START_QUIZ)))
        .addMethod(
          getRecordRoundMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest,
              com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse>(
                service, METHODID_RECORD_ROUND)))
        .addMethod(
          getFinalizeQuizMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest,
              com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse>(
                service, METHODID_FINALIZE_QUIZ)))
        .addMethod(
          getGetQuizResultMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest,
              com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse>(
                service, METHODID_GET_QUIZ_RESULT)))
        .addMethod(
          getGetPlayerHistoryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest,
              com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse>(
                service, METHODID_GET_PLAYER_HISTORY)))
        .build();
  }

  private static abstract class ScoringServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ScoringServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ScoringService");
    }
  }

  private static final class ScoringServiceFileDescriptorSupplier
      extends ScoringServiceBaseDescriptorSupplier {
    ScoringServiceFileDescriptorSupplier() {}
  }

  private static final class ScoringServiceMethodDescriptorSupplier
      extends ScoringServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ScoringServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ScoringServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ScoringServiceFileDescriptorSupplier())
              .addMethod(getStartQuizMethod())
              .addMethod(getRecordRoundMethod())
              .addMethod(getFinalizeQuizMethod())
              .addMethod(getGetQuizResultMethod())
              .addMethod(getGetPlayerHistoryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
