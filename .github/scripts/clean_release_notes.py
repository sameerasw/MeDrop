#!/usr/bin/env python3
import os
import re

def escape_telegram_html(text: str) -> str:
    text = re.sub(r"&(?!amp;|lt;|gt;|quot;|#\d+;|#x[0-9a-fA-F]+;)", "&amp;", text)
    valid_tag_pattern = r"(</?(?:a|b|i|s|u|code|pre|blockquote)(?:\s+href=\"[^\"]*\")?\s*>)"
    parts = re.split(valid_tag_pattern, text, flags=re.IGNORECASE)
    for i in range(len(parts)):
        if not re.match(valid_tag_pattern, parts[i], flags=re.IGNORECASE):
            parts[i] = parts[i].replace("<", "&lt;").replace(">", "&gt;")
    return "".join(parts)

def clean_release_notes(raw: str) -> str:
    text = re.sub(r"<img\b[^>]*\/?>", "", raw, flags=re.IGNORECASE)
    text = re.sub(r"<a\b[^>]*>\s*</a>", "", text, flags=re.IGNORECASE)
    text = re.sub(r"^[-\*_]{3,}\s*$", "", text, flags=re.MULTILINE)
    text = re.sub(r"</?(?:details|summary|h[1-6]|p|div|span|align)\b[^>]*>", "", text, flags=re.IGNORECASE)
    text = re.sub(r"!\[.*?\]\(.*?\)", "", text)

    def convert_md_link(match):
        return f'<a href="{match.group(2)}">{match.group(1)}</a>'

    text = re.sub(r"\[(.*?)\]\((.*?)\)", convert_md_link, text)
    text = re.sub(r"\*\*(.*?)\*\*", r"<b>\1</b>", text)

    cleaned_lines = []
    in_quote = False
    quote_buf = []

    for line in text.splitlines():
        trimmed = line.lstrip()
        if trimmed.startswith(">"):
            content = trimmed[1:].strip()
            if content:
                quote_buf.append(content)
                in_quote = True
            continue
        else:
            if in_quote:
                if quote_buf:
                    cleaned_lines.append("<blockquote>" + "\n".join(quote_buf) + "</blockquote>")
                quote_buf = []
                in_quote = False

        pr_match = re.search(r"/pull/(\d+)", line)
        if pr_match:
            pr_num = pr_match.group(1)
            users = re.findall(r"@[A-Za-z0-9_-]+", line)
            author = next((u for u in users if u.lower() != "@github-actions"), users[0] if users else None)
            cleaned_lines.append(f"- #{pr_num} by {author}" if author else f"- #{pr_num}")
        else:
            cleaned_lines.append(line)

    if in_quote and quote_buf:
        cleaned_lines.append("<blockquote>" + "\n".join(quote_buf) + "</blockquote>")

    text = "\n".join(cleaned_lines)
    text = re.sub(r"^#+\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"^\*\s+", "- ", text, flags=re.MULTILINE)
    text = re.sub(r"\n\s*\n\s*\n+", "\n\n", text).strip()
    text = escape_telegram_html(text)

    return text[:3800]

if __name__ == "__main__":
    raw_body = os.environ.get("RAW_BODY", "")
    print(clean_release_notes(raw_body))
