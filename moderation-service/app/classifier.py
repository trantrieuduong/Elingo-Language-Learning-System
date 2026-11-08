import logging
import threading

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

from app.config import Settings
from app.schemas import TextResponse

log = logging.getLogger("VISOBERT-CLASSIFIER")

# Thứ tự nhãn theo model card: 0 = clean, 1 = offensive, 2 = hate
# 0: clean (Bình luận không chứa ngôn từ thù ghét hoặc xúc phạm)
# 1: offensive (Bình luận có tính xúc phạm hoặc gây khó chịu, nhưng không mang tính thù ghét rõ ràng)
# 2: hate (Bình luận chứa ngôn từ thù ghét, thường nhắm vào cá nhân hoặc nhóm cụ thể)
# ref: https://huggingface.co/nd-khoa/vihsd-uit-visobert-v2
LABELS = ("CLEAN", "OFFENSIVE", "HATE")


class ViSoBertClassifier:
    """Nạp model đúng 1 lần; mọi lần suy luận đi qua 1 lock để không tranh CPU."""

    def __init__(self, settings: Settings) -> None:
        self._s = settings
        self._lock = threading.Lock()
        self._tokenizer = None
        self._model = None

    @property
    def ready(self) -> bool:
        return self._model is not None

    def load(self) -> None:
        s = self._s
        torch.set_num_threads(s.torch_threads)
        try:
            torch.set_num_interop_threads(1)
        except RuntimeError:  # thread pool đã được khởi tạo trước đó
            pass

        log.info("Loading %s (threads=%d, quantize=%s)", s.hf_model_name, s.torch_threads, s.quantize)
        tokenizer = AutoTokenizer.from_pretrained(s.hf_model_name, revision=s.hf_revision)
        model = AutoModelForSequenceClassification.from_pretrained(s.hf_model_name, revision=s.hf_revision)
        if model.config.num_labels != len(LABELS):
            raise RuntimeError(f"Model có {model.config.num_labels} nhãn, mong đợi {len(LABELS)}")
        model.eval()
        if s.quantize:
            model = torch.quantization.quantize_dynamic(model, {torch.nn.Linear}, dtype=torch.qint8)

        self._tokenizer, self._model = tokenizer, model
        self.classify("khởi động mô hình")  # warm-up: cấp phát bộ nhớ trước request đầu tiên
        log.info("Model ready")

    def classify(self, text: str) -> TextResponse:
        with self._lock, torch.inference_mode():
            probs = self._predict(text)  # [n_chunks, 3]

        # Văn bản dài: lấy đoạn "xấu" nhất (xác suất CLEAN thấp nhất) làm kết quả đại diện
        worst = probs[(1.0 - probs[:, 0]).argmax()].tolist()
        idx = max(range(len(LABELS)), key=worst.__getitem__)
        return TextResponse(
            label=LABELS[idx],
            score=round(worst[idx], 4),
            scores={name: round(p, 4) for name, p in zip(LABELS, worst)},
            chunks=probs.shape[0],
        )

    def _predict(self, text: str) -> torch.Tensor:
        s = self._s
        if self._tokenizer.is_fast:
            enc = self._tokenizer(
                text, truncation=True, max_length=s.max_length, stride=s.stride,
                return_overflowing_tokens=True, padding=True, return_tensors="pt",
            )
        else:  # tokenizer chậm không hỗ trợ overflow: chỉ lấy đoạn đầu
            enc = self._tokenizer(text, truncation=True, max_length=s.max_length, padding=True, return_tensors="pt")

        input_ids = enc["input_ids"][: s.max_chunks]
        attention_mask = enc["attention_mask"][: s.max_chunks]

        outputs = []
        for i in range(0, input_ids.shape[0], s.batch_size):
            logits = self._model(
                input_ids=input_ids[i : i + s.batch_size],
                attention_mask=attention_mask[i : i + s.batch_size],
            ).logits
            outputs.append(torch.softmax(logits, dim=-1))
        return torch.cat(outputs)