---
name: commit-suggest
description: Gợi ý commit message (và tên nhánh khi được yêu cầu) theo chuẩn COMMIT_CONVENTION.md của repo, dựa trên các thay đổi git mới nhất. Nếu thay đổi nên tách thành nhiều commit thì chia nhóm và đưa luôn lệnh `git add` 1 dòng cho từng commit. LUÔN dùng skill này khi người dùng nói "commit", "gợi ý commit", "commit message", "viết commit", "đặt tên nhánh", "branch name", "git add", "chia commit", hoặc gọi /commit-suggest, kể cả khi họ không nhắc tên skill hay tên file convention.
---

# Commit Suggest

Gợi ý commit message / tên nhánh theo `COMMIT_CONVENTION.md` cho các thay đổi mới nhất. Skill chỉ **gợi ý**, không tự chạy `git add`, `git commit`, `git push`, `git checkout -b`.

## Bước 1: Đọc convention

1. Tìm `COMMIT_CONVENTION.md`, ưu tiên theo thứ tự: `.github/COMMIT_CONVENTION.md` (vị trí thường dùng của người dùng), thư mục gốc repo, `docs/`. Lấy gốc repo bằng `git rev-parse --show-toplevel`, nếu cần thì `find` để tìm.
2. Đọc toàn bộ file và tuân thủ đúng: format, danh sách type/scope, ngôn ngữ, độ dài subject, quy tắc đặt tên nhánh.
3. Nếu không tìm thấy file: dùng Conventional Commits (`type(scope): subject`, tiếng Anh, imperative, subject ≤ 72 ký tự) và nói ngắn gọn một dòng là không thấy file convention.

## Bước 2: Xem thay đổi mới nhất

Chạy các lệnh sau (chỉ đọc):

```bash
git status --short
git diff --stat
git diff              # thay đổi chưa staged
git diff --cached     # thay đổi đã staged
```

- File untracked (`??`) cũng là thay đổi mới: xem nội dung nếu cần để hiểu mục đích.
- Diff quá dài thì xem `--stat` trước, rồi đọc diff của từng file/nhóm quan trọng.
- Nếu working tree sạch: dùng `git show --stat HEAD` / `git diff HEAD~1` để gợi ý (hoặc sửa) message cho commit gần nhất, và nói rõ đang làm vậy.

## Bước 3: Quyết định số commit

Gom thay đổi theo **một mục đích logic** (một tính năng, một bugfix, một refactor, một thay đổi docs/config/test riêng).

- Chỉ 1 mục đích: **1 commit**.
- Nhiều mục đích khác nhau (ví dụ feat + fix + docs, hoặc nhiều module không liên quan): tách **nhiều commit**, mỗi commit là một nhóm file gắn kết. Không tách quá vụn; test đi kèm tính năng nào thì nằm cùng commit tính năng đó.
- Thứ tự commit: những thay đổi nền tảng/độc lập trước, phụ thuộc sau.

## Bước 4: Định dạng đầu ra

Trả lời bằng tiếng Việt, ngắn gọn. Commit message viết theo ngôn ngữ mà convention quy định.

**1 commit:**

```bash
git add <file1> <file2> ...
git commit -m "<type(scope): subject>" -m "- <ý 1>
- <ý 2>
- <ý 3>"
```

**Nhiều commit:** với mỗi commit, ghi một dòng mô tả mục đích rồi đưa lệnh:

```bash
# Commit 1: <mô tả ngắn>
git add <file1> <file2> ...
git commit -m "<subject 1>" -m "- <ý 1>
- <ý 2>"

# Commit 2: <mô tả ngắn>
git add <file3> <dir/> ...
git commit -m "<subject 2>" -m "- <ý 1>
- <ý 2>"
```

Quy tắc cho lệnh `git add`:
- **Mỗi commit đúng 1 dòng `git add`**, liệt kê path cụ thể (có thể dùng thư mục nếu toàn bộ thư mục thuộc cùng commit). Không dùng `git add .` hay `-A` khi có nhiều commit.
- Path dùng `/`, bọc dấu nháy kép nếu có khoảng trắng.
- Nếu một file chứa thay đổi thuộc 2 commit khác nhau, nhắc dùng `git add -p <file>` cho file đó.
- Không đưa vào commit các file không nên commit (`.env`, secret, file build/log, `node_modules`); nếu thấy, cảnh báo ngắn.

## Body của commit (luôn có)

Mỗi commit gồm **subject + body**. Body là danh sách gạch đầu dòng nêu những gì đã làm:

- 2 đến 5 gạch đầu, mỗi gạch 1 dòng, bắt đầu bằng động từ (theo ngôn ngữ/giọng văn convention quy định), nêu *đã làm gì* ở mức khái niệm (thêm endpoint X, tách logic Y ra service, bổ sung test cho Z), không liệt kê từng file hay từng dòng code.
- Ngắn gọn, đủ ý. Commit rất nhỏ (đổi 1 dòng, sửa typo, đổi version) chỉ cần subject hoặc 1 gạch, đừng độn thêm.
- Thêm body bằng `-m` thứ hai chứa nhiều dòng (xuống dòng thật bên trong dấu nháy kép), không dùng `\n`. Chạy được trên bash, Git Bash và PowerShell.
- Trong body tránh dấu backtick và ký tự `$` vì shell sẽ diễn giải chúng; dùng chữ thường hoặc nháy đơn thay thế.
- Nếu convention quy định footer (ví dụ `Closes #12`, `BREAKING CHANGE:`), thêm vào cuối body theo đúng format của convention.

## Tên nhánh: chỉ khi được nhắc

- **Chỉ** gợi ý tên nhánh khi người dùng đề cập đến nhánh/branch trong yêu cầu ("gợi ý tên nhánh", "tạo branch", "đặt tên branch"...). Nếu không nhắc thì **không** thêm phần tên nhánh.
- Đặt tên theo quy tắc nhánh trong `COMMIT_CONVENTION.md` (nếu không có: `<type>/<kebab-case-mô-tả-ngắn>`, ví dụ `feat/user-profile-page`).
- Đưa 1 tên chính, tối đa 2 phương án thay thế, kèm lệnh `git checkout -b <tên>` (hoặc `git switch -c`).

## Lưu ý

- Mô tả thay đổi dựa trên diff thực tế, không đoán.
- Subject nêu "làm gì / vì sao" cô đọng; body liệt kê các ý chính bằng gạch đầu dòng, không liệt kê tên file.
- Nếu người dùng thêm ngữ cảnh (ví dụ số ticket, "fix lỗi login"), đưa vào scope/footer theo convention.
- Nếu convention mâu thuẫn với mặc định ở trên thì convention thắng.