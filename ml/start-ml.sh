#!/usr/bin/env bash
set -e

# Переходим в папку ml
cd "$(dirname "$0")"

# Создаём venv при необходимости
if [ ! -d ".venv" ]; then
  echo "🔧 Creating venv..."
  python3.11 -m venv .venv
  source .venv/bin/activate
  pip install -r requirements.txt
else
  source .venv/bin/activate
fi

# Устанавливаем путь к датасету
export DATA_PATH="$PWD/data/got.yaml"

# Запуск uvicorn
echo "🚀 Starting ML service on http://localhost:8000 ..."
uvicorn app:app --host 0.0.0.0 --port 8000 --reload