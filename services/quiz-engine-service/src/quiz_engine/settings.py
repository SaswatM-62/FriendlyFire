"""Configuration, from environment variables only (see .env.example)."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(frozen=True)

    grpc_port: int = 9093
    matchdata_grpc_addr: str = "localhost:9095"
