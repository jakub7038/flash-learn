# FlashLearn — Developer Setup Guide

## Wymagania

| Narzędzie | Wersja | Pobierz |
|-----------|--------|---------|
| JDK | 21+ | [adoptium.net](https://adoptium.net) |
| Android Studio | najnowsza | [developer.android.com](https://developer.android.com/studio) |
| Docker Desktop | najnowsza | [docker.com](https://www.docker.com/products/docker-desktop) |
| Git | dowolna | [git-scm.com](https://git-scm.com) |

---

## Pierwsze uruchomienie

### 1. Klonowanie repozytorium

```bash
git clone https://github.com/jakub7038/flash-learn.git
cd flash-learn
```

### 2. Konfiguracja zmiennych środowiskowych

```bash
cp docker/.env.example docker/.env
```

Otwórz `docker/.env` i uzupełnij wartości:

```env
POSTGRES_DB=flashlearn
POSTGRES_USER=flashlearn_user
POSTGRES_PASSWORD=twoje_haslo
JWT_SECRET=dlugitajnyklucz_min32znaki
JWT_EXPIRATION_MS=3600000
```

### 3. Uruchomienie backendu (Docker)

```bash
cd docker
docker compose up --build   # pierwsze uruchomienie (buduje obraz)
docker compose up           # kolejne uruchomienia
```

Backend dostępny pod: `http://localhost:8080`

### 4. Zatrzymanie

```bash
docker compose down         # zatrzymaj kontenery (dane zachowane)
docker compose down -v      # zatrzymaj i usuń dane bazy
```

---

## Połączenie z bazą danych (pgAdmin)

> **Uwaga:** Jeśli masz lokalnie zainstalowanego PostgreSQL, może on zajmować port `5432`.
> W takim przypadku Docker używa portu `5433` — sprawdź `docker/docker-compose.yaml`.

| Pole | Wartość |
|------|---------|
| Host | `localhost` |
| Port | `5433` (lub `5432` jeśli brak lokalnego Postgresa) |
| Database | wartość `POSTGRES_DB` z `.env` |
| Username | wartość `POSTGRES_USER` z `.env` |
| Password | wartość `POSTGRES_PASSWORD` z `.env` |

---

## Struktura repozytorium

```
flash-learn/
├── android/        # aplikacja mobilna (Kotlin + Jetpack Compose)
├── backend/        # REST API (Java + Spring Boot)
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── docker/         # konfiguracja Docker Compose
│   ├── docker-compose.yaml
│   ├── .env.example
│   └── .env        # NIE commitować!
└── docs/
    └── diagrams/   # ERD, diagramy UML
```

---

## Ważne informacje

- `ddl-auto: update` — Spring automatycznie tworzy/aktualizuje tabele na podstawie encji JPA. Na produkcji zmienić na `validate`
- Healthcheck w compose — serwis `app` czeka na gotowość bazy (`service_healthy`) zanim wystartuje
- Plik `.env` jest w `.gitignore` — **nigdy nie commituj go do repozytorium**

---

## Troubleshooting

### Port 5432 zajęty
**Objaw:** pgAdmin odmawia połączenia lub Docker nie startuje poprawnie.  
**Przyczyna:** Lokalny PostgreSQL zajmuje port `5432`.  
**Rozwiązanie:** W `docker/docker-compose.yaml` zmień mapowanie portu:
```yaml
ports:
  - "5433:5432"
```
W pgAdmin połącz się przez port `5433`.

---

### Tabele nie pojawiają się w bazie
**Objaw:** Baza `flashlearn` istnieje ale jest pusta.  
**Rozwiązanie:** Sprawdź logi aplikacji:
```bash
docker compose logs app
```
Najczęstsza przyczyna: stare dane na wolumenie. Wyczyść i uruchom od nowa:
```bash
docker compose down -v
docker compose up --build
```

---

### Błąd autoryzacji w pgAdmin
**Objaw:** `password authentication failed`  
**Przyczyna:** pgAdmin łączy się z lokalnym Postgresem zamiast Dockerowym.  
**Rozwiązanie:** Upewnij się że używasz portu `5433` i danych z `docker/.env`.

---

### Build Dockera nie widzi nowych plików Java
**Objaw:** Encje nie są tworzone mimo że pliki istnieją lokalnie.  
**Rozwiązanie:** Wymuś pełny rebuild bez cache:
```bash
docker compose down -v
docker rmi docker-app
docker compose build --no-cache
docker compose up
```

---

### `./mvnw: Permission denied` (Linux/Mac)
```bash
chmod +x backend/mvnw
```

---

### Aplikacja nie łączy się z bazą przy starcie
**Objaw:** `Connection to localhost:5432 refused` w logach.  
**Przyczyna:** Aplikacja uruchomiona poza Docker Compose (bez sieci Dockera).  
**Rozwiązanie:** Zawsze uruchamiaj przez `docker compose up`, nigdy przez `java -jar` bezpośrednio.
