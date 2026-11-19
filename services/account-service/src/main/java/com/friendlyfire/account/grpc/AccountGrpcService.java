package com.friendlyfire.account.grpc;

import com.friendlyfire.account.datastatus.DataStatusService;
import com.friendlyfire.account.riotlink.RiotAccountLink;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
import com.friendlyfire.account.riotlink.RiotLinkService;
import com.friendlyfire.account.user.AccountDeletion;
import com.friendlyfire.account.user.AccountUser;
import com.friendlyfire.account.user.DisplayNames;
import com.friendlyfire.account.user.EnsuredUser;
import com.friendlyfire.account.user.NewUserNames;
import com.friendlyfire.account.user.UserRepository;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.FinishOnboardingRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.FinishOnboardingResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.RiotLink;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * friendlyfire.account.v1.AccountService.
 *
 * <p>Errors from the contract are gRPC statuses thrown by the classes below and returned
 * here as they are. When the database can't be reached, DatabaseErrorHandler answers
 * UNAVAILABLE. When Match Data or Scoring can't be reached, anything that deletes data
 * answers UNAVAILABLE and changes nothing; sign-in and linking carry on without the sync.
 */
@Service
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final UserRepository users;
    private final NewUserNames newUserNames;
    private final RiotLinkRepository riotLinks;
    private final RiotLinkService riotLinking;
    private final DataStatusService dataStatus;
    private final AccountDeletion deletion;
    private final Clock clock;

    public AccountGrpcService(
            UserRepository users,
            NewUserNames newUserNames,
            RiotLinkRepository riotLinks,
            RiotLinkService riotLinking,
            DataStatusService dataStatus,
            AccountDeletion deletion,
            Clock clock) {
        this.users = users;
        this.newUserNames = newUserNames;
        this.riotLinks = riotLinks;
        this.riotLinking = riotLinking;
        this.dataStatus = dataStatus;
        this.deletion = deletion;
        this.clock = clock;
    }

    @Override
    public void ensureUser(EnsureUserRequest request, StreamObserver<EnsureUserResponse> responseObserver) {
        respond(responseObserver, () -> {
            EnsuredUser ensured = users.ensure(
                    request.getAuthSubject(),
                    request.getEmail(),
                    newUserNames.from(request.getSuggestedDisplayName()));
            AccountUser user = ensured.user();
            Optional<RiotAccountLink> link = riotLinks.findByUserId(user.userId());
            // Every sign-in and app load: start a sync if their data is stale (never fails).
            dataStatus.syncIfStale(link);
            return EnsureUserResponse.newBuilder()
                    .setUser(toProto(user, link))
                    .setCreated(ensured.created())
                    .build();
        });
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        respond(responseObserver, () -> GetUserResponse.newBuilder()
                .setUser(existingUser(request.getUserId()))
                .build());
    }

    @Override
    public void updateDisplayName(
            UpdateDisplayNameRequest request, StreamObserver<UpdateDisplayNameResponse> responseObserver) {
        respond(responseObserver, () -> {
            String name = DisplayNames.valid(request.getDisplayName()).orElseThrow(() -> Status.INVALID_ARGUMENT
                    .withDescription("Your name needs 1 to " + NewUserNames.MAX_LENGTH + " characters.")
                    .asRuntimeException());
            AccountUser user = userId(request.getUserId())
                    .flatMap(id -> users.changeDisplayName(id, name))
                    .orElseThrow(AccountGrpcService::notFound);
            return UpdateDisplayNameResponse.newBuilder()
                    .setUser(toProto(user, riotLinks.findByUserId(user.userId())))
                    .build();
        });
    }

    @Override
    public void startRiotLink(StartRiotLinkRequest request, StreamObserver<StartRiotLinkResponse> responseObserver) {
        respond(responseObserver, () -> {
            UUID userId = existingUserId(request.getUserId());
            return StartRiotLinkResponse.newBuilder()
                    .setAuthorizeUrl(riotLinking.start(userId, request.getRedirectUri()))
                    .build();
        });
    }

    @Override
    public void completeRiotLink(
            CompleteRiotLinkRequest request, StreamObserver<CompleteRiotLinkResponse> responseObserver) {
        respond(responseObserver, () -> {
            // An unknown user can't have a state from StartRiotLink, so that check covers it.
            UUID userId = userId(request.getUserId()).orElseThrow(() -> Status.INVALID_ARGUMENT
                    .withDescription("That link request wasn't started here.")
                    .asRuntimeException());
            RiotAccountLink link = riotLinking.complete(
                    userId, request.getCode(), request.getState(), request.getRedirectUri());
            return CompleteRiotLinkResponse.newBuilder().setRiotLink(toProto(link)).build();
        });
    }

    @Override
    public void unlinkRiot(UnlinkRiotRequest request, StreamObserver<UnlinkRiotResponse> responseObserver) {
        respond(responseObserver, () -> {
            userId(request.getUserId()).ifPresent(riotLinking::unlink);
            return UnlinkRiotResponse.getDefaultInstance();
        });
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        respond(responseObserver, () -> {
            userId(request.getUserId()).ifPresent(deletion::delete);
            return DeleteAccountResponse.getDefaultInstance();
        });
    }

    @Override
    public void getDataStatus(GetDataStatusRequest request, StreamObserver<GetDataStatusResponse> responseObserver) {
        respond(responseObserver, () -> GetDataStatusResponse.newBuilder()
                .addAllStatuses(dataStatus.forUsers(request.getUserIdsList()))
                .build());
    }

    @Override
    public void finishOnboarding(
            FinishOnboardingRequest request, StreamObserver<FinishOnboardingResponse> responseObserver) {
        respond(responseObserver, () -> {
            AccountUser user = userId(request.getUserId())
                    .flatMap(id -> users.finishOnboarding(id, clock.instant()))
                    .orElseThrow(AccountGrpcService::notFound);
            return FinishOnboardingResponse.newBuilder()
                    .setUser(toProto(user, riotLinks.findByUserId(user.userId())))
                    .build();
        });
    }

    /**
     * Answers with the response, or with the gRPC status it throws. Anything else (a
     * database failure) goes to the exception handlers.
     */
    private static <T> void respond(StreamObserver<T> responseObserver, Supplier<T> body) {
        T response;
        try {
            response = body.get();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
            return;
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private User existingUser(String userId) {
        AccountUser user = userId(userId).flatMap(users::findById).orElseThrow(AccountGrpcService::notFound);
        return toProto(user, riotLinks.findByUserId(user.userId()));
    }

    private UUID existingUserId(String userId) {
        return userId(userId).filter(id -> users.findById(id).isPresent()).orElseThrow(AccountGrpcService::notFound);
    }

    // An ID that isn't a UUID can't belong to anyone, so callers treat it like an unknown user.
    private static Optional<UUID> userId(String userId) {
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static StatusRuntimeException notFound() {
        return Status.NOT_FOUND.withDescription("user not found").asRuntimeException();
    }

    // Every RPC that returns a User passes the link, so none can leave it out by mistake.
    private static User toProto(AccountUser user, Optional<RiotAccountLink> riotLink) {
        var builder = User.newBuilder()
                .setUserId(user.userId().toString())
                .setEmail(user.email())
                .setDisplayName(user.displayName())
                .setCreatedAt(timestamp(user.createdAt()));
        if (user.onboardedAt() != null) {
            builder.setOnboardedAt(timestamp(user.onboardedAt()));
        }
        riotLink.ifPresent(link -> builder.setRiotLink(toProto(link)));
        return builder.build();
    }

    // RiotLink has no PUUID field: the PUUID never leaves this service.
    private static RiotLink toProto(RiotAccountLink link) {
        return RiotLink.newBuilder()
                .setGameName(link.gameName())
                .setTagLine(link.tagLine())
                .setRegion(link.region())
                .setLinkedAt(timestamp(link.linkedAt()))
                .build();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
