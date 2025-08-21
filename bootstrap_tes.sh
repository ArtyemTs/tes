set -euo pipefail

mkdir -p infra api/src/main/java/com/tes/api/{controller,service,dto,client} \
         api/src/main/resources ml/data web/src .github/workflows

# ---------- infra/docker-compose.yml ----------
cat > infra/docker-compose.yml <<'EOF'
services:
  ml:
    build: ../ml
    container_name: tes-ml
    ports: ["8000:8000"]
    environment:
      - LOG_LEVEL=info
    restart: unless-stopped
  api:
    build: ../api
    container_name: tes-api
    depends_on: [ml]
    ports: ["8080:8080"]
    environment:
      - ML_BASE_URL=http://ml:8000
      - SERVER_PORT=8080
    restart: unless-stopped
  web:
    build: ../web
    container_name: tes-web
    depends_on: [api]
    ports: ["5173:80"]
    environment:
      - VITE_API_BASE=http://localhost:8080
    restart: unless-stopped
EOF

# ---------- API ----------
cat > api/pom.xml <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.tes</groupId>
  <artifactId>tes-api</artifactId>
  <version>0.1.0</version>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.2</spring.boot.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-json</artifactId></dependency>
    <dependency><groupId>org.apache.httpcomponents.client5</groupId><artifactId>httpclient5</artifactId><version>5.3.1</version></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><configuration><release>21</release></configuration></plugin>
    </plugins>
  </build>
</project>
EOF

cat > api/src/main/resources/application.yml <<'EOF'
server:
  port: ${SERVER_PORT:8080}
ml:
  baseUrl: ${ML_BASE_URL:http://localhost:8000}
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics"
EOF

cat > api/src/main/java/com/tes/api/ApiApplication.java <<'EOF'
package com.tes.api;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }
}
EOF

cat > api/src/main/java/com/tes/api/dto/RecommendationRequest.java <<'EOF'
package com.tes.api.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RecommendationRequest {
  @NotBlank private String showId;
  @Min(1) private int targetSeason;
  @Min(1) @Max(5) private int immersion;
  private String locale;
}
EOF

cat > api/src/main/java/com/tes/api/dto/RecommendationItem.java <<'EOF'
package com.tes.api.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data @NoArgsConstructor @AllArgsConstructor
public class RecommendationItem {
  private int season;
  private int episode;
  private String reason;
  private String title;
}
EOF

cat > api/src/main/java/com/tes/api/dto/RecommendationResponse.java <<'EOF'
package com.tes.api.dto;
import java.util.List;
import lombok.Data;
@Data
public class RecommendationResponse {
  private String showId;
  private int targetSeason;
  private int immersion;
  private List<RecommendationItem> items;
}
EOF

cat > api/src/main/java/com/tes/api/client/MlClient.java <<'EOF'
package com.tes.api.client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class MlClient {
  private final ObjectMapper mapper = new ObjectMapper();
  @Value("${ml.baseUrl}") private String baseUrl;
  public RecommendationResponse recommend(RecommendationRequest request) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost post = new HttpPost(baseUrl + "/recommend");
      post.setEntity(new StringEntity(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
      return client.execute(post, resp -> mapper.readValue(resp.getEntity().getContent(), RecommendationResponse.class));
    } catch (Exception e) {
      throw new RuntimeException("Failed to call ML service: " + e.getMessage(), e);
    }
  }
}
EOF

cat > api/src/main/java/com/tes/api/service/RecommendationService.java <<'EOF'
package com.tes.api.service;
import com.tes.api.client.MlClient;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final MlClient mlClient;
  public RecommendationResponse getRecommendations(RecommendationRequest req) {
    return mlClient.recommend(req);
  }
}
EOF

cat > api/src/main/java/com/tes/api/controller/RecommendationController.java <<'EOF'
package com.tes.api.controller;
import com.tes.api.dto.RecommendationRequest;
import com.tes.api.dto.RecommendationResponse;
import com.tes.api.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RecommendationController {
  private final RecommendationService service;
  @PostMapping("/recommendations")
  public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
    return service.getRecommendations(request);
  }
}
EOF

cat > api/Dockerfile <<'EOF'
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/tes-api-0.1.0.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
EOF

# ---------- ML ----------
cat > ml/requirements.txt <<'EOF'
fastapi==0.115.0
uvicorn==0.30.6
pyyaml==6.0.2
pydantic==2.8.2
EOF

cat > ml/models.py <<'EOF'
from pydantic import BaseModel, Field
from typing import List, Optional
class RecommendationRequest(BaseModel):
    showId: str
    targetSeason: int = Field(ge=1)
    immersion: int = Field(ge=1, le=5)
    locale: Optional[str] = "en"
class RecommendationItem(BaseModel):
    season: int
    episode: int
    title: Optional[str] = None
    reason: str
class RecommendationResponse(BaseModel):
    showId: str
    targetSeason: int
    immersion: int
    items: List[RecommendationItem]
EOF

cat > ml/logic.py <<'EOF'
import yaml
from typing import Dict, Any, List
from .models import RecommendationRequest, RecommendationItem, RecommendationResponse
def load_dataset(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)
def recommend(req: RecommendationRequest, data: Dict[str, Any]) -> RecommendationResponse:
    show = data["shows"].get(req.showId)
    if not show:
        return RecommendationResponse(showId=req.showId, targetSeason=req.targetSeason, immersion=req.immersion, items=[])
    threshold = {1:5, 2:4, 3:3, 4:2, 5:1}[req.immersion]
    items: List[RecommendationItem] = []
    for season in show["seasons"]:
        if season["number"] >= req.targetSeason:
            break
        for ep in season["episodes"]:
            imp = ep.get("importance", 3)
            if imp >= threshold:
                reason_parts = []
                if ep.get("arcs"): reason_parts.append("arcs: " + ", ".join(ep["arcs"][:3]))
                if ep.get("summary_short"): reason_parts.append(ep["summary_short"])
                reason = " — ".join(reason_parts) if reason_parts else "Key plot episode"
                items.append(RecommendationItem(season=season["number"], episode=ep["number"], title=ep.get("title"), reason=reason))
    core_arcs = set(show.get("core_arcs", []))
    covered = set()
    for it in items:
        for ep in next(s for s in show["seasons"] if s["number"]==it.season)["episodes"]:
            if ep["number"]==it.episode:
                covered.update(ep.get("arcs", []))
    missing = list(core_arcs - covered)
    if missing:
        for arc in missing:
            candidate = None
            for season in show["seasons"]:
                if season["number"] >= req.targetSeason: break
                for ep in sorted(season["episodes"], key=lambda e: -e.get("importance",3)):
                    if arc in ep.get("arcs", []):
                        candidate = (season["number"], ep)
                        break
                if candidate: break
            if candidate:
                snum, ep = candidate
                if not any((it.season==snum and it.episode==ep["number"]) for it in items):
                    items.append(RecommendationItem(season=snum, episode=ep["number"], title=ep.get("title"), reason=f"core arc: {arc}"))
    items.sort(key=lambda x: (x.season, x.episode))
    return RecommendationResponse(showId=req.showId, targetSeason=req.targetSeason, immersion=req.immersion, items=items)
EOF

cat > ml/app.py <<'EOF'
from fastapi import FastAPI
from .models import RecommendationRequest, RecommendationResponse
from .logic import load_dataset, recommend
import os
app = FastAPI(title="TES ML Service", version="0.1.0")
DATA_PATH = os.environ.get("DATA_PATH", "/app/data/got.yaml")
DATA = load_dataset(DATA_PATH)
@app.post("/recommend", response_model=RecommendationResponse)
def recommend_endpoint(req: RecommendationRequest):
    return recommend(req, DATA)
EOF

cat > ml/data/got.yaml <<'EOF'
shows:
  got:
    title: "Game of Thrones"
    core_arcs: ["War of the Five Kings", "White Walkers", "Daenerys Rise", "Stark Family"]
    seasons:
      - number: 1
        episodes:
          - number: 1
            title: "Winter Is Coming"
            importance: 5
            arcs: ["Stark Family", "White Walkers"]
            summary_short: "Introduces Starks; first White Walker threat."
          - number: 7
            title: "You Win or You Die"
            importance: 5
            arcs: ["War of the Five Kings"]
            summary_short: "Ned confronts Cersei; power struggle ignites."
          - number: 9
            title: "Baelor"
            importance: 5
            arcs: ["War of the Five Kings", "Stark Family"]
            summary_short: "Shock execution pivots the realm into war."
          - number: 10
            title: "Fire and Blood"
            importance: 5
            arcs: ["Daenerys Rise"]
            summary_short: "Daenerys' turning point with dragons."
      - number: 2
        episodes:
          - number: 1
            title: "The North Remembers"
            importance: 4
            arcs: ["War of the Five Kings", "Stark Family"]
            summary_short: "Factions mobilize; revenge and survival."
          - number: 6
            title: "The Old Gods and the New"
            importance: 4
            arcs: ["Stark Family"]
            summary_short: "Stark children scattered; stakes grow."
          - number: 9
            title: "Blackwater"
            importance: 5
            arcs: ["War of the Five Kings"]
            summary_short: "Pivotal battle reshapes power in King’s Landing."
      - number: 3
        episodes:
          - number: 4
            title: "And Now His Watch Is Ended"
            importance: 5
            arcs: ["Daenerys Rise"]
            summary_short: "Daenerys makes a decisive move in Essos."
          - number: 5
            title: "Kissed by Fire"
            importance: 3
            arcs: ["Stark Family"]
            summary_short: "Alliances and oaths complicate loyalties."
          - number: 9
            title: "The Rains of Castamere"
            importance: 5
            arcs: ["Stark Family", "War of the Five Kings"]
            summary_short: "Red Wedding: fate of Starks and the war turns."
EOF

cat > ml/Dockerfile <<'EOF'
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
ENV DATA_PATH=/app/data/got.yaml
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
EOF

# ---------- WEB ----------
cat > web/package.json <<'EOF'
{
  "name": "tes-web",
  "version": "0.1.0",
  "private": true,
  "scripts": { "dev": "vite", "build": "vite build" },
  "dependencies": { "react": "^18.3.1", "react-dom": "^18.3.1" },
  "devDependencies": { "vite": "^5.4.2" }
}
EOF

cat > web/vite.config.js <<'EOF'
export default { server: { port: 5173, host: true } }
EOF

cat > web/index.html <<'EOF'
<!doctype html>
<html>
  <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1.0"/><title>TES</title></head>
  <body><div id="root"></div><script type="module" src="/src/main.jsx"></script></body>
</html>
EOF

cat > web/src/main.jsx <<'EOF'
import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
createRoot(document.getElementById('root')).render(<App />)
EOF

cat > web/src/App.jsx <<'EOF'
import React, { useState } from 'react'
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080"
export default function App() {
  const [showId, setShowId] = useState('got')
  const [targetSeason, setTargetSeason] = useState(4)
  const [immersion, setImmersion] = useState(2)
  const [items, setItems] = useState([])
  async function fetchRecs(e){
    e.preventDefault()
    const res = await fetch(`${API_BASE}/recommendations`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ showId, targetSeason: Number(targetSeason), immersion: Number(immersion), locale:'en' })
    })
    const data = await res.json()
    setItems(data.items || [])
  }
  return (
    <div style={{maxWidth: 720, margin: '40px auto', fontFamily:'Inter, system-ui, sans-serif'}}>
      <h1>Through Every Season (MVP)</h1>
      <form onSubmit={fetchRecs} style={{display:'grid', gap:12}}>
        <label>Show ID <input value={showId} onChange={e=>setShowId(e.target.value)} /></label>
        <label>Target Season <input type="number" min="1" value={targetSeason} onChange={e=>setTargetSeason(e.target.value)} /></label>
        <label>Immersion (1..5) <input type="number" min="1" max="5" value={immersion} onChange={e=>setImmersion(e.target.value)} /></label>
        <button type="submit">Get recommendations</button>
      </form>
      <ul style={{marginTop:24}}>
        {items.map((it, idx)=>(
          <li key={idx}><strong>S{it.season}E{it.episode}</strong> {it.title ? `— ${it.title} — `: '— '}<i>{it.reason}</i></li>
        ))}
      </ul>
    </div>
  )
}
EOF

cat > web/Dockerfile <<'EOF'
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm i
COPY . .
RUN npm run build
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
EOF

# ---------- CI ----------
mkdir -p .github/workflows
cat > .github/workflows/ci.yml <<'EOF'
name: ci
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-22.04
    steps:
      - uses: actions/checkout@v4
      - name: API build & tests
        uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21" }
      - run: mvn -q -f api/pom.xml -DskipTests=false test
      - name: Python lint
        run: |
          python -m pip install --upgrade pip
          pip install ruff
          ruff ml
      - name: Docker build
        run: |
          docker build -t tes-ml ./ml
          docker build -t tes-api ./api
          docker build -t tes-web ./web
EOF
