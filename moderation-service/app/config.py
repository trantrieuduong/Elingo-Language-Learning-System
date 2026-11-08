from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Cấu hình đọc từ biến môi trường MODERATION_* (hoặc file .env khi chạy local)."""

    model_config = SettingsConfigDict(env_prefix="MODERATION_", env_file=".env", extra="ignore")

    hf_model_name: str = "nd-khoa/vihsd-uit-visobert-v2"
    hf_revision: str | None = None      # nên pin commit hash khi lên production

    max_length: int = 128               # theo model card
    stride: int = 32                    # phần gối đầu giữa các đoạn khi văn bản dài
    max_chunks: int = 16                # trần số đoạn/ request, chặn tốn CPU
    batch_size: int = 8

    torch_threads: int = 2              # chừa CPU cho JVM / ffmpeg / MySQL
    quantize: bool = False              # int8 dynamic quantization: nhanh/ nhẹ hơn, đổi lại có thể giảm độ chính xác

    internal_token: str = ""            # rỗng = không kiểm tra token
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    return Settings()