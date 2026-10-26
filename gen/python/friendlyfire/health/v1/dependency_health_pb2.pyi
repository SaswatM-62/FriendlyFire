from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ComponentStatus(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    COMPONENT_STATUS_UNSPECIFIED: _ClassVar[ComponentStatus]
    COMPONENT_STATUS_UP: _ClassVar[ComponentStatus]
    COMPONENT_STATUS_DEGRADED: _ClassVar[ComponentStatus]
    COMPONENT_STATUS_DOWN: _ClassVar[ComponentStatus]
COMPONENT_STATUS_UNSPECIFIED: ComponentStatus
COMPONENT_STATUS_UP: ComponentStatus
COMPONENT_STATUS_DEGRADED: ComponentStatus
COMPONENT_STATUS_DOWN: ComponentStatus

class ComponentHealth(_message.Message):
    __slots__ = ("name", "status", "latency_ms", "error", "dependencies")
    NAME_FIELD_NUMBER: _ClassVar[int]
    STATUS_FIELD_NUMBER: _ClassVar[int]
    LATENCY_MS_FIELD_NUMBER: _ClassVar[int]
    ERROR_FIELD_NUMBER: _ClassVar[int]
    DEPENDENCIES_FIELD_NUMBER: _ClassVar[int]
    name: str
    status: ComponentStatus
    latency_ms: int
    error: str
    dependencies: _containers.RepeatedCompositeFieldContainer[ComponentHealth]
    def __init__(self, name: _Optional[str] = ..., status: _Optional[_Union[ComponentStatus, str]] = ..., latency_ms: _Optional[int] = ..., error: _Optional[str] = ..., dependencies: _Optional[_Iterable[_Union[ComponentHealth, _Mapping]]] = ...) -> None: ...

class CheckDependenciesRequest(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class CheckDependenciesResponse(_message.Message):
    __slots__ = ("component",)
    COMPONENT_FIELD_NUMBER: _ClassVar[int]
    component: ComponentHealth
    def __init__(self, component: _Optional[_Union[ComponentHealth, _Mapping]] = ...) -> None: ...
