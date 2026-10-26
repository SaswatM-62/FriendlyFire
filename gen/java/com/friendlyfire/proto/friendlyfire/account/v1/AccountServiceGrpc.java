package com.friendlyfire.proto.friendlyfire.account.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class AccountServiceGrpc {

  private AccountServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.account.v1.AccountService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> getEnsureUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EnsureUser",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> getEnsureUserMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest, com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> getEnsureUserMethod;
    if ((getEnsureUserMethod = AccountServiceGrpc.getEnsureUserMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getEnsureUserMethod = AccountServiceGrpc.getEnsureUserMethod) == null) {
          AccountServiceGrpc.getEnsureUserMethod = getEnsureUserMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest, com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EnsureUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("EnsureUser"))
              .build();
        }
      }
    }
    return getEnsureUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> getGetUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetUser",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> getGetUserMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest, com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> getGetUserMethod;
    if ((getGetUserMethod = AccountServiceGrpc.getGetUserMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getGetUserMethod = AccountServiceGrpc.getGetUserMethod) == null) {
          AccountServiceGrpc.getGetUserMethod = getGetUserMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest, com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("GetUser"))
              .build();
        }
      }
    }
    return getGetUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> getUpdateDisplayNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateDisplayName",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> getUpdateDisplayNameMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest, com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> getUpdateDisplayNameMethod;
    if ((getUpdateDisplayNameMethod = AccountServiceGrpc.getUpdateDisplayNameMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getUpdateDisplayNameMethod = AccountServiceGrpc.getUpdateDisplayNameMethod) == null) {
          AccountServiceGrpc.getUpdateDisplayNameMethod = getUpdateDisplayNameMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest, com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateDisplayName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("UpdateDisplayName"))
              .build();
        }
      }
    }
    return getUpdateDisplayNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> getStartRiotLinkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartRiotLink",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> getStartRiotLinkMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest, com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> getStartRiotLinkMethod;
    if ((getStartRiotLinkMethod = AccountServiceGrpc.getStartRiotLinkMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getStartRiotLinkMethod = AccountServiceGrpc.getStartRiotLinkMethod) == null) {
          AccountServiceGrpc.getStartRiotLinkMethod = getStartRiotLinkMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest, com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartRiotLink"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("StartRiotLink"))
              .build();
        }
      }
    }
    return getStartRiotLinkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> getCompleteRiotLinkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CompleteRiotLink",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> getCompleteRiotLinkMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest, com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> getCompleteRiotLinkMethod;
    if ((getCompleteRiotLinkMethod = AccountServiceGrpc.getCompleteRiotLinkMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getCompleteRiotLinkMethod = AccountServiceGrpc.getCompleteRiotLinkMethod) == null) {
          AccountServiceGrpc.getCompleteRiotLinkMethod = getCompleteRiotLinkMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest, com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CompleteRiotLink"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("CompleteRiotLink"))
              .build();
        }
      }
    }
    return getCompleteRiotLinkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> getUnlinkRiotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UnlinkRiot",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> getUnlinkRiotMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest, com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> getUnlinkRiotMethod;
    if ((getUnlinkRiotMethod = AccountServiceGrpc.getUnlinkRiotMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getUnlinkRiotMethod = AccountServiceGrpc.getUnlinkRiotMethod) == null) {
          AccountServiceGrpc.getUnlinkRiotMethod = getUnlinkRiotMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest, com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UnlinkRiot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("UnlinkRiot"))
              .build();
        }
      }
    }
    return getUnlinkRiotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> getDeleteAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteAccount",
      requestType = com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest,
      com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> getDeleteAccountMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest, com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> getDeleteAccountMethod;
    if ((getDeleteAccountMethod = AccountServiceGrpc.getDeleteAccountMethod) == null) {
      synchronized (AccountServiceGrpc.class) {
        if ((getDeleteAccountMethod = AccountServiceGrpc.getDeleteAccountMethod) == null) {
          AccountServiceGrpc.getDeleteAccountMethod = getDeleteAccountMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest, com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AccountServiceMethodDescriptorSupplier("DeleteAccount"))
              .build();
        }
      }
    }
    return getDeleteAccountMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AccountServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AccountServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AccountServiceStub>() {
        @java.lang.Override
        public AccountServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AccountServiceStub(channel, callOptions);
        }
      };
    return AccountServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static AccountServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AccountServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AccountServiceBlockingV2Stub>() {
        @java.lang.Override
        public AccountServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AccountServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return AccountServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AccountServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AccountServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AccountServiceBlockingStub>() {
        @java.lang.Override
        public AccountServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AccountServiceBlockingStub(channel, callOptions);
        }
      };
    return AccountServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AccountServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AccountServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AccountServiceFutureStub>() {
        @java.lang.Override
        public AccountServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AccountServiceFutureStub(channel, callOptions);
        }
      };
    return AccountServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Finds the user for an Auth0 subject, or creates one on their first visit.
     * If the user has a linked Riot account, this also asks Match Data to
     * fetch any new matches.
     * </pre>
     */
    default void ensureUser(com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEnsureUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns one user. NOT_FOUND if the user doesn't exist.
     * </pre>
     */
    default void getUser(com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetUserMethod(), responseObserver);
    }

    /**
     * <pre>
     * Changes the name other players see. INVALID_ARGUMENT if the name is
     * empty or longer than 24 characters.
     * </pre>
     */
    default void updateDisplayName(com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateDisplayNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Step 1 of linking a Riot account: returns the Riot Sign-On URL to send
     * the browser to. The one-time `state` value is stored here and is
     * already inside the URL.
     * </pre>
     */
    default void startRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStartRiotLinkMethod(), responseObserver);
    }

    /**
     * <pre>
     * Step 2, after Riot redirects back with a code. Checks `state`, swaps the
     * code for a token, looks up the player's PUUID and Riot ID, saves the
     * link, starts a match sync, and then throws the Riot token away.
     * Errors:
     *   INVALID_ARGUMENT  `state` is missing, expired, or belongs to another user
     *   ALREADY_EXISTS    this Riot account is linked to a different user
     *   UNAVAILABLE       Riot Sign-On could not be reached
     * </pre>
     */
    default void completeRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCompleteRiotLinkMethod(), responseObserver);
    }

    /**
     * <pre>
     * Removes the Riot link and deletes all of that player's match data.
     * </pre>
     */
    default void unlinkRiot(com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnlinkRiotMethod(), responseObserver);
    }

    /**
     * <pre>
     * Deletes the user, their Riot link and their match data.
     * </pre>
     */
    default void deleteAccount(com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAccountMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AccountService.
   */
  public static abstract class AccountServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AccountServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AccountService.
   */
  public static final class AccountServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AccountServiceStub> {
    private AccountServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AccountServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AccountServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Finds the user for an Auth0 subject, or creates one on their first visit.
     * If the user has a linked Riot account, this also asks Match Data to
     * fetch any new matches.
     * </pre>
     */
    public void ensureUser(com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEnsureUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns one user. NOT_FOUND if the user doesn't exist.
     * </pre>
     */
    public void getUser(com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Changes the name other players see. INVALID_ARGUMENT if the name is
     * empty or longer than 24 characters.
     * </pre>
     */
    public void updateDisplayName(com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateDisplayNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Step 1 of linking a Riot account: returns the Riot Sign-On URL to send
     * the browser to. The one-time `state` value is stored here and is
     * already inside the URL.
     * </pre>
     */
    public void startRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStartRiotLinkMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Step 2, after Riot redirects back with a code. Checks `state`, swaps the
     * code for a token, looks up the player's PUUID and Riot ID, saves the
     * link, starts a match sync, and then throws the Riot token away.
     * Errors:
     *   INVALID_ARGUMENT  `state` is missing, expired, or belongs to another user
     *   ALREADY_EXISTS    this Riot account is linked to a different user
     *   UNAVAILABLE       Riot Sign-On could not be reached
     * </pre>
     */
    public void completeRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCompleteRiotLinkMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Removes the Riot link and deletes all of that player's match data.
     * </pre>
     */
    public void unlinkRiot(com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnlinkRiotMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Deletes the user, their Riot link and their match data.
     * </pre>
     */
    public void deleteAccount(com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAccountMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AccountService.
   */
  public static final class AccountServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<AccountServiceBlockingV2Stub> {
    private AccountServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AccountServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AccountServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Finds the user for an Auth0 subject, or creates one on their first visit.
     * If the user has a linked Riot account, this also asks Match Data to
     * fetch any new matches.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse ensureUser(com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getEnsureUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns one user. NOT_FOUND if the user doesn't exist.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse getUser(com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Changes the name other players see. INVALID_ARGUMENT if the name is
     * empty or longer than 24 characters.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse updateDisplayName(com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateDisplayNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Step 1 of linking a Riot account: returns the Riot Sign-On URL to send
     * the browser to. The one-time `state` value is stored here and is
     * already inside the URL.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse startRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getStartRiotLinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Step 2, after Riot redirects back with a code. Checks `state`, swaps the
     * code for a token, looks up the player's PUUID and Riot ID, saves the
     * link, starts a match sync, and then throws the Riot token away.
     * Errors:
     *   INVALID_ARGUMENT  `state` is missing, expired, or belongs to another user
     *   ALREADY_EXISTS    this Riot account is linked to a different user
     *   UNAVAILABLE       Riot Sign-On could not be reached
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse completeRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCompleteRiotLinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Removes the Riot link and deletes all of that player's match data.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse unlinkRiot(com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUnlinkRiotMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes the user, their Riot link and their match data.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse deleteAccount(com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteAccountMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service AccountService.
   */
  public static final class AccountServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AccountServiceBlockingStub> {
    private AccountServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AccountServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AccountServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Finds the user for an Auth0 subject, or creates one on their first visit.
     * If the user has a linked Riot account, this also asks Match Data to
     * fetch any new matches.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse ensureUser(com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEnsureUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns one user. NOT_FOUND if the user doesn't exist.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse getUser(com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetUserMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Changes the name other players see. INVALID_ARGUMENT if the name is
     * empty or longer than 24 characters.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse updateDisplayName(com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateDisplayNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Step 1 of linking a Riot account: returns the Riot Sign-On URL to send
     * the browser to. The one-time `state` value is stored here and is
     * already inside the URL.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse startRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStartRiotLinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Step 2, after Riot redirects back with a code. Checks `state`, swaps the
     * code for a token, looks up the player's PUUID and Riot ID, saves the
     * link, starts a match sync, and then throws the Riot token away.
     * Errors:
     *   INVALID_ARGUMENT  `state` is missing, expired, or belongs to another user
     *   ALREADY_EXISTS    this Riot account is linked to a different user
     *   UNAVAILABLE       Riot Sign-On could not be reached
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse completeRiotLink(com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCompleteRiotLinkMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Removes the Riot link and deletes all of that player's match data.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse unlinkRiot(com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnlinkRiotMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Deletes the user, their Riot link and their match data.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse deleteAccount(com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAccountMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AccountService.
   */
  public static final class AccountServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AccountServiceFutureStub> {
    private AccountServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AccountServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AccountServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Finds the user for an Auth0 subject, or creates one on their first visit.
     * If the user has a linked Riot account, this also asks Match Data to
     * fetch any new matches.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse> ensureUser(
        com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEnsureUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns one user. NOT_FOUND if the user doesn't exist.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse> getUser(
        com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetUserMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Changes the name other players see. INVALID_ARGUMENT if the name is
     * empty or longer than 24 characters.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse> updateDisplayName(
        com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateDisplayNameMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Step 1 of linking a Riot account: returns the Riot Sign-On URL to send
     * the browser to. The one-time `state` value is stored here and is
     * already inside the URL.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse> startRiotLink(
        com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStartRiotLinkMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Step 2, after Riot redirects back with a code. Checks `state`, swaps the
     * code for a token, looks up the player's PUUID and Riot ID, saves the
     * link, starts a match sync, and then throws the Riot token away.
     * Errors:
     *   INVALID_ARGUMENT  `state` is missing, expired, or belongs to another user
     *   ALREADY_EXISTS    this Riot account is linked to a different user
     *   UNAVAILABLE       Riot Sign-On could not be reached
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse> completeRiotLink(
        com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCompleteRiotLinkMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Removes the Riot link and deletes all of that player's match data.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse> unlinkRiot(
        com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnlinkRiotMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Deletes the user, their Riot link and their match data.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse> deleteAccount(
        com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAccountMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ENSURE_USER = 0;
  private static final int METHODID_GET_USER = 1;
  private static final int METHODID_UPDATE_DISPLAY_NAME = 2;
  private static final int METHODID_START_RIOT_LINK = 3;
  private static final int METHODID_COMPLETE_RIOT_LINK = 4;
  private static final int METHODID_UNLINK_RIOT = 5;
  private static final int METHODID_DELETE_ACCOUNT = 6;

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
        case METHODID_ENSURE_USER:
          serviceImpl.ensureUser((com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse>) responseObserver);
          break;
        case METHODID_GET_USER:
          serviceImpl.getUser((com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse>) responseObserver);
          break;
        case METHODID_UPDATE_DISPLAY_NAME:
          serviceImpl.updateDisplayName((com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse>) responseObserver);
          break;
        case METHODID_START_RIOT_LINK:
          serviceImpl.startRiotLink((com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse>) responseObserver);
          break;
        case METHODID_COMPLETE_RIOT_LINK:
          serviceImpl.completeRiotLink((com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse>) responseObserver);
          break;
        case METHODID_UNLINK_RIOT:
          serviceImpl.unlinkRiot((com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse>) responseObserver);
          break;
        case METHODID_DELETE_ACCOUNT:
          serviceImpl.deleteAccount((com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse>) responseObserver);
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
          getEnsureUserMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse>(
                service, METHODID_ENSURE_USER)))
        .addMethod(
          getGetUserMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse>(
                service, METHODID_GET_USER)))
        .addMethod(
          getUpdateDisplayNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse>(
                service, METHODID_UPDATE_DISPLAY_NAME)))
        .addMethod(
          getStartRiotLinkMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse>(
                service, METHODID_START_RIOT_LINK)))
        .addMethod(
          getCompleteRiotLinkMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse>(
                service, METHODID_COMPLETE_RIOT_LINK)))
        .addMethod(
          getUnlinkRiotMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse>(
                service, METHODID_UNLINK_RIOT)))
        .addMethod(
          getDeleteAccountMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest,
              com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse>(
                service, METHODID_DELETE_ACCOUNT)))
        .build();
  }

  private static abstract class AccountServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AccountServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AccountService");
    }
  }

  private static final class AccountServiceFileDescriptorSupplier
      extends AccountServiceBaseDescriptorSupplier {
    AccountServiceFileDescriptorSupplier() {}
  }

  private static final class AccountServiceMethodDescriptorSupplier
      extends AccountServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AccountServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AccountServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AccountServiceFileDescriptorSupplier())
              .addMethod(getEnsureUserMethod())
              .addMethod(getGetUserMethod())
              .addMethod(getUpdateDisplayNameMethod())
              .addMethod(getStartRiotLinkMethod())
              .addMethod(getCompleteRiotLinkMethod())
              .addMethod(getUnlinkRiotMethod())
              .addMethod(getDeleteAccountMethod())
              .build();
        }
      }
    }
    return result;
  }
}
