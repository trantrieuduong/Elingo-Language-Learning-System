import secrets

from fastapi import APIRouter, Depends, Header, HTTPException, Request

from app.classifier import ViSoBertClassifier
from app.config import get_settings
from app.schemas import TextRequest, TextResponse

router = APIRouter()


def get_classifier(request: Request) -> ViSoBertClassifier:
    return request.app.state.classifier


def verify_token(x_internal_token: str | None = Header(default=None)) -> None:
    expected = get_settings().internal_token
    if expected and not secrets.compare_digest(x_internal_token or "", expected):
        raise HTTPException(status_code=401, detail="Unauthorized")


@router.get("/health")
def health(classifier: ViSoBertClassifier = Depends(get_classifier)) -> dict:
    if not classifier.ready:
        raise HTTPException(status_code=503, detail="Model not ready")
    return {"status": "ok"}


# `def` (không phải async): suy luận là CPU-bound, để FastAPI chạy trong threadpool, không chặn event loop.
@router.post("/v1/moderate/text", response_model=TextResponse, dependencies=[Depends(verify_token)])
def moderate_text(body: TextRequest, classifier: ViSoBertClassifier = Depends(get_classifier)) -> TextResponse:
    return classifier.classify(body.text)