# Cloud File Storage

![Java](https://img.shields.io/badge/Java-17%2B-red.svg?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-24%2B-blue.svg?logo=docker&logoColor=white)

Многофункциональное облачное хранилище файлов с надежным REST API и интуитивно понятным веб-интерфейсом для управления. Проект вдохновлен функциональностью Google Drive и предоставляет безопасную и эффективную платформу для хранения данных.
## Ключевые особенности

- **Управление пользователями:** Простая регистрация, аутентификация и авторизация.
- **Операции с файлами:**
    - Загрузка и скачивание файлов и папок (папки скачиваются как ZIP-архивы).
    - Управление файловой системой: удаление, переименование, перемещение и копирование.
    - Поддержка рекурсивной загрузки папок для удобной передачи больших объемов данных.
- **Расширенный поиск:** Быстрый поиск файлов и папок по имени.
- **Масштабируемое хранилище:** Интеграция с MinIO для надежного и совместимого с S3 объектного хранилища.
- **Управление сессиями:** Использование Redis для масштабируемого управления пользовательскими сессиями.
- **Интерактивная документация API:** Автоматически генерируемая документация через Swagger.

## Технологии и инструменты

Современный стек технологий обеспечивает высокую производительность, масштабируемость и удобство поддержки.

### Backend

| Технология/Инструмент | Описание                                                                 | Иконка                                                                                              |
|-----------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Java                  | Основной язык программирования, обеспечивающий надежность и кроссплатформенность. | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/java/java-original-wordmark.svg" width="40" height="40" alt="Java"/> |
| Maven                 | Инструмент автоматизации сборки, управление зависимостями и жизненным циклом. | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/maven/maven-original-wordmark.svg" width="40" height="40" alt="Maven"/> |
| Spring Boot           | Упрощает настройку приложений на Spring для быстрой разработки.           | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original-wordmark.svg" width="40" height="40" alt="Spring Boot"/> |
| Spring Security       | Мощная и настраиваемая аутентификация и контроль доступа.                | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original-wordmark.svg" width="40" height="40" alt="Spring Security"/> |
| Spring Session        | Управление HTTP-сессиями в распределенной среде с поддержкой Redis.      | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original-wordmark.svg" width="40" height="40" alt="Spring Session"/> |
| Spring Data JPA       | Упрощает разработку уровня доступа к данным с помощью репозиториев.      | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original-wordmark.svg" width="40" height="40" alt="Spring Data JPA"/> |
| Hibernate             | ORM-фреймворк для абстракции взаимодействия с базой данных.              | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/hibernate/hibernate-original-wordmark.svg" width="40" height="40" alt="Hibernate"/> |
| Swagger               | Генерация интерактивной документации REST API.                           | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/swagger/swagger-original.svg" width="40" height="40" alt="Swagger"/> |

### Базы данных и хранилище

| Технология/Инструмент | Описание                                                                      | Иконка                                                                                                                                                     |
|-----------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **PostgreSQL**        | Реляционная база данных для хранения пользовательских данных и метаданных.     | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/postgresql/postgresql-original-wordmark.svg" width="40" height="40" alt="PostgreSQL"/> |
| **MinIO**             | S3-совместимое объектное хранилище для файлов.                                | <img src="https://blog.ozkula.com/wp-content/uploads/2023/08/minio-nedir.png" width="40" height="20" alt="MinIO"/>                            |
| **Redis**             | Хранилище ключ-значение для управления сессиями и кэширования.                | <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/redis/redis-original-wordmark.svg" width="40" height="40" alt="Redis"/>                |


## Требования

Для запуска проекта убедитесь, что установлены:

- **JDK:** Версия 17 или выше.
- **Docker:** Версия 24.0 или выше.
- **IntelliJ IDEA:** Рекомендуется для разработки благодаря поддержке Spring Boot.
- **Оперативная память:** Минимум 2 ГБ свободной памяти.

## Установка и запуск

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/DavidTagirov/storage.git
   cd storage
   ```

2. **Запустите сервисы с Docker Compose:**
    - В терминале в корне проекта выполните:
   ```bash
   mvn clean package -DskipTests && docker-compose down && docker-compose build && docker-compose up -d
   ```

5. **Доступ к приложению:**
   
    Временно приложение будет доступно по адресу `http://81.177.175.247:8080/`

    После чего доступ будет только локально на вашем ПК:
    - по адресу: `http://localhost:8080`
    - Swagger UI: `http://localhost:8080/swagger-ui.html`

## Тестирование

- **Интеграционное тестирование:** Проверяет взаимодействие компонентов и внешних сервисов.
- **Testcontainers:** Используется для создания изолированных контейнеров базы данных и других сервисов.

Пример запуска тестов:
```bash
mvn test
mvn verify -Pintegration-tests
```
