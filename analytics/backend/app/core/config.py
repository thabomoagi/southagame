from typing import Literal, Optional
from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=(".env", "../../backend/.env"),
        env_ignore_empty=True,
        extra="ignore",
    )

    DATABASE_URL: Optional[str] = Field(default=None, validation_alias="DATABASE_URL")
    SPRING_DATASOURCE_URL: Optional[str] = Field(default=None, validation_alias="SPRING_DATASOURCE_URL")
    SPRING_DATASOURCE_USERNAME: Optional[str] = Field(default=None, validation_alias="SPRING_DATASOURCE_USERNAME")
    SPRING_DATASOURCE_PASSWORD: Optional[str] = Field(default=None, validation_alias="SPRING_DATASOURCE_PASSWORD")

    SECRET_KEY: str = Field(default="dev-secret-key-change-me", validation_alias="SECRET_KEY")
    ENVIRONMENT: Literal["local", "staging", "production"] = "local"

    @model_validator(mode="after")
    def assemble_db_url(self) -> "Settings":
        if not self.DATABASE_URL:
            if self.SPRING_DATASOURCE_URL:
                url = self.SPRING_DATASOURCE_URL
                if url.startswith("jdbc:postgresql://"):
                    url = url.replace("jdbc:postgresql://", "postgresql://", 1)
                
                if self.SPRING_DATASOURCE_USERNAME and self.SPRING_DATASOURCE_PASSWORD:
                    if "@" in url:
                        parts = url.split("://")
                        if len(parts) > 1:
                            sub_parts = parts[1].split("@")
                            host_db = sub_parts[-1]
                            url = f"{parts[0]}://{self.SPRING_DATASOURCE_USERNAME}:{self.SPRING_DATASOURCE_PASSWORD}@{host_db}"
                self.DATABASE_URL = url
        if not self.DATABASE_URL:
            self.DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/southadb"
            
        if self.DATABASE_URL.startswith("postgresql://") and not self.DATABASE_URL.startswith("postgresql+asyncpg://"):
            self.DATABASE_URL = self.DATABASE_URL.replace("postgresql://", "postgresql+asyncpg://", 1)

        # Remove query parameters like ?sslmode=... which asyncpg doesn't support directly in URL
        if "?" in self.DATABASE_URL:
            self.DATABASE_URL = self.DATABASE_URL.split("?")[0]

        return self


settings = Settings()
