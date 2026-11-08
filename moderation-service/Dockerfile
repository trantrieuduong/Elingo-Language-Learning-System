FROM python:3.11-slim

ARG HF_MODEL=nd-khoa/vihsd-uit-visobert-v2

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HOME=/opt/hf-cache \
    MODERATION_HF_MODEL_NAME=${HF_MODEL} \
    OMP_NUM_THREADS=2 \
    MKL_NUM_THREADS=2 \
    TOKENIZERS_PARALLELISM=false

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

# Tải model lúc build: container khởi động nhanh và không cần mạng khi chạy
RUN python -c "from transformers import AutoTokenizer, AutoModelForSequenceClassification as M; AutoTokenizer.from_pretrained('${HF_MODEL}'); M.from_pretrained('${HF_MODEL}')"
ENV HF_HUB_OFFLINE=1

RUN addgroup --system appgroup && adduser --system --ingroup appgroup --no-create-home appuser
COPY app ./app
USER appuser

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=4).status == 200 else 1)"

# --workers 1: chỉ 1 bản model trong RAM
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]