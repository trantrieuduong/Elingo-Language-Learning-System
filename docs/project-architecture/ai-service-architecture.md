**`AI Service`** được thiết kế dưới dạng một **Microservice độc lập** (viết bằng Python), giao tiếp với Backend chính (Spring Boot) thông qua REST API.

### 1. Tổ chức Modules

```text
ai-service/
├── api/
│   ├── routes.py              # Xử lý HTTP requests (nhận file audio, transcript)
│   └── schemas.py             # Chứa các pydantic base model
├── core/
│   └── orchestrator.py        # Controller chính gọi lần lượt Tầng 1 -> Tầng 2
├── modules/
│   ├── tier1_extraction/      # TẦNG 1
│   │   ├── prosody.py         # Trích xuất Pitch (F0) và Năng lượng (Energy)
│   │   ├── lexicon.py         # Bộ lọc ràng buộc ngôn ngữ (Lexicon Contrainst)
│   │   └── alignment.py       # Forced Alignment (trích xuất mốc thời gian âm vị)
│   └── tier2_scoring/         # TẦNG 2
│       ├── gop.py             # Tính điểm GOP (Goodness of Pronunciation)
│       ├── intonation.py      # Chấm điểm F0 (So sánh đường cong ngữ điệu)
│       └── rhythm.py          # Tính điểm nhịp điệu (Pause, Speech rate, Duration)
├── .env                       # Biến môi trường
└── requirements.txt           # Thư viện cần thiết cho aiService

```

### 2. Chi tiết các thành phần & Thư viện đề xuất

#### Tầng 1: Trích xuất đặc trưng (Feature Extraction)
Nhiệm vụ: Nhận file audio thô, transcript và xác định các đặc trưng.
- **Thư viện khuyên dùng:** 
  - **parselmouth**: trích xuất F0/Pitch.
  - **HuggingFace(Wav2Vec 2.0):** Tính ma trận xác suất của các âm vị và Forced Alignment.
- **Đầu ra:** F0 contour, Intensity (Energy) contour, Danh sách các âm vị cùng timestamps (start, end), Ma trận xác suất âm vị (Phoneme probabilities).

#### Tầng 2: Scoring Engine
Nhiệm vụ: Chấm điểm dựa trên dữ liệu từ Tầng 1.
- **GOP (Goodness of Pronunciation):** Tính tỷ lệ log-likelihood (dựa trên ma trận xác suất âm vị).
- **Intonation (Ngữ điệu):** Dùng `scipy` hoặc `fastdtw` (Dynamic Time Warping) để so sánh đường cong F0 của người học với file audio bản xứ (dựa vào F0 contour, Intensity contour).
- **Rhythm (Nhịp điệu):** Thuật toán tự viết dựa trên timestamps của Forced Alignment. Tính toán số âm tiết/giây, xác định các khoảng lặng (pauses) lớn hơn một threshold nhất định (vd: 0.3s).

### 3. Thiết kế API Payload (Giao tiếp với Spring Boot)

**Request từ Spring Boot sang aiService:**
```json
{
  "transcript": "I want to improve my English speaking skills.",
  "audio_url": "s3://bucket/audio/user_123.wav"
}
```
*(Lưu ý: Thay vì gửi trực tiếp file audio qua HTTP gây nặng băng thông, Spring Boot nên lưu file lên S3 hoặc thư mục chung và truyền URL cho aiService đọc).*

**Response trả về cho Spring Boot:**
```json
{
  "overall_score": 85,
  "pronunciation": {
    "gop_score": 88,
    "phoneme_details": [
      {"word": "I", "phoneme": "AY", "score": 95, "start": 0.1, "end": 0.25},
      {"word": "want", "phoneme": "W", "score": 60, "start": 0.3, "end": 0.35} 
      // ...
    ]
  },
  "intonation": {
    "score": 82,
    "dtw_distance": 15.4,
    "feedback": "Your pitch is a bit flat at the end of the sentence."
  },
  "rhythm": {
    "score": 85,
    "speech_rate": 3.2, // âm tiết/giây
    "pause_metrics": {
      "unexpected_pauses": 1, // lần ngắt sai
      "longest_pause": 0.8 // khoảng lặng dài nhất
    }
  }
}
```