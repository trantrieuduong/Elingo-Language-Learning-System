import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.classifier import ViSoBertClassifier
from app.config import get_settings
from app.routes import router


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    logging.basicConfig(level=settings.log_level, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
    classifier = ViSoBertClassifier(settings)
    classifier.load()
    app.state.classifier = classifier
    yield


def create_app() -> FastAPI:
    # Service nội bộ: tắt docs/openapi
    app = FastAPI(title="Elingo Moderation Service", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)
    app.include_router(router)
    return app


app = create_app()