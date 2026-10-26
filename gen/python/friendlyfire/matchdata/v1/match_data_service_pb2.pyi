import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class SyncStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    SYNC_STATUS_UNSPECIFIED: _ClassVar[SyncStatus]
    SYNC_STATUS_NOT_LINKED: _ClassVar[SyncStatus]
    SYNC_STATUS_IN_PROGRESS: _ClassVar[SyncStatus]
    SYNC_STATUS_COMPLETE: _ClassVar[SyncStatus]
    SYNC_STATUS_FAILED: _ClassVar[SyncStatus]
SYNC_STATUS_UNSPECIFIED: SyncStatus
SYNC_STATUS_NOT_LINKED: SyncStatus
SYNC_STATUS_IN_PROGRESS: SyncStatus
SYNC_STATUS_COMPLETE: SyncStatus
SYNC_STATUS_FAILED: SyncStatus

class SyncPlayerRequest(_message.Message):
    __slots__ = ("user_id", "game", "external_player_id", "region")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    GAME_FIELD_NUMBER: _ClassVar[int]
    EXTERNAL_PLAYER_ID_FIELD_NUMBER: _ClassVar[int]
    REGION_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    game: str
    external_player_id: str
    region: str
    def __init__(self, user_id: _Optional[str] = ..., game: _Optional[str] = ..., external_player_id: _Optional[str] = ..., region: _Optional[str] = ...) -> None: ...

class SyncPlayerResponse(_message.Message):
    __slots__ = ("status",)
    STATUS_FIELD_NUMBER: _ClassVar[int]
    status: SyncStatus
    def __init__(self, status: _Optional[_Union[SyncStatus, str]] = ...) -> None: ...

class GetSyncStatusRequest(_message.Message):
    __slots__ = ("user_ids",)
    USER_IDS_FIELD_NUMBER: _ClassVar[int]
    user_ids: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, user_ids: _Optional[_Iterable[str]] = ...) -> None: ...

class GetSyncStatusResponse(_message.Message):
    __slots__ = ("statuses",)
    STATUSES_FIELD_NUMBER: _ClassVar[int]
    statuses: _containers.RepeatedCompositeFieldContainer[PlayerSyncStatus]
    def __init__(self, statuses: _Optional[_Iterable[_Union[PlayerSyncStatus, _Mapping]]] = ...) -> None: ...

class PlayerSyncStatus(_message.Message):
    __slots__ = ("user_id", "status", "matches_stored", "last_synced_at", "error_message")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    MATCHES_STORED_FIELD_NUMBER: _ClassVar[int]
    LAST_SYNCED_AT_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    status: SyncStatus
    matches_stored: int
    last_synced_at: _timestamp_pb2.Timestamp
    error_message: str
    def __init__(self, user_id: _Optional[str] = ..., status: _Optional[_Union[SyncStatus, str]] = ..., matches_stored: _Optional[int] = ..., last_synced_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., error_message: _Optional[str] = ...) -> None: ...

class GetPlayerMatchStatsRequest(_message.Message):
    __slots__ = ("user_ids", "since", "max_matches_per_player")
    USER_IDS_FIELD_NUMBER: _ClassVar[int]
    SINCE_FIELD_NUMBER: _ClassVar[int]
    MAX_MATCHES_PER_PLAYER_FIELD_NUMBER: _ClassVar[int]
    user_ids: _containers.RepeatedScalarFieldContainer[str]
    since: _timestamp_pb2.Timestamp
    max_matches_per_player: int
    def __init__(self, user_ids: _Optional[_Iterable[str]] = ..., since: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., max_matches_per_player: _Optional[int] = ...) -> None: ...

class GetPlayerMatchStatsResponse(_message.Message):
    __slots__ = ("stats",)
    STATS_FIELD_NUMBER: _ClassVar[int]
    stats: _containers.RepeatedCompositeFieldContainer[PlayerMatchStats]
    def __init__(self, stats: _Optional[_Iterable[_Union[PlayerMatchStats, _Mapping]]] = ...) -> None: ...

class PlayerMatchStats(_message.Message):
    __slots__ = ("match_id", "user_id", "game", "played_at", "won", "kills", "deaths", "assists", "labels", "numbers")
    class LabelsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    class NumbersEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: float
        def __init__(self, key: _Optional[str] = ..., value: _Optional[float] = ...) -> None: ...
    MATCH_ID_FIELD_NUMBER: _ClassVar[int]
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    GAME_FIELD_NUMBER: _ClassVar[int]
    PLAYED_AT_FIELD_NUMBER: _ClassVar[int]
    WON_FIELD_NUMBER: _ClassVar[int]
    KILLS_FIELD_NUMBER: _ClassVar[int]
    DEATHS_FIELD_NUMBER: _ClassVar[int]
    ASSISTS_FIELD_NUMBER: _ClassVar[int]
    LABELS_FIELD_NUMBER: _ClassVar[int]
    NUMBERS_FIELD_NUMBER: _ClassVar[int]
    match_id: str
    user_id: str
    game: str
    played_at: _timestamp_pb2.Timestamp
    won: bool
    kills: int
    deaths: int
    assists: int
    labels: _containers.ScalarMap[str, str]
    numbers: _containers.ScalarMap[str, float]
    def __init__(self, match_id: _Optional[str] = ..., user_id: _Optional[str] = ..., game: _Optional[str] = ..., played_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., won: _Optional[bool] = ..., kills: _Optional[int] = ..., deaths: _Optional[int] = ..., assists: _Optional[int] = ..., labels: _Optional[_Mapping[str, str]] = ..., numbers: _Optional[_Mapping[str, float]] = ...) -> None: ...

class DeletePlayerDataRequest(_message.Message):
    __slots__ = ("user_id",)
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    def __init__(self, user_id: _Optional[str] = ...) -> None: ...

class DeletePlayerDataResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...
