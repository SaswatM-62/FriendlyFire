"""Configuration, from environment variables only (see .env.example)."""

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(frozen=True)

    grpc_port: int = 9094
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "friendlyfire"
    postgres_user: str = "postgres"
    postgres_password: SecretStr = SecretStr("")
