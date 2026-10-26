package com.friendlyfire.proto.friendlyfire.quiz.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class QuizServiceGrpc {

  private QuizServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.quiz.v1.QuizService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest,
      com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> getGenerateQuizMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateQuiz",
      requestType = com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest,
      com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> getGenerateQuizMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest, com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> getGenerateQuizMethod;
    if ((getGenerateQuizMethod = QuizServiceGrpc.getGenerateQuizMethod) == null) {
      synchronized (QuizServiceGrpc.class) {
        if ((getGenerateQuizMethod = QuizServiceGrpc.getGenerateQuizMethod) == null) {
          QuizServiceGrpc.getGenerateQuizMethod = getGenerateQuizMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest, com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateQuiz"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QuizServiceMethodDescriptorSupplier("GenerateQuiz"))
              .build();
        }
      }
    }
    return getGenerateQuizMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QuizServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QuizServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QuizServiceStub>() {
        @java.lang.Override
        public QuizServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QuizServiceStub(channel, callOptions);
        }
      };
    return QuizServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static QuizServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QuizServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QuizServiceBlockingV2Stub>() {
        @java.lang.Override
        public QuizServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QuizServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return QuizServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QuizServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QuizServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QuizServiceBlockingStub>() {
        @java.lang.Override
        public QuizServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QuizServiceBlockingStub(channel, callOptions);
        }
      };
    return QuizServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QuizServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QuizServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QuizServiceFutureStub>() {
        @java.lang.Override
        public QuizServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QuizServiceFutureStub(channel, callOptions);
        }
      };
    return QuizServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Builds every question for a quiz. Uses both matches the players played
     * together and each player's solo matches.
     * Errors:
     *   INVALID_ARGUMENT     fewer than 2 players, or question_count outside 5–20
     *   FAILED_PRECONDITION  not enough match data to make question_count
     *                        different questions (the message says how many
     *                        could be made)
     * </pre>
     */
    default void generateQuiz(com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateQuizMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service QuizService.
   */
  public static abstract class QuizServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QuizServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service QuizService.
   */
  public static final class QuizServiceStub
      extends io.grpc.stub.AbstractAsyncStub<QuizServiceStub> {
    private QuizServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QuizServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QuizServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Builds every question for a quiz. Uses both matches the players played
     * together and each player's solo matches.
     * Errors:
     *   INVALID_ARGUMENT     fewer than 2 players, or question_count outside 5–20
     *   FAILED_PRECONDITION  not enough match data to make question_count
     *                        different questions (the message says how many
     *                        could be made)
     * </pre>
     */
    public void generateQuiz(com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateQuizMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service QuizService.
   */
  public static final class QuizServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<QuizServiceBlockingV2Stub> {
    private QuizServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QuizServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QuizServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Builds every question for a quiz. Uses both matches the players played
     * together and each player's solo matches.
     * Errors:
     *   INVALID_ARGUMENT     fewer than 2 players, or question_count outside 5–20
     *   FAILED_PRECONDITION  not enough match data to make question_count
     *                        different questions (the message says how many
     *                        could be made)
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse generateQuiz(com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGenerateQuizMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service QuizService.
   */
  public static final class QuizServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QuizServiceBlockingStub> {
    private QuizServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QuizServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QuizServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Builds every question for a quiz. Uses both matches the players played
     * together and each player's solo matches.
     * Errors:
     *   INVALID_ARGUMENT     fewer than 2 players, or question_count outside 5–20
     *   FAILED_PRECONDITION  not enough match data to make question_count
     *                        different questions (the message says how many
     *                        could be made)
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse generateQuiz(com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateQuizMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service QuizService.
   */
  public static final class QuizServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<QuizServiceFutureStub> {
    private QuizServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QuizServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QuizServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Builds every question for a quiz. Uses both matches the players played
     * together and each player's solo matches.
     * Errors:
     *   INVALID_ARGUMENT     fewer than 2 players, or question_count outside 5–20
     *   FAILED_PRECONDITION  not enough match data to make question_count
     *                        different questions (the message says how many
     *                        could be made)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse> generateQuiz(
        com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateQuizMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GENERATE_QUIZ = 0;

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
        case METHODID_GENERATE_QUIZ:
          serviceImpl.generateQuiz((com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse>) responseObserver);
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
          getGenerateQuizMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest,
              com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizResponse>(
                service, METHODID_GENERATE_QUIZ)))
        .build();
  }

  private static abstract class QuizServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QuizServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.quiz.v1.QuizServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QuizService");
    }
  }

  private static final class QuizServiceFileDescriptorSupplier
      extends QuizServiceBaseDescriptorSupplier {
    QuizServiceFileDescriptorSupplier() {}
  }

  private static final class QuizServiceMethodDescriptorSupplier
      extends QuizServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QuizServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (QuizServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QuizServiceFileDescriptorSupplier())
              .addMethod(getGenerateQuizMethod())
              .build();
        }
      }
    }
    return result;
  }
}
