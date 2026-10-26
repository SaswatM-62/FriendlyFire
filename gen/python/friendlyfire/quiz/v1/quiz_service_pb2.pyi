from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class QuestionType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    QUESTION_TYPE_UNSPECIFIED: _ClassVar[QuestionType]
    QUESTION_TYPE_PICK_THE_PLAYER: _ClassVar[QuestionType]
QUESTION_TYPE_UNSPECIFIED: QuestionType
QUESTION_TYPE_PICK_THE_PLAYER: QuestionType

class GenerateQuizRequest(_message.Message):
    __slots__ = ("players", "question_count", "seed")
    PLAYERS_FIELD_NUMBER: _ClassVar[int]
    QUESTION_COUNT_FIELD_NUMBER: _ClassVar[int]
    SEED_FIELD_NUMBER: _ClassVar[int]
    players: _containers.RepeatedCompositeFieldContainer[QuizPlayer]
    question_count: int
    seed: int
    def __init__(self, players: _Optional[_Iterable[_Union[QuizPlayer, _Mapping]]] = ..., question_count: _Optional[int] = ..., seed: _Optional[int] = ...) -> None: ...

class QuizPlayer(_message.Message):
    __slots__ = ("user_id", "display_name")
    USER_ID_FIELD_NUMBER: _ClassVar[int]
    DISPLAY_NAME_FIELD_NUMBER: _ClassVar[int]
    user_id: str
    display_name: str
    def __init__(self, user_id: _Optional[str] = ..., display_name: _Optional[str] = ...) -> None: ...

class GenerateQuizResponse(_message.Message):
    __slots__ = ("questions",)
    QUESTIONS_FIELD_NUMBER: _ClassVar[int]
    questions: _containers.RepeatedCompositeFieldContainer[Question]
    def __init__(self, questions: _Optional[_Iterable[_Union[Question, _Mapping]]] = ...) -> None: ...

class Question(_message.Message):
    __slots__ = ("question_id", "type", "text", "options", "correct_option_id", "explanation", "source_match_id")
    QUESTION_ID_FIELD_NUMBER: _ClassVar[int]
    TYPE_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    OPTIONS_FIELD_NUMBER: _ClassVar[int]
    CORRECT_OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    EXPLANATION_FIELD_NUMBER: _ClassVar[int]
    SOURCE_MATCH_ID_FIELD_NUMBER: _ClassVar[int]
    question_id: str
    type: QuestionType
    text: str
    options: _containers.RepeatedCompositeFieldContainer[AnswerOption]
    correct_option_id: str
    explanation: str
    source_match_id: str
    def __init__(self, question_id: _Optional[str] = ..., type: _Optional[_Union[QuestionType, str]] = ..., text: _Optional[str] = ..., options: _Optional[_Iterable[_Union[AnswerOption, _Mapping]]] = ..., correct_option_id: _Optional[str] = ..., explanation: _Optional[str] = ..., source_match_id: _Optional[str] = ...) -> None: ...

class AnswerOption(_message.Message):
    __slots__ = ("option_id", "label")
    OPTION_ID_FIELD_NUMBER: _ClassVar[int]
    LABEL_FIELD_NUMBER: _ClassVar[int]
    option_id: str
    label: str
    def __init__(self, option_id: _Optional[str] = ..., label: _Optional[str] = ...) -> None: ...
