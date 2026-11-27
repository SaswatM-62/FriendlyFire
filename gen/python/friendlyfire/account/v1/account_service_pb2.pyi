import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class DataStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DATA_STATUS_UNSPECIFIED: _ClassVar[DataStatus]
    DATA_STATUS_SYNCED: _ClassVar[DataStatus]
    DATA_STATUS_SYNCING: _ClassVar[DataStatus]
    DATA_STATUS_NOT_SYNCED: _ClassVar[DataStatus]
    DATA_STATUS_NOT_LINKED: _ClassVar[DataStatus]
    DATA_STATUS_FAILED: _ClassVar[DataStatus]
DATA_STATUS_UNSPECIFIED: DataStatus
DATA_STATUS_SYNCED: DataStatus
DATA_STATUS_SYNCING: DataStatus
DATA_STATUS_NOT_SYNCED: DataStatus
DATA_STATUS_NOT_LINKED: DataStatus
DATA_STATUS_FAILED: DataStatus

class User(_message.Message):
    __slots__ = ("user_id", "display_name", "email", "created_at", "riot_link", "onboarded_at", "look")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    EMAIL_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_FIELD_NUMBER: _ClassVar[int]
    RIOT_LINK_FIELD_NUMBER: _ClassVar[int]
    ONBOARDED_AT_FIELD_NUMBER: _ClassVar[int]
    LOOK_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    email: str
    created_at: _timestamp_pb2.Timestamp
    riot_link: RiotLink
    onboarded_at: _timestamp_pb2.Timestamp
    look: Look
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ..., email: _Optional[str] = ..., created_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., riot_link: _Optional[_Union[RiotLink, _Mapping]] = ..., onboarded_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., look: _Optional[_Union[Look, _Mapping]] = ...) -> None: ...

class Look(_message.Message):
    __slots__ = ("icon", "colour", "expression")
    ICON_FIELD_NUMBER: _ClassVar[int]
    COLOUR_FIELD_NUMBER: _ClassVar[int]
    EXPRESSION_FIELD_NUMBER: _ClassVar[int]
    icon: str
    colour: str
    expression: str
    def __init__(self, icon: _Optional[str] = ..., colour: _Optional[str] = ..., expression: _Optional[str] = ...) -> None: ...

class RiotLink(_message.Message):
    __slots__ = ("game_name", "tag_line", "region", "linked_at")
    GAME_NAME_FIELD_NUMBER: _ClassVar[int]
    TAG_LINE_FIELD_NUMBER: _ClassVar[int]
    REGION_FIELD_NUMBER: _ClassVar[int]
    LINKED_AT_FIELD_NUMBER: _ClassVar[int]
    game_name: str
    tag_line: str
    region: str
    linked_at: _timestamp_pb2.Timestamp
    def __init__(self, game_name: _Optional[str] = ..., tag_line: _Optional[str] = ..., region: _Optional[str] = ..., linked_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class EnsureUserRequest(_message.Message):
    __slots__ = ("auth_subject", "email", "suggested_display_name")
    AUTH_SUBJECT_FIELD_NUMBER: _ClassVar[int]
    EMAIL_FIELD_NUMBER: _ClassVar[int]
    SUGGESTED_DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    auth_subject: str
    email: str
    suggested_display_name: str
    def __init__(self, auth_subject: _Optional[str] = ..., email: _Optional[str] = ..., suggested_display_name: _Optional[str] = ...) -> None: ...

class EnsureUserResponse(_message.Message):
    __slots__ = ("user", "created")
    USER_FIELD_NUMBER: _ClassVar[int]
    CREATED_FIELD_NUMBER: _ClassVar[int]
    user: User
    created: bool
    def __init__(self, user: _Optional[_Union[User, _Mapping]] = ..., created: _Optional[bool] = ...) -> None: ...

class GetUserRequest(_message.Message):
    __slots__ = ("user_id",)
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    def __init__(self, user_id: _Optional[str] = ...) -> None: ...

class GetUserResponse(_message.Message):
    __slots__ = ("user",)
    USER_FIELD_NUMBER: _ClassVar[int]
    user: User
    def __init__(self, user: _Optional[_Union[User, _Mapping]] = ...) -> None: ...

class UpdateDisplayNameRequest(_message.Message):
    __slots__ = ("user_id", "display_name")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ...) -> None: ...

class UpdateDisplayNameResponse(_message.Message):
    __slots__ = ("user",)
    USER_FIELD_NUMBER: _ClassVar[int]
    user: User
    def __init__(self, user: _Optional[_Union[User, _Mapping]] = ...) -> None: ...

class StartRiotLinkRequest(_message.Message):
    __slots__ = ("user_id", "redirect_uri")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    REDIRECT_URI_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    redirect_uri: str
    def __init__(self, user_id: _Optional[str] = ..., redirect_uri: _Optional[str] = ...) -> None: ...

class StartRiotLinkResponse(_message.Message):
    __slots__ = ("authorize_url",)
    AUTHORIZE_URL_FIELD_NUMBER: _ClassVar[int]
    authorize_url: str
    def __init__(self, authorize_url: _Optional[str] = ...) -> None: ...

class CompleteRiotLinkRequest(_message.Message):
    __slots__ = ("user_id", "code", "state", "redirect_uri")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    CODE_FIELD_NUMBER: _ClassVar[int]
    STATE_FIELD_NUMBER: _ClassVar[int]
    REDIRECT_URI_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    code: str
    state: str
    redirect_uri: str
    def __init__(self, user_id: _Optional[str] = ..., code: _Optional[str] = ..., state: _Optional[str] = ..., redirect_uri: _Optional[str] = ...) -> None: ...

class CompleteRiotLinkResponse(_message.Message):
    __slots__ = ("riot_link",)
    RIOT_LINK_FIELD_NUMBER: _ClassVar[int]
    riot_link: RiotLink
    def __init__(self, riot_link: _Optional[_Union[RiotLink, _Mapping]] = ...) -> None: ...

class UnlinkRiotRequest(_message.Message):
    __slots__ = ("user_id",)
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    def __init__(self, user_id: _Optional[str] = ...) -> None: ...

class UnlinkRiotResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class DeleteAccountRequest(_message.Message):
    __slots__ = ("user_id",)
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    def __init__(self, user_id: _Optional[str] = ...) -> None: ...

class DeleteAccountResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class GetDataStatusRequest(_message.Message):
    __slots__ = ("user_ids",)
    USER_IDS_FIELD_NUMBER: _ClassVar[int]
    user_ids: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, user_ids: _Optional[_Iterable[str]] = ...) -> None: ...

class GetDataStatusResponse(_message.Message):
    __slots__ = ("statuses",)
    STATUSES_FIELD_NUMBER: _ClassVar[int]
    statuses: _containers.RepeatedCompositeFieldContainer[UserDataStatus]
    def __init__(self, statuses: _Optional[_Iterable[_Union[UserDataStatus, _Mapping]]] = ...) -> None: ...

class UserDataStatus(_message.Message):
    __slots__ = ("user_id", "status", "estimated_seconds", "matches_stored", "last_synced_at", "error_message")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    ESTIMATED_SECONDS_FIELD_NUMBER: _ClassVar[int]
    MATCHES_STORED_FIELD_NUMBER: _ClassVar[int]
    LAST_SYNCED_AT_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    status: DataStatus
    estimated_seconds: int
    matches_stored: int
    last_synced_at: _timestamp_pb2.Timestamp
    error_message: str
    def __init__(self, user_id: _Optional[str] = ..., status: _Optional[_Union[DataStatus, str]] = ..., estimated_seconds: _Optional[int] = ..., matches_stored: _Optional[int] = ..., last_synced_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., error_message: _Optional[str] = ...) -> None: ...

class FinishOnboardingRequest(_message.Message):
    __slots__ = ("user_id",)
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    def __init__(self, user_id: _Optional[str] = ...) -> None: ...

class FinishOnboardingResponse(_message.Message):
    __slots__ = ("user",)
    USER_FIELD_NUMBER: _ClassVar[int]
    user: User
    def __init__(self, user: _Optional[_Union[User, _Mapping]] = ...) -> None: ...

class UpdateLookRequest(_message.Message):
    __slots__ = ("user_id", "look")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    LOOK_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    look: Look
    def __init__(self, user_id: _Optional[str] = ..., look: _Optional[_Union[Look, _Mapping]] = ...) -> None: ...

class UpdateLookResponse(_message.Message):
    __slots__ = ("user",)
    USER_FIELD_NUMBER: _ClassVar[int]
    user: User
    def __init__(self, user: _Optional[_Union[User, _Mapping]] = ...) -> None: ...
