FROM python:3.11-slim

WORKDIR /app
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app

# зависимости берём из папки ml проекта
COPY tes/ml/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

# Кладём весь пакет tes внутрь образа
COPY tes /app/tes

# Данные (если есть) переносим в /app/data
# скорректируй путь, если у тебя data лежит в другом месте
COPY tes/ml/data /app/data

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]