from typing import Literal

from pydantic import BaseModel, Field

Label = Literal["CLEAN", "OFFENSIVE", "HATE"]


class TextRequest(BaseModel):
    # Phía Java nên cắt text trước khi gửi (title + content), vượt giới hạn sẽ nhận 422.
    text: str = Field(min_length=1, max_length=20_000)


class TextResponse(BaseModel):
    label: Label
    score: float                 # xác suất của label, dùng làm ai_score (DECIMAL(5,4))
    scores: dict[str, float]     # xác suất từng nhãn
    chunks: int                  # số đoạn đã chạy qua model