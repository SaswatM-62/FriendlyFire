import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class RoomPhase(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    ROOM_PHASE_UNSPECIFIED: _ClassVar[RoomPhase]
    ROOM_PHASE_LOBBY: _ClassVar[RoomPhase]
    ROOM_PHASE_STARTING: _ClassVar[RoomPhase]
    ROOM_PHASE_QUESTION: _ClassVar[RoomPhase]
    ROOM_PHASE_REVEAL: _ClassVar[RoomPhase]
    ROOM_PHASE_VOTE: _ClassVar[RoomPhase]
    ROOM_PHASE_FINISHED: _ClassVar[RoomPhase]

class PlayerStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    PLAYER_STATUS_UNSPECIFIED: _ClassVar[PlayerStatus]
    PLAYER_STATUS_CONNECTED: _ClassVar[PlayerStatus]
    PLAYER_STATUS_DISCONNECTED: _ClassVar[PlayerStatus]
    PLAYER_STATUS_SITTING_OUT: _ClassVar[PlayerStatus]

class ConnectionQuality(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CONNECTION_QUALITY_UNSPECIFIED: _ClassVar[ConnectionQuality]
    CONNECTION_QUALITY_GOOD: _ClassVar[ConnectionQuality]
    CONNECTION_QUALITY_FAIR: _ClassVar[ConnectionQuality]
    CONNECTION_QUALITY_POOR: _ClassVar[ConnectionQuality]

class VoteOption(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    VOTE_OPTION_UNSPECIFIED: _ClassVar[VoteOption]
    VOTE_OPTION_WAIT: _ClassVar[VoteOption]
    VOTE_OPTION_CONTINUE_WITHOUT_PLAYER: _ClassVar[VoteOption]
    VOTE_OPTION_END_QUIZ: _ClassVar[VoteOption]

class RoomEventType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    ROOM_EVENT_TYPE_UNSPECIFIED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_SNAPSHOT: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_JOINED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_LEFT: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_KICKED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_DISCONNECTED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_RECONNECTED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_HOST_CHANGED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_SETTINGS_CHANGED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_DATA_READY: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_QUIZ_STARTING: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_QUESTION_STARTED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_PLAYER_ANSWERED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_QUESTION_REVEALED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_VOTE_STARTED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_VOTE_RESOLVED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_QUIZ_FINISHED: _ClassVar[RoomEventType]
    ROOM_EVENT_TYPE_ROOM_CLOSED: _ClassVar[RoomEventType]
ROOM_PHASE_UNSPECIFIED: RoomPhase
ROOM_PHASE_LOBBY: RoomPhase
ROOM_PHASE_STARTING: RoomPhase
ROOM_PHASE_QUESTION: RoomPhase
ROOM_PHASE_REVEAL: RoomPhase
ROOM_PHASE_VOTE: RoomPhase
ROOM_PHASE_FINISHED: RoomPhase
PLAYER_STATUS_UNSPECIFIED: PlayerStatus
PLAYER_STATUS_CONNECTED: PlayerStatus
PLAYER_STATUS_DISCONNECTED: PlayerStatus
PLAYER_STATUS_SITTING_OUT: PlayerStatus
CONNECTION_QUALITY_UNSPECIFIED: ConnectionQuality
CONNECTION_QUALITY_GOOD: ConnectionQuality
CONNECTION_QUALITY_FAIR: ConnectionQuality
CONNECTION_QUALITY_POOR: ConnectionQuality
VOTE_OPTION_UNSPECIFIED: VoteOption
VOTE_OPTION_WAIT: VoteOption
VOTE_OPTION_CONTINUE_WITHOUT_PLAYER: VoteOption
VOTE_OPTION_END_QUIZ: VoteOption
ROOM_EVENT_TYPE_UNSPECIFIED: RoomEventType
ROOM_EVENT_TYPE_SNAPSHOT: RoomEventType
ROOM_EVENT_TYPE_PLAYER_JOINED: RoomEventType
ROOM_EVENT_TYPE_PLAYER_LEFT: RoomEventType
ROOM_EVENT_TYPE_PLAYER_KICKED: RoomEventType
ROOM_EVENT_TYPE_PLAYER_DISCONNECTED: RoomEventType
ROOM_EVENT_TYPE_PLAYER_RECONNECTED: RoomEventType
ROOM_EVENT_TYPE_HOST_CHANGED: RoomEventType
ROOM_EVENT_TYPE_SETTINGS_CHANGED: RoomEventType
ROOM_EVENT_TYPE_PLAYER_DATA_READY: RoomEventType
ROOM_EVENT_TYPE_CONNECTION_QUALITY_CHANGED: RoomEventType
ROOM_EVENT_TYPE_QUIZ_STARTING: RoomEventType
ROOM_EVENT_TYPE_QUESTION_STARTED: RoomEventType
ROOM_EVENT_TYPE_PLAYER_ANSWERED: RoomEventType
ROOM_EVENT_TYPE_QUESTION_REVEALED: RoomEventType
ROOM_EVENT_TYPE_VOTE_STARTED: RoomEventType
ROOM_EVENT_TYPE_VOTE_RESOLVED: RoomEventType
ROOM_EVENT_TYPE_QUIZ_FINISHED: RoomEventType
ROOM_EVENT_TYPE_ROOM_CLOSED: RoomEventType

class RoomSnapshot(_message.Message):
    __slots__ = ("room_code", "host_user_id", "phase", "settings", "players", "quiz_id", "round_number", "current_question", "phase_deadline", "scoreboard", "active_vote")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    HOST_USER_ID_FIELD_NUMBER: _ClassVar[int]
    PHASE_FIELD_NUMBER: _ClassVar[int]
    SETTINGS_FIELD_NUMBER: _ClassVar[int]
    PLAYERS_FIELD_NUMBER: _ClassVar[int]
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    ROUND_NUMBER_FIELD_NUMBER: _ClassVar[int]
    CURRENT_QUESTION_FIELD_NUMBER: _ClassVar[int]
    PHASE_DEADLINE_FIELD_NUMBER: _ClassVar[int]
    SCOREBOARD_FIELD_NUMBER: _ClassVar[int]
    ACTIVE_VOTE_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    host_user_id: str
    phase: RoomPhase
    settings: RoomSettings
    players: _containers.RepeatedCompositeFieldContainer[Player]
    quiz_id: str
    round_number: int
    current_question: QuestionView
    phase_deadline: _timestamp_pb2.Timestamp
    scoreboard: _containers.RepeatedCompositeFieldContainer[ScoreEntry]
    active_vote: Vote
    def __init__(self, room_code: _Optional[str] = ..., host_user_id: _Optional[str] = ..., phase: _Optional[_Union[RoomPhase, str]] = ..., settings: _Optional[_Union[RoomSettings, _Mapping]] = ..., players: _Optional[_Iterable[_Union[Player, _Mapping]]] = ..., quiz_id: _Optional[str] = ..., round_number: _Optional[int] = ..., current_question: _Optional[_Union[QuestionView, _Mapping]] = ..., phase_deadline: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., scoreboard: _Optional[_Iterable[_Union[ScoreEntry, _Mapping]]] = ..., active_vote: _Optional[_Union[Vote, _Mapping]] = ...) -> None: ...

class RoomSettings(_message.Message):
    __slots__ = ("question_count", "seconds_per_question")
    QUESTION_COUNT_FIELD_NUMBER: _ClassVar[int]
    SECONDS_PER_QUESTION_FIELD_NUMBER: _ClassVar[int]
    question_count: int
    seconds_per_question: int
    def __init__(self, question_count: _Optional[int] = ..., seconds_per_question: _Optional[int] = ...) -> None: ...

class Player(_message.Message):
    __slots__ = ("user_id", "display_name", "status", "match_data_ready", "connection_quality", "answered_current_question")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    MATCH_DATA_READY_FIELD_NUMBER: _ClassVar[int]
    CONNECTION_QUALITY_FIELD_NUMBER: _ClassVar[int]
    ANSWERED_CURRENT_QUESTION_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    status: PlayerStatus
    match_data_ready: bool
    connection_quality: ConnectionQuality
    answered_current_question: bool
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ..., status: _Optional[_Union[PlayerStatus, str]] = ..., match_data_ready: _Optional[bool] = ..., connection_quality: _Optional[_Union[ConnectionQuality, str]] = ..., answered_current_question: _Optional[bool] = ...) -> None: ...

class QuestionView(_message.Message):
    __slots__ = ("question_id", "text", "options", "reveal")
    QUESTION_ID_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    OPTIONS_FIELD_NUMBER: _ClassVar[int]
    REVEAL_FIELD_NUMBER: _ClassVar[int]
    question_id: str
    text: str
    options: _containers.RepeatedCompositeFieldContainer[QuestionOption]
    reveal: Reveal
    def __init__(self, question_id: _Optional[str] = ..., text: _Optional[str] = ..., options: _Optional[_Iterable[_Union[QuestionOption, _Mapping]]] = ..., reveal: _Optional[_Union[Reveal, _Mapping]] = ...) -> None: ...

class QuestionOption(_message.Message):
    __slots__ = ("option_id", "label")
    OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    LABEL_FIELD_NUMBER: _ClassVar[int]
    option_id: str
    label: str
    def __init__(self, option_id: _Optional[str] = ..., label: _Optional[str] = ...) -> None: ...

class Reveal(_message.Message):
    __slots__ = ("correct_option_id", "explanation", "results")
    CORRECT_OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    EXPLANATION_FIELD_NUMBER: _ClassVar[int]
    RESULTS_FIELD_NUMBER: _ClassVar[int]
    correct_option_id: str
    explanation: str
    results: _containers.RepeatedCompositeFieldContainer[PlayerResult]
    def __init__(self, correct_option_id: _Optional[str] = ..., explanation: _Optional[str] = ..., results: _Optional[_Iterable[_Union[PlayerResult, _Mapping]]] = ...) -> None: ...

class PlayerResult(_message.Message):
    __slots__ = ("user_id", "chosen_option_id", "correct", "points")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    CHOSEN_OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    CORRECT_FIELD_NUMBER: _ClassVar[int]
    POINTS_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    chosen_option_id: str
    correct: bool
    points: int
    def __init__(self, user_id: _Optional[str] = ..., chosen_option_id: _Optional[str] = ..., correct: _Optional[bool] = ..., points: _Optional[int] = ...) -> None: ...

class ScoreEntry(_message.Message):
    __slots__ = ("user_id", "display_name", "total_points", "rank", "points_last_round")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    TOTAL_POINTS_FIELD_NUMBER: _ClassVar[int]
    RANK_FIELD_NUMBER: _ClassVar[int]
    POINTS_LAST_ROUND_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    total_points: int
    rank: int
    points_last_round: int
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ..., total_points: _Optional[int] = ..., rank: _Optional[int] = ..., points_last_round: _Optional[int] = ...) -> None: ...

class Vote(_message.Message):
    __slots__ = ("about_user_id", "deadline", "wait_votes", "continue_votes", "end_votes", "voted_user_ids")
    ABOUT_USER_ID_FIELD_NUMBER: _ClassVar[int]
    DEADLINE_FIELD_NUMBER: _ClassVar[int]
    WAIT_VOTES_FIELD_NUMBER: _ClassVar[int]
    CONTINUE_VOTES_FIELD_NUMBER: _ClassVar[int]
    END_VOTES_FIELD_NUMBER: _ClassVar[int]
    VOTED_USER_IDS_FIELD_NUMBER: _ClassVar[int]
    about_user_id: str
    deadline: _timestamp_pb2.Timestamp
    wait_votes: int
    continue_votes: int
    end_votes: int
    voted_user_ids: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, about_user_id: _Optional[str] = ..., deadline: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., wait_votes: _Optional[int] = ..., continue_votes: _Optional[int] = ..., end_votes: _Optional[int] = ..., voted_user_ids: _Optional[_Iterable[str]] = ...) -> None: ...

class RoomEvent(_message.Message):
    __slots__ = ("type", "subject_user_id", "occurred_at", "snapshot")
    TYPE_FIELD_NUMBER: _ClassVar[int]
    SUBJECT_USER_ID_FIELD_NUMBER: _ClassVar[int]
    OCCURRED_AT_FIELD_NUMBER: _ClassVar[int]
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    type: RoomEventType
    subject_user_id: str
    occurred_at: _timestamp_pb2.Timestamp
    snapshot: RoomSnapshot
    def __init__(self, type: _Optional[_Union[RoomEventType, str]] = ..., subject_user_id: _Optional[str] = ..., occurred_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class CreateRoomRequest(_message.Message):
    __slots__ = ("host_user_id", "host_display_name")
    HOST_USER_ID_FIELD_NUMBER: _ClassVar[int]
    HOST_DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    host_user_id: str
    host_display_name: str
    def __init__(self, host_user_id: _Optional[str] = ..., host_display_name: _Optional[str] = ...) -> None: ...

class CreateRoomResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class JoinRoomRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "display_name")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    display_name: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., display_name: _Optional[str] = ...) -> None: ...

class JoinRoomResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class LeaveRoomRequest(_message.Message):
    __slots__ = ("room_code", "user_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ...) -> None: ...

class LeaveRoomResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class KickPlayerRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "target_user_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    TARGET_USER_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    target_user_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., target_user_id: _Optional[str] = ...) -> None: ...

class KickPlayerResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class UpdateSettingsRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "settings")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    SETTINGS_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    settings: RoomSettings
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., settings: _Optional[_Union[RoomSettings, _Mapping]] = ...) -> None: ...

class UpdateSettingsResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class StartQuizRequest(_message.Message):
    __slots__ = ("room_code", "user_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ...) -> None: ...

class StartQuizResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class SubmitAnswerRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "question_id", "option_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    QUESTION_ID_FIELD_NUMBER: _ClassVar[int]
    OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    question_id: str
    option_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., question_id: _Optional[str] = ..., option_id: _Optional[str] = ...) -> None: ...

class SubmitAnswerResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class ReportDisconnectedRequest(_message.Message):
    __slots__ = ("room_code", "user_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ...) -> None: ...

class ReportDisconnectedResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class ReportConnectionQualityRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "quality")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    QUALITY_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    quality: ConnectionQuality
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., quality: _Optional[_Union[ConnectionQuality, str]] = ...) -> None: ...

class ReportConnectionQualityResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class CastVoteRequest(_message.Message):
    __slots__ = ("room_code", "user_id", "option")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    OPTION_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    option: VoteOption
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ..., option: _Optional[_Union[VoteOption, str]] = ...) -> None: ...

class CastVoteResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class GetRoomSnapshotRequest(_message.Message):
    __slots__ = ("room_code", "user_id")
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    user_id: str
    def __init__(self, room_code: _Optional[str] = ..., user_id: _Optional[str] = ...) -> None: ...

class GetRoomSnapshotResponse(_message.Message):
    __slots__ = ("snapshot",)
    SNAPSHOT_FIELD_NUMBER: _ClassVar[int]
    snapshot: RoomSnapshot
    def __init__(self, snapshot: _Optional[_Union[RoomSnapshot, _Mapping]] = ...) -> None: ...

class StreamRoomEventsRequest(_message.Message):
    __slots__ = ("room_code",)
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    room_code: str
    def __init__(self, room_code: _Optional[str] = ...) -> None: ...

class StreamRoomEventsResponse(_message.Message):
    __slots__ = ("event",)
    EVENT_FIELD_NUMBER: _ClassVar[int]
    event: RoomEvent
    def __init__(self, event: _Optional[_Union[RoomEvent, _Mapping]] = ...) -> None: ...
