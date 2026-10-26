import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class User(_message.Message):
    __slots__ = ("user_id", "display_name", "email", "created_at", "riot_link")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    EMAIL_FIELD_NUMBER: _ClassVar[int]
    CREATED_AT_FIELD_NUMBER: _ClassVar[int]
    RIOT_LINK_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    email: str
    created_at: _timestamp_pb2.Timestamp
    riot_link: RiotLink
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ..., email: _Optional[str] = ..., created_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., riot_link: _Optional[_Union[RiotLink, _Mapping]] = ...) -> None: ...

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
