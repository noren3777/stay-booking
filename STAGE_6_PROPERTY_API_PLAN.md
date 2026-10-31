# Этап 6 — Property public API

Источник требований: [Этап 6 в Notion](https://app.notion.com/p/3e9936b6df42811c8ac0c43cded7e3ec). Цель этапа — дать будущему Booking только факты, необходимые для создания бронирования, не раскрывая `property.internal`.

## Что уже есть в проекте

- Модуль `property` объявляет `property.api` как `@NamedInterface("api")`; `booking` разрешено зависеть от `property::api`. `ModularityTest` уже вызывает `ApplicationModules.verify()`.
- `Property` хранит `hostId`, `pricePerNight`, `maxGuests`, `status` и наследует `deletedAt`. Статусы: `ACTIVE`, `INACTIVE`, `BLOCKED`; Hibernate `@SQLRestriction("deleted_at IS NULL")` скрывает soft-deleted записи при чтении через репозиторий.
- Цена нормализуется до scale 2 в `Property`, хранится как `NUMERIC(12,2)` и передаётся через `BigDecimal`. Дополнительный денежный тип на этом этапе не нужен.
- `PropertyService.getActiveById()` обслуживает HTTP detail и строит полный результат с amenities/images. Его нельзя переиспользовать как межмодульный API: он скрывает неактивные объекты и загружает лишние данные.
- В `property.api` пока нет контракта; Booking пока не реализован. Изменения схемы, HTTP endpoint и классы Booking для этого этапа не требуются.

## Решение по контракту

```java
public interface PropertyApi {
    Optional<PropertyBookingView> findForBooking(UUID propertyId);
}

public record PropertyBookingView(
        UUID propertyId,
        UUID hostId,
        BigDecimal pricePerNight,
        int maxGuests,
        boolean bookable
) {}
```

`findForBooking` отражает потребность Booking, а `Optional` явно задаёт семантику отсутствия. Несуществующий или soft-deleted объект даёт `Optional.empty()`. `ACTIVE` даёт view с `bookable=true`; `INACTIVE` и `BLOCKED` дают view с `bookable=false`. Booking сможет отклонять все три случая, не зная внутренний `PropertyStatus`. Метод не делает HTTP-проверок, не читает Booking и не проверяет даты/доступность интервала. `BigDecimal` immutable; значение уже имеет scale 2 благодаря инварианту Property и типу колонки. В view нет коллекций, JPA-сущностей и web DTO.

## Шаг 1 — правило bookability внутри Property (коммит 1)

**Результат коммита:** объект Property сам отвечает, можно ли его бронировать по его текущему состоянию.

1. Добавить в `property.internal.domain.entity.Property` метод `isBookable()` (или равнозначное короткое доменное имя): `status == ACTIVE && !isDeleted()`. Не вводить отдельную абстракцию или дублировать условие в сервисе.
2. В `PropertyTest` проверить начальный `INACTIVE`, активированный объект, деактивированный/заблокированный объект и активный объект после `delete()`. Проверять через публичное поведение aggregate, без изменения приватных полей.

**Граница коммита:** только доменное правило и его unit tests. Нет пустого `PropertyApi`, адаптера, репозиторных методов, SQL-миграций или Booking-кода.

**Проверка:** `./mvnw -q -Dtest=PropertyTest test`; существующие тесты домена остаются зелёными.

## Шаг 2 — минимальный межмодульный API (коммит 2)

**Результат коммита:** другой модуль может получить booking facts через `PropertyApi`, не импортируя Property internals.

1. Создать `PropertyApi` и `PropertyBookingView` непосредственно в `property.api`. Оставить ровно пять полей из раздела «Решение по контракту»; не публиковать `PropertyStatus`, `PropertyResult`, `PropertyResponse`, `Amenity` или `PropertyImage`.
2. Создать Spring `PropertyApiImpl` в `property.internal.application.service`, по образцу `IdentityApiImpl`. Метод пометить `@Transactional(readOnly = true)`. Через внутренний `PropertyRepository.findById()` получить aggregate и сразу собрать record из скалярных полей и `property.isBookable()`. Не проходить через `PropertyService.getActiveById()`, HTTP mapper или коллекции; отдельный mapper ради пяти полей не добавлять.
3. Написать `PropertyApiIntegrationTest` с PostgreSQL/Testcontainers в стиле существующих интеграционных тестов. Вызывать проверяемый сценарий только через внедрённый `PropertyApi`; фикстуры создавать внутри Property test setup. Проверить: значения всех полей и scale 2 для `ACTIVE`; `bookable=false` для `INACTIVE` и `BLOCKED`; `Optional.empty()` для отсутствующего и soft-deleted ID. Для удаления очистить persistence context перед вызовом API, чтобы проверялось чтение из БД через `@SQLRestriction`.
4. Обновить README: убрать утверждение, что cross-module Property API ещё не реализован, кратко описать контракт и правило его изменения — новое поле добавляется только при подтверждённой потребности consumer. Не описывать несуществующий Booking flow.

**Граница коммита:** контракт, его рабочая реализация, contract integration test и относящаяся к нему документация. Не менять `booking`, `identity`, HTTP endpoints, схему БД или правила каталога.

**Проверка:** `./mvnw -q -Dtest=PropertyApiIntegrationTest,ModularityTest test`, затем `./mvnw clean verify`. `ModularityTest` проверяет существующую границу модулей; фактическую зависимость Booking от API можно будет проверить только после появления Booking consumer на этапе 7.

## Критерий завершения этапа

- В `property.api` опубликованы только business-oriented интерфейс и минимальный immutable view; реализация и репозиторий остаются internal.
- Для активного объекта возвращаются корректные `propertyId`, `hostId`, `pricePerNight`, `maxGuests` и `bookable=true`; для неактивного/заблокированного — `bookable=false`; удалённый и отсутствующий не возвращаются.
- `PropertyApiIntegrationTest` и `ModularityTest` проходят; полный `./mvnw clean verify` зелёный.
- Booking можно начать реализовывать без импорта `property.internal`, JPA entity или HTTP DTO. Проверки дат, self-booking, capacity и price snapshot остаются задачами этапа 7.
