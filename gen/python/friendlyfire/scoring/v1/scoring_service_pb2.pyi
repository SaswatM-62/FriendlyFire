import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class EndReason(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    END_REASON_UNSPECIFIED: _ClassVar[EndReason]
    END_REASON_COMPLETED: _ClassVar[EndReason]
    END_REASON_VOTED_TO_END: _ClassVar[EndReason]
    END_REASON_NOT_ENOUGH_PLAYERS: _ClassVar[EndReason]
END_REASON_UNSPECIFIED: EndReason
END_REASON_COMPLETED: EndReason
END_REASON_VOTED_TO_END: EndReason
END_REASON_NOT_ENOUGH_PLAYERS: EndReason

class StartQuizRequest(_message.Message):
    __slots__ = ("quiz_id", "room_code", "players", "total_rounds", "seconds_per_question")
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    ROOM_CODE_FIELD_NUMBER: _ClassVar[int]
    PLAYERS_FIELD_NUMBER: _ClassVar[int]
    TOTAL_ROUNDS_FIELD_NUMBER: _ClassVar[int]
    SECONDS_PER_QUESTION_FIELD_NUMBER: _ClassVar[int]
    quiz_id: str
    room_code: str
    players: _containers.RepeatedCompositeFieldContainer[QuizParticipant]
    total_rounds: int
    seconds_per_question: int
    def __init__(self, quiz_id: _Optional[str] = ..., room_code: _Optional[str] = ..., players: _Optional[_Iterable[_Union[QuizParticipant, _Mapping]]] = ..., total_rounds: _Optional[int] = ..., seconds_per_question: _Optional[int] = ...) -> None: ...

class QuizParticipant(_message.Message):
    __slots__ = ("user_id", "display_name")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ...) -> None: ...

class StartQuizResponse(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class RecordRoundRequest(_message.Message):
    __slots__ = ("quiz_id", "round_number", "answers")
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    ROUND_NUMBER_FIELD_NUMBER: _ClassVar[int]
    ANSWERS_FIELD_NUMBER: _ClassVar[int]
    quiz_id: str
    round_number: int
    answers: _containers.RepeatedCompositeFieldContainer[PlayerAnswer]
    def __init__(self, quiz_id: _Optional[str] = ..., round_number: _Optional[int] = ..., answers: _Optional[_Iterable[_Union[PlayerAnswer, _Mapping]]] = ...) -> None: ...

class PlayerAnswer(_message.Message):
    __slots__ = ("user_id", "answered", "correct", "response_time_ms")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    ANSWERED_FIELD_NUMBER: _ClassVar[int]
    CORRECT_FIELD_NUMBER: _ClassVar[int]
    RESPONSE_TIME_MS_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    answered: bool
    correct: bool
    response_time_ms: int
    def __init__(self, user_id: _Optional[str] = ..., answered: _Optional[bool] = ..., correct: _Optional[bool] = ..., response_time_ms: _Optional[int] = ...) -> None: ...

class RecordRoundResponse(_message.Message):
    __slots__ = ("round_points", "scoreboard")
    ROUND_POINTS_FIELD_NUMBER: _ClassVar[int]
    SCOREBOARD_FIELD_NUMBER: _ClassVar[int]
    round_points: _containers.RepeatedCompositeFieldContainer[RoundPoints]
    scoreboard: _containers.RepeatedCompositeFieldContainer[ScoreboardEntry]
    def __init__(self, round_points: _Optional[_Iterable[_Union[RoundPoints, _Mapping]]] = ..., scoreboard: _Optional[_Iterable[_Union[ScoreboardEntry, _Mapping]]] = ...) -> None: ...

class RoundPoints(_message.Message):
    __slots__ = ("user_id", "points")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    POINTS_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    points: int
    def __init__(self, user_id: _Optional[str] = ..., points: _Optional[int] = ...) -> None: ...

class ScoreboardEntry(_message.Message):
    __slots__ = ("user_id", "display_name", "total_points", "correct_answers", "rank")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    TOTAL_POINTS_FIELD_NUMBER: _ClassVar[int]
    CORRECT_ANSWERS_FIELD_NUMBER: _ClassVar[int]
    RANK_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    total_points: int
    correct_answers: int
    rank: int
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ..., total_points: _Optional[int] = ..., correct_answers: _Optional[int] = ..., rank: _Optional[int] = ...) -> None: ...

class FinalizeQuizRequest(_message.Message):
    __slots__ = ("quiz_id", "rounds_played", "end_reason")
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    ROUNDS_PLAYED_FIELD_NUMBER: _ClassVar[int]
    END_REASON_FIELD_NUMBER: _ClassVar[int]
    quiz_id: str
    rounds_played: int
    end_reason: EndReason
    def __init__(self, quiz_id: _Optional[str] = ..., rounds_played: _Optional[int] = ..., end_reason: _Optional[_Union[EndReason, str]] = ...) -> None: ...

class FinalizeQuizResponse(_message.Message):
    __slots__ = ("summary",)
    SUMMARY_FIELD_NUMBER: _ClassVar[int]
    summary: QuizSummary
    def __init__(self, summary: _Optional[_Union[QuizSummary, _Mapping]] = ...) -> None: ...

class QuizSummary(_message.Message):
    __slots__ = ("quiz_id", "started_at", "finished_at", "total_rounds", "rounds_played", "end_reason", "final_scoreboard")
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    STARTED_AT_FIELD_NUMBER: _ClassVar[int]
    FINISHED_AT_FIELD_NUMBER: _ClassVar[int]
    TOTAL_ROUNDS_FIELD_NUMBER: _ClassVar[int]
    ROUNDS_PLAYED_FIELD_NUMBER: _ClassVar[int]
    END_REASON_FIELD_NUMBER: _ClassVar[int]
    FINAL_SCOREBOARD_FIELD_NUMBER: _ClassVar[int]
    quiz_id: str
    started_at: _timestamp_pb2.Timestamp
    finished_at: _timestamp_pb2.Timestamp
    total_rounds: int
    rounds_played: int
    end_reason: EndReason
    final_scoreboard: _containers.RepeatedCompositeFieldContainer[ScoreboardEntry]
    def __init__(self, quiz_id: _Optional[str] = ..., started_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., finished_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., total_rounds: _Optional[int] = ..., rounds_played: _Optional[int] = ..., end_reason: _Optional[_Union[EndReason, str]] = ..., final_scoreboard: _Optional[_Iterable[_Union[ScoreboardEntry, _Mapping]]] = ...) -> None: ...

class GetQuizResultRequest(_message.Message):
    __slots__ = ("quiz_id",)
    QUIZ_ID_FIELD_NUMBER: _ClassVar[int]
    quiz_id: str
    def __init__(self, quiz_id: _Optional[str] = ...) -> None: ...

class GetQuizResultResponse(_message.Message):
    __slots__ = ("summary",)
    SUMMARY_FIELD_NUMBER: _ClassVar[int]
    summary: QuizSummary
    def __init__(self, summary: _Optional[_Union[QuizSummary, _Mapping]] = ...) -> None: ...

class GetPlayerHistoryRequest(_message.Message):
    __slots__ = ("user_id", "page_size", "page_token")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    PAGE_SIZE_FIELD_NUMBER: _ClassVar[int]
    PAGE_TOKEN_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    page_size: int
    page_token: str
    def __init__(self, user_id: _Optional[str] = ..., page_size: _Optional[int] = ..., page_token: _Optional[str] = ...) -> None: ...

class GetPlayerHistoryResponse(_message.Message):
    __slots__ = ("quizzes", "next_page_token")
    QUIZZES_FIELD_NUMBER: _ClassVar[int]
    NEXT_PAGE_TOKEN_FIELD_NUMBER: _ClassVar[int]
    quizzes: _containers.RepeatedCompositeFieldContainer[QuizSummary]
    next_page_token: str
    def __init__(self, quizzes: _Optional[_Iterable[_Union[QuizSummary, _Mapping]]] = ..., next_page_token: _Optional[str] = ...) -> None: ...
