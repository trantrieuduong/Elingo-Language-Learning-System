from fastapi.testclient import TestClient

from app.main import create_app
from app.routes import get_classifier
from app.schemas import TextResponse


class FakeClassifier:
    ready = True

    def classify(self, text: str) -> TextResponse:
        return TextResponse(label="CLEAN", score=0.99,
                            scores={"CLEAN": 0.99, "OFFENSIVE": 0.005, "HATE": 0.005}, chunks=1)


def make_client() -> TestClient:
    app = create_app()
    app.dependency_overrides[get_classifier] = lambda: FakeClassifier()
    return TestClient(app)  # không dùng `with` => không chạy lifespan (không nạp model thật)


def test_health():
    assert make_client().get("/health").status_code == 200


def test_moderate_text():
    res = make_client().post("/v1/moderate/text", json={"text": "xin chào"})
    assert res.status_code == 200
    assert res.json()["label"] == "CLEAN"


def test_rejects_empty_text():
    assert make_client().post("/v1/moderate/text", json={"text": ""}).status_code == 422