# FlashLearn

FlashLearn to innowacyjna aplikacja mobilna do nauki, która rozwiązuje problem nieefektywnego zapamiętywania poprzez wykorzystanie krzywej zapominania Ebbinghausa. System automatyzuje proces tworzenia materiałów dzięki AI oraz optymalizuje powtórki za pomocą algorytmu spaced repetition.

## Zespół
* **Jakub Siłka** – [@jakub7038](https://github.com/jakub7038)
* **Paweł Powęska** – [@SpeedYoo](https://github.com/SpeedYoo)
* **Piotr Gorzkiewicz** – [@g0rzki](https://github.com/g0rzki)
* **Krzysztof Dąbrowski** – [@SooNlK](https://github.com/SooNlK)

## Stos technologiczny
* **Android:** Kotlin, Jetpack Compose, Room (SQLite)
* **Backend:** Java + Spring Boot, Docker
* **Baza danych:** PostgreSQL
* **CI/CD:** GitHub Actions
* **Zarządzanie:** Jira, metodyka zwinna (Scrum/Kanban)

## Architektura i Moduły

### Opis Architektury
System opiera się na architekturze klient-serwer z podejściem **offline-first**. Aplikacja mobilna posiada lokalną bazę danych, która synchronizuje się z backendem (REST API) po odzyskaniu połączenia. Całość infrastruktury serwerowej jest skonteneryzowana przy użyciu Docker.


### Podział na moduły
* **Moduł Mobilny:** Obsługa interfejsu (Compose), lokalna baza (Room) oraz implementacja algorytmu SM-2.
* **Moduł AI:** Integracja z zewnętrznymi modelami w celu generowania fiszek z surowego tekstu.
* **Moduł Synchronizacji:** Zarządzanie spójnością danych między urządzeniem a serwerem.
* **Moduł Społecznościowy:** Marketplace umożliwiający udostępnianie i pobieranie publicznych talii.

## Sprint Backlog (to jest jeszcze do edycji)
1. **Sprint 1**
2. **Sprint 2**
3. **Sprint 3:**
4. **Sprint 4**
5. **Sprint 5** 
6. **Sprint 6** 

## Uruchomienie lokalne

### Backend

### Android

## Dokumentacja API
