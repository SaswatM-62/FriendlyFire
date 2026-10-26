package com.friendlyfire.proto.friendlyfire.room.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class RoomServiceGrpc {

  private RoomServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "friendlyfire.room.v1.RoomService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> getCreateRoomMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateRoom",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> getCreateRoomMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> getCreateRoomMethod;
    if ((getCreateRoomMethod = RoomServiceGrpc.getCreateRoomMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getCreateRoomMethod = RoomServiceGrpc.getCreateRoomMethod) == null) {
          RoomServiceGrpc.getCreateRoomMethod = getCreateRoomMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateRoom"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("CreateRoom"))
              .build();
        }
      }
    }
    return getCreateRoomMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> getJoinRoomMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "JoinRoom",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> getJoinRoomMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> getJoinRoomMethod;
    if ((getJoinRoomMethod = RoomServiceGrpc.getJoinRoomMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getJoinRoomMethod = RoomServiceGrpc.getJoinRoomMethod) == null) {
          RoomServiceGrpc.getJoinRoomMethod = getJoinRoomMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "JoinRoom"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("JoinRoom"))
              .build();
        }
      }
    }
    return getJoinRoomMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> getLeaveRoomMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LeaveRoom",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> getLeaveRoomMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> getLeaveRoomMethod;
    if ((getLeaveRoomMethod = RoomServiceGrpc.getLeaveRoomMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getLeaveRoomMethod = RoomServiceGrpc.getLeaveRoomMethod) == null) {
          RoomServiceGrpc.getLeaveRoomMethod = getLeaveRoomMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest, com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LeaveRoom"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("LeaveRoom"))
              .build();
        }
      }
    }
    return getLeaveRoomMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> getKickPlayerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "KickPlayer",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> getKickPlayerMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest, com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> getKickPlayerMethod;
    if ((getKickPlayerMethod = RoomServiceGrpc.getKickPlayerMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getKickPlayerMethod = RoomServiceGrpc.getKickPlayerMethod) == null) {
          RoomServiceGrpc.getKickPlayerMethod = getKickPlayerMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest, com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "KickPlayer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("KickPlayer"))
              .build();
        }
      }
    }
    return getKickPlayerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> getUpdateSettingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateSettings",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> getUpdateSettingsMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest, com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> getUpdateSettingsMethod;
    if ((getUpdateSettingsMethod = RoomServiceGrpc.getUpdateSettingsMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getUpdateSettingsMethod = RoomServiceGrpc.getUpdateSettingsMethod) == null) {
          RoomServiceGrpc.getUpdateSettingsMethod = getUpdateSettingsMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest, com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateSettings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("UpdateSettings"))
              .build();
        }
      }
    }
    return getUpdateSettingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> getStartQuizMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartQuiz",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> getStartQuizMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest, com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> getStartQuizMethod;
    if ((getStartQuizMethod = RoomServiceGrpc.getStartQuizMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getStartQuizMethod = RoomServiceGrpc.getStartQuizMethod) == null) {
          RoomServiceGrpc.getStartQuizMethod = getStartQuizMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest, com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartQuiz"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("StartQuiz"))
              .build();
        }
      }
    }
    return getStartQuizMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> getSubmitAnswerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubmitAnswer",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> getSubmitAnswerMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest, com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> getSubmitAnswerMethod;
    if ((getSubmitAnswerMethod = RoomServiceGrpc.getSubmitAnswerMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getSubmitAnswerMethod = RoomServiceGrpc.getSubmitAnswerMethod) == null) {
          RoomServiceGrpc.getSubmitAnswerMethod = getSubmitAnswerMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest, com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubmitAnswer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("SubmitAnswer"))
              .build();
        }
      }
    }
    return getSubmitAnswerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> getReportDisconnectedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportDisconnected",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> getReportDisconnectedMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest, com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> getReportDisconnectedMethod;
    if ((getReportDisconnectedMethod = RoomServiceGrpc.getReportDisconnectedMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getReportDisconnectedMethod = RoomServiceGrpc.getReportDisconnectedMethod) == null) {
          RoomServiceGrpc.getReportDisconnectedMethod = getReportDisconnectedMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest, com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportDisconnected"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("ReportDisconnected"))
              .build();
        }
      }
    }
    return getReportDisconnectedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> getReportConnectionQualityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportConnectionQuality",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> getReportConnectionQualityMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest, com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> getReportConnectionQualityMethod;
    if ((getReportConnectionQualityMethod = RoomServiceGrpc.getReportConnectionQualityMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getReportConnectionQualityMethod = RoomServiceGrpc.getReportConnectionQualityMethod) == null) {
          RoomServiceGrpc.getReportConnectionQualityMethod = getReportConnectionQualityMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest, com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportConnectionQuality"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("ReportConnectionQuality"))
              .build();
        }
      }
    }
    return getReportConnectionQualityMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> getCastVoteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CastVote",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> getCastVoteMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest, com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> getCastVoteMethod;
    if ((getCastVoteMethod = RoomServiceGrpc.getCastVoteMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getCastVoteMethod = RoomServiceGrpc.getCastVoteMethod) == null) {
          RoomServiceGrpc.getCastVoteMethod = getCastVoteMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest, com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CastVote"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("CastVote"))
              .build();
        }
      }
    }
    return getCastVoteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> getGetRoomSnapshotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRoomSnapshot",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> getGetRoomSnapshotMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest, com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> getGetRoomSnapshotMethod;
    if ((getGetRoomSnapshotMethod = RoomServiceGrpc.getGetRoomSnapshotMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getGetRoomSnapshotMethod = RoomServiceGrpc.getGetRoomSnapshotMethod) == null) {
          RoomServiceGrpc.getGetRoomSnapshotMethod = getGetRoomSnapshotMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest, com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRoomSnapshot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("GetRoomSnapshot"))
              .build();
        }
      }
    }
    return getGetRoomSnapshotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> getStreamRoomEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamRoomEvents",
      requestType = com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest.class,
      responseType = com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest,
      com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> getStreamRoomEventsMethod() {
    io.grpc.MethodDescriptor<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest, com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> getStreamRoomEventsMethod;
    if ((getStreamRoomEventsMethod = RoomServiceGrpc.getStreamRoomEventsMethod) == null) {
      synchronized (RoomServiceGrpc.class) {
        if ((getStreamRoomEventsMethod = RoomServiceGrpc.getStreamRoomEventsMethod) == null) {
          RoomServiceGrpc.getStreamRoomEventsMethod = getStreamRoomEventsMethod =
              io.grpc.MethodDescriptor.<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest, com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamRoomEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RoomServiceMethodDescriptorSupplier("StreamRoomEvents"))
              .build();
        }
      }
    }
    return getStreamRoomEventsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RoomServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RoomServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RoomServiceStub>() {
        @java.lang.Override
        public RoomServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RoomServiceStub(channel, callOptions);
        }
      };
    return RoomServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static RoomServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RoomServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RoomServiceBlockingV2Stub>() {
        @java.lang.Override
        public RoomServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RoomServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return RoomServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RoomServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RoomServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RoomServiceBlockingStub>() {
        @java.lang.Override
        public RoomServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RoomServiceBlockingStub(channel, callOptions);
        }
      };
    return RoomServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RoomServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RoomServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RoomServiceFutureStub>() {
        @java.lang.Override
        public RoomServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RoomServiceFutureStub(channel, callOptions);
        }
      };
    return RoomServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Creates a room with the caller as host and returns it.
     * </pre>
     */
    default void createRoom(com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateRoomMethod(), responseObserver);
    }

    /**
     * <pre>
     * Adds the user to a room. If they're already in it (after a page
     * refresh or dropped connection), marks them connected again instead.
     * Errors:
     *   RESOURCE_EXHAUSTED   the room already has 8 players
     *   FAILED_PRECONDITION  the quiz has started and the user wasn't in it
     * </pre>
     */
    default void joinRoom(com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getJoinRoomMethod(), responseObserver);
    }

    /**
     * <pre>
     * The user leaves for good. If the host leaves, the player who joined
     * next becomes host. The room closes when the last player leaves.
     * </pre>
     */
    default void leaveRoom(com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLeaveRoomMethod(), responseObserver);
    }

    /**
     * <pre>
     * Host only. Removes another player. Only allowed in the lobby.
     * </pre>
     */
    default void kickPlayer(com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getKickPlayerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Host only. Only allowed in the lobby.
     * INVALID_ARGUMENT if a value is outside the allowed range.
     * </pre>
     */
    default void updateSettings(com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateSettingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Host only. Allowed in the lobby, or after a quiz finishes to play again.
     * Generates all questions, then starts a short countdown.
     * FAILED_PRECONDITION (with a message for the host) if there are fewer
     * than 2 players, someone's matches haven't finished syncing, or there
     * isn't enough match data for the chosen number of questions.
     * </pre>
     */
    default void startQuiz(com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStartQuizMethod(), responseObserver);
    }

    /**
     * <pre>
     * Records a player's answer to the current question, using the time it
     * arrives here (not a time sent by the browser).
     * Errors:
     *   FAILED_PRECONDITION  no question is open, the question_id isn't the
     *                        current one, or time is up
     *   ALREADY_EXISTS       the player already answered this question
     *   INVALID_ARGUMENT     option_id isn't one of the question's options
     * </pre>
     */
    default void submitAnswer(com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSubmitAnswerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Called by the Gateway when a player's WebSocket connection drops.
     * During a quiz, if they don't come back within 15 seconds, the room
     * pauses and the others vote on what to do.
     * </pre>
     */
    default void reportDisconnected(com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportDisconnectedMethod(), responseObserver);
    }

    /**
     * <pre>
     * Called by the Gateway with a player's latest connection quality (from
     * the ping messages behind the network bar). Room events are only sent
     * when the quality level actually changes.
     * </pre>
     */
    default void reportConnectionQuality(com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportConnectionQualityMethod(), responseObserver);
    }

    /**
     * <pre>
     * A connected player's vote while the room is in ROOM_PHASE_VOTE.
     * The vote ends when everyone has voted or its deadline passes.
     * The option with the most votes wins; a tie means WAIT.
     * </pre>
     */
    default void castVote(com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCastVoteMethod(), responseObserver);
    }

    /**
     * <pre>
     * The current state of the room.
     * </pre>
     */
    default void getRoomSnapshot(com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRoomSnapshotMethod(), responseObserver);
    }

    /**
     * <pre>
     * A live feed of everything that happens in a room. The first message is
     * always ROOM_EVENT_TYPE_SNAPSHOT. The stream ends when the room closes.
     * The Gateway opens one stream per room and forwards each event to the
     * STOMP topic /topic/room/{room_code}.
     * </pre>
     */
    default void streamRoomEvents(com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStreamRoomEventsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RoomService.
   */
  public static abstract class RoomServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RoomServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RoomService.
   */
  public static final class RoomServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RoomServiceStub> {
    private RoomServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RoomServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RoomServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a room with the caller as host and returns it.
     * </pre>
     */
    public void createRoom(com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateRoomMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Adds the user to a room. If they're already in it (after a page
     * refresh or dropped connection), marks them connected again instead.
     * Errors:
     *   RESOURCE_EXHAUSTED   the room already has 8 players
     *   FAILED_PRECONDITION  the quiz has started and the user wasn't in it
     * </pre>
     */
    public void joinRoom(com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getJoinRoomMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The user leaves for good. If the host leaves, the player who joined
     * next becomes host. The room closes when the last player leaves.
     * </pre>
     */
    public void leaveRoom(com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLeaveRoomMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Host only. Removes another player. Only allowed in the lobby.
     * </pre>
     */
    public void kickPlayer(com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getKickPlayerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Host only. Only allowed in the lobby.
     * INVALID_ARGUMENT if a value is outside the allowed range.
     * </pre>
     */
    public void updateSettings(com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateSettingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Host only. Allowed in the lobby, or after a quiz finishes to play again.
     * Generates all questions, then starts a short countdown.
     * FAILED_PRECONDITION (with a message for the host) if there are fewer
     * than 2 players, someone's matches haven't finished syncing, or there
     * isn't enough match data for the chosen number of questions.
     * </pre>
     */
    public void startQuiz(com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStartQuizMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Records a player's answer to the current question, using the time it
     * arrives here (not a time sent by the browser).
     * Errors:
     *   FAILED_PRECONDITION  no question is open, the question_id isn't the
     *                        current one, or time is up
     *   ALREADY_EXISTS       the player already answered this question
     *   INVALID_ARGUMENT     option_id isn't one of the question's options
     * </pre>
     */
    public void submitAnswer(com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSubmitAnswerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Called by the Gateway when a player's WebSocket connection drops.
     * During a quiz, if they don't come back within 15 seconds, the room
     * pauses and the others vote on what to do.
     * </pre>
     */
    public void reportDisconnected(com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportDisconnectedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Called by the Gateway with a player's latest connection quality (from
     * the ping messages behind the network bar). Room events are only sent
     * when the quality level actually changes.
     * </pre>
     */
    public void reportConnectionQuality(com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportConnectionQualityMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * A connected player's vote while the room is in ROOM_PHASE_VOTE.
     * The vote ends when everyone has voted or its deadline passes.
     * The option with the most votes wins; a tie means WAIT.
     * </pre>
     */
    public void castVote(com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCastVoteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * The current state of the room.
     * </pre>
     */
    public void getRoomSnapshot(com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRoomSnapshotMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * A live feed of everything that happens in a room. The first message is
     * always ROOM_EVENT_TYPE_SNAPSHOT. The stream ends when the room closes.
     * The Gateway opens one stream per room and forwards each event to the
     * STOMP topic /topic/room/{room_code}.
     * </pre>
     */
    public void streamRoomEvents(com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest request,
        io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getStreamRoomEventsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RoomService.
   */
  public static final class RoomServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<RoomServiceBlockingV2Stub> {
    private RoomServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RoomServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RoomServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a room with the caller as host and returns it.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse createRoom(com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Adds the user to a room. If they're already in it (after a page
     * refresh or dropped connection), marks them connected again instead.
     * Errors:
     *   RESOURCE_EXHAUSTED   the room already has 8 players
     *   FAILED_PRECONDITION  the quiz has started and the user wasn't in it
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse joinRoom(com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getJoinRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The user leaves for good. If the host leaves, the player who joined
     * next becomes host. The room closes when the last player leaves.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse leaveRoom(com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getLeaveRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Removes another player. Only allowed in the lobby.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse kickPlayer(com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getKickPlayerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Only allowed in the lobby.
     * INVALID_ARGUMENT if a value is outside the allowed range.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse updateSettings(com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getUpdateSettingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Allowed in the lobby, or after a quiz finishes to play again.
     * Generates all questions, then starts a short countdown.
     * FAILED_PRECONDITION (with a message for the host) if there are fewer
     * than 2 players, someone's matches haven't finished syncing, or there
     * isn't enough match data for the chosen number of questions.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse startQuiz(com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getStartQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Records a player's answer to the current question, using the time it
     * arrives here (not a time sent by the browser).
     * Errors:
     *   FAILED_PRECONDITION  no question is open, the question_id isn't the
     *                        current one, or time is up
     *   ALREADY_EXISTS       the player already answered this question
     *   INVALID_ARGUMENT     option_id isn't one of the question's options
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse submitAnswer(com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getSubmitAnswerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Called by the Gateway when a player's WebSocket connection drops.
     * During a quiz, if they don't come back within 15 seconds, the room
     * pauses and the others vote on what to do.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse reportDisconnected(com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getReportDisconnectedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Called by the Gateway with a player's latest connection quality (from
     * the ping messages behind the network bar). Room events are only sent
     * when the quality level actually changes.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse reportConnectionQuality(com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getReportConnectionQualityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A connected player's vote while the room is in ROOM_PHASE_VOTE.
     * The vote ends when everyone has voted or its deadline passes.
     * The option with the most votes wins; a tie means WAIT.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse castVote(com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCastVoteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The current state of the room.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse getRoomSnapshot(com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetRoomSnapshotMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A live feed of everything that happens in a room. The first message is
     * always ROOM_EVENT_TYPE_SNAPSHOT. The stream ends when the room closes.
     * The Gateway opens one stream per room and forwards each event to the
     * STOMP topic /topic/room/{room_code}.
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse>
        streamRoomEvents(com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getStreamRoomEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service RoomService.
   */
  public static final class RoomServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RoomServiceBlockingStub> {
    private RoomServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RoomServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RoomServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a room with the caller as host and returns it.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse createRoom(com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Adds the user to a room. If they're already in it (after a page
     * refresh or dropped connection), marks them connected again instead.
     * Errors:
     *   RESOURCE_EXHAUSTED   the room already has 8 players
     *   FAILED_PRECONDITION  the quiz has started and the user wasn't in it
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse joinRoom(com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getJoinRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The user leaves for good. If the host leaves, the player who joined
     * next becomes host. The room closes when the last player leaves.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse leaveRoom(com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLeaveRoomMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Removes another player. Only allowed in the lobby.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse kickPlayer(com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getKickPlayerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Only allowed in the lobby.
     * INVALID_ARGUMENT if a value is outside the allowed range.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse updateSettings(com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateSettingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Host only. Allowed in the lobby, or after a quiz finishes to play again.
     * Generates all questions, then starts a short countdown.
     * FAILED_PRECONDITION (with a message for the host) if there are fewer
     * than 2 players, someone's matches haven't finished syncing, or there
     * isn't enough match data for the chosen number of questions.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse startQuiz(com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStartQuizMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Records a player's answer to the current question, using the time it
     * arrives here (not a time sent by the browser).
     * Errors:
     *   FAILED_PRECONDITION  no question is open, the question_id isn't the
     *                        current one, or time is up
     *   ALREADY_EXISTS       the player already answered this question
     *   INVALID_ARGUMENT     option_id isn't one of the question's options
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse submitAnswer(com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSubmitAnswerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Called by the Gateway when a player's WebSocket connection drops.
     * During a quiz, if they don't come back within 15 seconds, the room
     * pauses and the others vote on what to do.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse reportDisconnected(com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportDisconnectedMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Called by the Gateway with a player's latest connection quality (from
     * the ping messages behind the network bar). Room events are only sent
     * when the quality level actually changes.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse reportConnectionQuality(com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportConnectionQualityMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A connected player's vote while the room is in ROOM_PHASE_VOTE.
     * The vote ends when everyone has voted or its deadline passes.
     * The option with the most votes wins; a tie means WAIT.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse castVote(com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCastVoteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * The current state of the room.
     * </pre>
     */
    public com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse getRoomSnapshot(com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRoomSnapshotMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A live feed of everything that happens in a room. The first message is
     * always ROOM_EVENT_TYPE_SNAPSHOT. The stream ends when the room closes.
     * The Gateway opens one stream per room and forwards each event to the
     * STOMP topic /topic/room/{room_code}.
     * </pre>
     */
    public java.util.Iterator<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse> streamRoomEvents(
        com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getStreamRoomEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RoomService.
   */
  public static final class RoomServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RoomServiceFutureStub> {
    private RoomServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RoomServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RoomServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a room with the caller as host and returns it.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse> createRoom(
        com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateRoomMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Adds the user to a room. If they're already in it (after a page
     * refresh or dropped connection), marks them connected again instead.
     * Errors:
     *   RESOURCE_EXHAUSTED   the room already has 8 players
     *   FAILED_PRECONDITION  the quiz has started and the user wasn't in it
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse> joinRoom(
        com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getJoinRoomMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The user leaves for good. If the host leaves, the player who joined
     * next becomes host. The room closes when the last player leaves.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse> leaveRoom(
        com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLeaveRoomMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Host only. Removes another player. Only allowed in the lobby.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse> kickPlayer(
        com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getKickPlayerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Host only. Only allowed in the lobby.
     * INVALID_ARGUMENT if a value is outside the allowed range.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse> updateSettings(
        com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateSettingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Host only. Allowed in the lobby, or after a quiz finishes to play again.
     * Generates all questions, then starts a short countdown.
     * FAILED_PRECONDITION (with a message for the host) if there are fewer
     * than 2 players, someone's matches haven't finished syncing, or there
     * isn't enough match data for the chosen number of questions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse> startQuiz(
        com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStartQuizMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Records a player's answer to the current question, using the time it
     * arrives here (not a time sent by the browser).
     * Errors:
     *   FAILED_PRECONDITION  no question is open, the question_id isn't the
     *                        current one, or time is up
     *   ALREADY_EXISTS       the player already answered this question
     *   INVALID_ARGUMENT     option_id isn't one of the question's options
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse> submitAnswer(
        com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSubmitAnswerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Called by the Gateway when a player's WebSocket connection drops.
     * During a quiz, if they don't come back within 15 seconds, the room
     * pauses and the others vote on what to do.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse> reportDisconnected(
        com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportDisconnectedMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Called by the Gateway with a player's latest connection quality (from
     * the ping messages behind the network bar). Room events are only sent
     * when the quality level actually changes.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse> reportConnectionQuality(
        com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportConnectionQualityMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * A connected player's vote while the room is in ROOM_PHASE_VOTE.
     * The vote ends when everyone has voted or its deadline passes.
     * The option with the most votes wins; a tie means WAIT.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse> castVote(
        com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCastVoteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * The current state of the room.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse> getRoomSnapshot(
        com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRoomSnapshotMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_ROOM = 0;
  private static final int METHODID_JOIN_ROOM = 1;
  private static final int METHODID_LEAVE_ROOM = 2;
  private static final int METHODID_KICK_PLAYER = 3;
  private static final int METHODID_UPDATE_SETTINGS = 4;
  private static final int METHODID_START_QUIZ = 5;
  private static final int METHODID_SUBMIT_ANSWER = 6;
  private static final int METHODID_REPORT_DISCONNECTED = 7;
  private static final int METHODID_REPORT_CONNECTION_QUALITY = 8;
  private static final int METHODID_CAST_VOTE = 9;
  private static final int METHODID_GET_ROOM_SNAPSHOT = 10;
  private static final int METHODID_STREAM_ROOM_EVENTS = 11;

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
        case METHODID_CREATE_ROOM:
          serviceImpl.createRoom((com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse>) responseObserver);
          break;
        case METHODID_JOIN_ROOM:
          serviceImpl.joinRoom((com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse>) responseObserver);
          break;
        case METHODID_LEAVE_ROOM:
          serviceImpl.leaveRoom((com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse>) responseObserver);
          break;
        case METHODID_KICK_PLAYER:
          serviceImpl.kickPlayer((com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse>) responseObserver);
          break;
        case METHODID_UPDATE_SETTINGS:
          serviceImpl.updateSettings((com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse>) responseObserver);
          break;
        case METHODID_START_QUIZ:
          serviceImpl.startQuiz((com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse>) responseObserver);
          break;
        case METHODID_SUBMIT_ANSWER:
          serviceImpl.submitAnswer((com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse>) responseObserver);
          break;
        case METHODID_REPORT_DISCONNECTED:
          serviceImpl.reportDisconnected((com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse>) responseObserver);
          break;
        case METHODID_REPORT_CONNECTION_QUALITY:
          serviceImpl.reportConnectionQuality((com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse>) responseObserver);
          break;
        case METHODID_CAST_VOTE:
          serviceImpl.castVote((com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse>) responseObserver);
          break;
        case METHODID_GET_ROOM_SNAPSHOT:
          serviceImpl.getRoomSnapshot((com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse>) responseObserver);
          break;
        case METHODID_STREAM_ROOM_EVENTS:
          serviceImpl.streamRoomEvents((com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse>) responseObserver);
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
          getCreateRoomMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomResponse>(
                service, METHODID_CREATE_ROOM)))
        .addMethod(
          getJoinRoomMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomResponse>(
                service, METHODID_JOIN_ROOM)))
        .addMethod(
          getLeaveRoomMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.LeaveRoomResponse>(
                service, METHODID_LEAVE_ROOM)))
        .addMethod(
          getKickPlayerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.KickPlayerResponse>(
                service, METHODID_KICK_PLAYER)))
        .addMethod(
          getUpdateSettingsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.UpdateSettingsResponse>(
                service, METHODID_UPDATE_SETTINGS)))
        .addMethod(
          getStartQuizMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.StartQuizRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.StartQuizResponse>(
                service, METHODID_START_QUIZ)))
        .addMethod(
          getSubmitAnswerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.SubmitAnswerResponse>(
                service, METHODID_SUBMIT_ANSWER)))
        .addMethod(
          getReportDisconnectedMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.ReportDisconnectedResponse>(
                service, METHODID_REPORT_DISCONNECTED)))
        .addMethod(
          getReportConnectionQualityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.ReportConnectionQualityResponse>(
                service, METHODID_REPORT_CONNECTION_QUALITY)))
        .addMethod(
          getCastVoteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.CastVoteRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.CastVoteResponse>(
                service, METHODID_CAST_VOTE)))
        .addMethod(
          getGetRoomSnapshotMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse>(
                service, METHODID_GET_ROOM_SNAPSHOT)))
        .addMethod(
          getStreamRoomEventsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsRequest,
              com.friendlyfire.proto.friendlyfire.room.v1.StreamRoomEventsResponse>(
                service, METHODID_STREAM_ROOM_EVENTS)))
        .build();
  }

  private static abstract class RoomServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RoomServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RoomService");
    }
  }

  private static final class RoomServiceFileDescriptorSupplier
      extends RoomServiceBaseDescriptorSupplier {
    RoomServiceFileDescriptorSupplier() {}
  }

  private static final class RoomServiceMethodDescriptorSupplier
      extends RoomServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RoomServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (RoomServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RoomServiceFileDescriptorSupplier())
              .addMethod(getCreateRoomMethod())
              .addMethod(getJoinRoomMethod())
              .addMethod(getLeaveRoomMethod())
              .addMethod(getKickPlayerMethod())
              .addMethod(getUpdateSettingsMethod())
              .addMethod(getStartQuizMethod())
              .addMethod(getSubmitAnswerMethod())
              .addMethod(getReportDisconnectedMethod())
              .addMethod(getReportConnectionQualityMethod())
              .addMethod(getCastVoteMethod())
              .addMethod(getGetRoomSnapshotMethod())
              .addMethod(getStreamRoomEventsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
