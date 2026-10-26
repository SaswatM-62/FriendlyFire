package com.friendlyfire.proto.friendlyfire.matchdata.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class MatchDataServiceGrpc {

  private MatchDataServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.matchdata.v1.MatchDataService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> getSyncPlayerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncPlayer",
      requestType = com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> getSyncPlayerMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> getSyncPlayerMethod;
    if ((getSyncPlayerMethod = MatchDataServiceGrpc.getSyncPlayerMethod) == null) {
      synchronized (MatchDataServiceGrpc.class) {
        if ((getSyncPlayerMethod = MatchDataServiceGrpc.getSyncPlayerMethod) == null) {
          MatchDataServiceGrpc.getSyncPlayerMethod = getSyncPlayerMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncPlayer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MatchDataServiceMethodDescriptorSupplier("SyncPlayer"))
              .build();
        }
      }
    }
    return getSyncPlayerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> getGetSyncStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSyncStatus",
      requestType = com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> getGetSyncStatusMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> getGetSyncStatusMethod;
    if ((getGetSyncStatusMethod = MatchDataServiceGrpc.getGetSyncStatusMethod) == null) {
      synchronized (MatchDataServiceGrpc.class) {
        if ((getGetSyncStatusMethod = MatchDataServiceGrpc.getGetSyncStatusMethod) == null) {
          MatchDataServiceGrpc.getGetSyncStatusMethod = getGetSyncStatusMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSyncStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MatchDataServiceMethodDescriptorSupplier("GetSyncStatus"))
              .build();
        }
      }
    }
    return getGetSyncStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> getGetPlayerMatchStatsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetPlayerMatchStats",
      requestType = com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> getGetPlayerMatchStatsMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> getGetPlayerMatchStatsMethod;
    if ((getGetPlayerMatchStatsMethod = MatchDataServiceGrpc.getGetPlayerMatchStatsMethod) == null) {
      synchronized (MatchDataServiceGrpc.class) {
        if ((getGetPlayerMatchStatsMethod = MatchDataServiceGrpc.getGetPlayerMatchStatsMethod) == null) {
          MatchDataServiceGrpc.getGetPlayerMatchStatsMethod = getGetPlayerMatchStatsMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetPlayerMatchStats"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MatchDataServiceMethodDescriptorSupplier("GetPlayerMatchStats"))
              .build();
        }
      }
    }
    return getGetPlayerMatchStatsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> getDeletePlayerDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePlayerData",
      requestType = com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest,
      com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> getDeletePlayerDataMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> getDeletePlayerDataMethod;
    if ((getDeletePlayerDataMethod = MatchDataServiceGrpc.getDeletePlayerDataMethod) == null) {
      synchronized (MatchDataServiceGrpc.class) {
        if ((getDeletePlayerDataMethod = MatchDataServiceGrpc.getDeletePlayerDataMethod) == null) {
          MatchDataServiceGrpc.getDeletePlayerDataMethod = getDeletePlayerDataMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest, com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePlayerData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MatchDataServiceMethodDescriptorSupplier("DeletePlayerData"))
              .build();
        }
      }
    }
    return getDeletePlayerDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MatchDataServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceStub>() {
        @java.lang.Override
        public MatchDataServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MatchDataServiceStub(channel, callOptions);
        }
      };
    return MatchDataServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static MatchDataServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceBlockingV2Stub>() {
        @java.lang.Override
        public MatchDataServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MatchDataServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return MatchDataServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MatchDataServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceBlockingStub>() {
        @java.lang.Override
        public MatchDataServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MatchDataServiceBlockingStub(channel, callOptions);
        }
      };
    return MatchDataServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MatchDataServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MatchDataServiceFutureStub>() {
        @java.lang.Override
        public MatchDataServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MatchDataServiceFutureStub(channel, callOptions);
        }
      };
    return MatchDataServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Starts fetching a player's matches in the background and returns right
     * away. The first sync fetches the last 30 days; later syncs only fetch
     * matches newer than the last one stored. Calling it while a sync is
     * already running does nothing.
     * </pre>
     */
    default void syncPlayer(com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSyncPlayerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Sync status for one or more players. Players we know nothing about come
     * back as SYNC_STATUS_NOT_LINKED.
     * </pre>
     */
    default void getSyncStatus(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSyncStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Stored match stats for the given players, newest first. A match that
     * several of the players played together appears once per player, with
     * the same match_id.
     * </pre>
     */
    default void getPlayerMatchStats(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetPlayerMatchStatsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes everything stored for a player. Used when they unlink or
     * delete their account.
     * </pre>
     */
    default void deletePlayerData(com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeletePlayerDataMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service MatchDataService.
   */
  public static abstract class MatchDataServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return MatchDataServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service MatchDataService.
   */
  public static final class MatchDataServiceStub
      extends io.grpc.stub.AbstractAsyncStub<MatchDataServiceStub> {
    private MatchDataServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MatchDataServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MatchDataServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Starts fetching a player's matches in the background and returns right
     * away. The first sync fetches the last 30 days; later syncs only fetch
     * matches newer than the last one stored. Calling it while a sync is
     * already running does nothing.
     * </pre>
     */
    public void syncPlayer(com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSyncPlayerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sync status for one or more players. Players we know nothing about come
     * back as SYNC_STATUS_NOT_LINKED.
     * </pre>
     */
    public void getSyncStatus(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSyncStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Stored match stats for the given players, newest first. A match that
     * several of the players played together appears once per player, with
     * the same match_id.
     * </pre>
     */
    public void getPlayerMatchStats(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetPlayerMatchStatsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes everything stored for a player. Used when they unlink or
     * delete their account.
     * </pre>
     */
    public void deletePlayerData(com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeletePlayerDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service MatchDataService.
   */
  public static final class MatchDataServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<MatchDataServiceBlockingV2Stub> {
    private MatchDataServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MatchDataServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MatchDataServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Starts fetching a player's matches in the background and returns right
     * away. The first sync fetches the last 30 days; later syncs only fetch
     * matches newer than the last one stored. Calling it while a sync is
     * already running does nothing.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse syncPlayer(com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSyncPlayerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sync status for one or more players. Players we know nothing about come
     * back as SYNC_STATUS_NOT_LINKED.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse getSyncStatus(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetSyncStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Stored match stats for the given players, newest first. A match that
     * several of the players played together appears once per player, with
     * the same match_id.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse getPlayerMatchStats(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetPlayerMatchStatsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes everything stored for a player. Used when they unlink or
     * delete their account.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse deletePlayerData(com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeletePlayerDataMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service MatchDataService.
   */
  public static final class MatchDataServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<MatchDataServiceBlockingStub> {
    private MatchDataServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MatchDataServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MatchDataServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Starts fetching a player's matches in the background and returns right
     * away. The first sync fetches the last 30 days; later syncs only fetch
     * matches newer than the last one stored. Calling it while a sync is
     * already running does nothing.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse syncPlayer(com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSyncPlayerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Sync status for one or more players. Players we know nothing about come
     * back as SYNC_STATUS_NOT_LINKED.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse getSyncStatus(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSyncStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Stored match stats for the given players, newest first. A match that
     * several of the players played together appears once per player, with
     * the same match_id.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse getPlayerMatchStats(com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetPlayerMatchStatsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes everything stored for a player. Used when they unlink or
     * delete their account.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse deletePlayerData(com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeletePlayerDataMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service MatchDataService.
   */
  public static final class MatchDataServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<MatchDataServiceFutureStub> {
    private MatchDataServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MatchDataServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MatchDataServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Starts fetching a player's matches in the background and returns right
     * away. The first sync fetches the last 30 days; later syncs only fetch
     * matches newer than the last one stored. Calling it while a sync is
     * already running does nothing.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse> syncPlayer(
        com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSyncPlayerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Sync status for one or more players. Players we know nothing about come
     * back as SYNC_STATUS_NOT_LINKED.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse> getSyncStatus(
        com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSyncStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Stored match stats for the given players, newest first. A match that
     * several of the players played together appears once per player, with
     * the same match_id.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse> getPlayerMatchStats(
        com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetPlayerMatchStatsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes everything stored for a player. Used when they unlink or
     * delete their account.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse> deletePlayerData(
        com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeletePlayerDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SYNC_PLAYER = 0;
  private static final int METHODID_GET_SYNC_STATUS = 1;
  private static final int METHODID_GET_PLAYER_MATCH_STATS = 2;
  private static final int METHODID_DELETE_PLAYER_DATA = 3;

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
        case METHODID_SYNC_PLAYER:
          serviceImpl.syncPlayer((com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse>) responseObserver);
          break;
        case METHODID_GET_SYNC_STATUS:
          serviceImpl.getSyncStatus((com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse>) responseObserver);
          break;
        case METHODID_GET_PLAYER_MATCH_STATS:
          serviceImpl.getPlayerMatchStats((com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse>) responseObserver);
          break;
        case METHODID_DELETE_PLAYER_DATA:
          serviceImpl.deletePlayerData((com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse>) responseObserver);
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
          getSyncPlayerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest,
              com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerResponse>(
                service, METHODID_SYNC_PLAYER)))
        .addMethod(
          getGetSyncStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest,
              com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusResponse>(
                service, METHODID_GET_SYNC_STATUS)))
        .addMethod(
          getGetPlayerMatchStatsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsRequest,
              com.friendlyfire.proto.friendlyfire.matchdata.v1.GetPlayerMatchStatsResponse>(
                service, METHODID_GET_PLAYER_MATCH_STATS)))
        .addMethod(
          getDeletePlayerDataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest,
              com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataResponse>(
                service, METHODID_DELETE_PLAYER_DATA)))
        .build();
  }

  private static abstract class MatchDataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MatchDataServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MatchDataService");
    }
  }

  private static final class MatchDataServiceFileDescriptorSupplier
      extends MatchDataServiceBaseDescriptorSupplier {
    MatchDataServiceFileDescriptorSupplier() {}
  }

  private static final class MatchDataServiceMethodDescriptorSupplier
      extends MatchDataServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    MatchDataServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (MatchDataServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MatchDataServiceFileDescriptorSupplier())
              .addMethod(getSyncPlayerMethod())
              .addMethod(getGetSyncStatusMethod())
              .addMethod(getGetPlayerMatchStatsMethod())
              .addMethod(getDeletePlayerDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
