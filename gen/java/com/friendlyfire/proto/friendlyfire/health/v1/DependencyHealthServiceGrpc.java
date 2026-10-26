package com.friendlyfire.proto.friendlyfire.health.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class DependencyHealthServiceGrpc {

  private DependencyHealthServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.health.v1.DependencyHealthService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest,
      com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> getCheckDependenciesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckDependencies",
      requestType = com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest,
      com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> getCheckDependenciesMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest, com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> getCheckDependenciesMethod;
    if ((getCheckDependenciesMethod = DependencyHealthServiceGrpc.getCheckDependenciesMethod) == null) {
      synchronized (DependencyHealthServiceGrpc.class) {
        if ((getCheckDependenciesMethod = DependencyHealthServiceGrpc.getCheckDependenciesMethod) == null) {
          DependencyHealthServiceGrpc.getCheckDependenciesMethod = getCheckDependenciesMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest, com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckDependencies"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DependencyHealthServiceMethodDescriptorSupplier("CheckDependencies"))
              .build();
        }
      }
    }
    return getCheckDependenciesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DependencyHealthServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceStub>() {
        @java.lang.Override
        public DependencyHealthServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DependencyHealthServiceStub(channel, callOptions);
        }
      };
    return DependencyHealthServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static DependencyHealthServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceBlockingV2Stub>() {
        @java.lang.Override
        public DependencyHealthServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DependencyHealthServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return DependencyHealthServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DependencyHealthServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceBlockingStub>() {
        @java.lang.Override
        public DependencyHealthServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DependencyHealthServiceBlockingStub(channel, callOptions);
        }
      };
    return DependencyHealthServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DependencyHealthServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DependencyHealthServiceFutureStub>() {
        @java.lang.Override
        public DependencyHealthServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DependencyHealthServiceFutureStub(channel, callOptions);
        }
      };
    return DependencyHealthServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Checks this service and everything below it. Never returns an error
     * for an unhealthy dependency; it reports it in the tree instead.
     * Each dependency check gets a 1-second deadline.
     * </pre>
     */
    default void checkDependencies(com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckDependenciesMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service DependencyHealthService.
   */
  public static abstract class DependencyHealthServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return DependencyHealthServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service DependencyHealthService.
   */
  public static final class DependencyHealthServiceStub
      extends io.grpc.stub.AbstractAsyncStub<DependencyHealthServiceStub> {
    private DependencyHealthServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DependencyHealthServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DependencyHealthServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Checks this service and everything below it. Never returns an error
     * for an unhealthy dependency; it reports it in the tree instead.
     * Each dependency check gets a 1-second deadline.
     * </pre>
     */
    public void checkDependencies(com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckDependenciesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service DependencyHealthService.
   */
  public static final class DependencyHealthServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<DependencyHealthServiceBlockingV2Stub> {
    private DependencyHealthServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DependencyHealthServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DependencyHealthServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Checks this service and everything below it. Never returns an error
     * for an unhealthy dependency; it reports it in the tree instead.
     * Each dependency check gets a 1-second deadline.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse checkDependencies(com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCheckDependenciesMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service DependencyHealthService.
   */
  public static final class DependencyHealthServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<DependencyHealthServiceBlockingStub> {
    private DependencyHealthServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DependencyHealthServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DependencyHealthServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Checks this service and everything below it. Never returns an error
     * for an unhealthy dependency; it reports it in the tree instead.
     * Each dependency check gets a 1-second deadline.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse checkDependencies(com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckDependenciesMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service DependencyHealthService.
   */
  public static final class DependencyHealthServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<DependencyHealthServiceFutureStub> {
    private DependencyHealthServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DependencyHealthServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DependencyHealthServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Checks this service and everything below it. Never returns an error
     * for an unhealthy dependency; it reports it in the tree instead.
     * Each dependency check gets a 1-second deadline.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse> checkDependencies(
        com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckDependenciesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CHECK_DEPENDENCIES = 0;

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
        case METHODID_CHECK_DEPENDENCIES:
          serviceImpl.checkDependencies((com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse>) responseObserver);
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
          getCheckDependenciesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest,
              com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse>(
                service, METHODID_CHECK_DEPENDENCIES)))
        .build();
  }

  private static abstract class DependencyHealthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DependencyHealthServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DependencyHealthService");
    }
  }

  private static final class DependencyHealthServiceFileDescriptorSupplier
      extends DependencyHealthServiceBaseDescriptorSupplier {
    DependencyHealthServiceFileDescriptorSupplier() {}
  }

  private static final class DependencyHealthServiceMethodDescriptorSupplier
      extends DependencyHealthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    DependencyHealthServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (DependencyHealthServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DependencyHealthServiceFileDescriptorSupplier())
              .addMethod(getCheckDependenciesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
