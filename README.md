# 🌌 AstraSMP Core — Ядро Сервера ChetCraft

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21-blue?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-orange?style=for-the-badge)
![Language](https://img.shields.io/badge/Language-Java%2021-red?style=for-the-badge&logo=openjdk)
![Status](https://img.shields.io/badge/Status-Active-green?style=for-the-badge)
![Discord](https://img.shields.io/badge/Discord-discord.gg%2Fcheterin-5865F2?style=for-the-badge&logo=discord)

Высокотехнологичное бэкенд-ядро для сервера **ChetCraft**, написанное на Java под **Minecraft 1.21 (Paper API)**. Плагин заменяет целую сборку тяжёлых модов, реализуя полноценный RPG-функционал, МЕ-сети, казино, тюремную систему, кровавые ритуалы и карманные измерения.

---

## 📋 Содержание

- [Технический стек](#️-технический-стек)
- [Системы и механики](#️-системы-и-механики)
- [Полный список команд](#-полный-список-команд)
- [Права (Permissions)](#-права-permissions)
- [Конфигурация](#️-конфигурация)
- [Сборка и деплой](#-сборка-и-деплой)

---

## ⚙️ Технический стек

| Компонент | Значение |
|-----------|----------|
| Сервер | Paper / Purpur 1.21+ |
| Язык | Java 21 |
| Сборщик | Gradle 8 (Kotlin DSL) |
| БД | MySQL 8 (HikariCP, асинхронные запросы) |
| API | Paper API 1.21, Adventure API, Spigot API |
| Зависимости | PlaceholderAPI (опц.), SitAndLay (опц.), CustomWorldHeight (опц.) |

---

## 🛠️ Системы и механики

### 🔮 1. Ритуалы, Магия, Оккультизм
*Пакеты: `rituals`, `pacts`, `blood`, `rift`*

| Система | Описание |
|---------|----------|
| **Ритуальные круги** | Многоблочные структуры для проведения ритуалов и нестандартного крафта |
| **Кровавые механики** | Сбор крови в Blood Tank'и через `BloodTankManager` |
| **Кровавая Луна** | Ивент-событие через `BloodMoonManager`, меняющее поведение мобов |
| **Порча чанков** | `ChunkCorruptionManager` / `CorruptionManager` — постепенное заражение чанков тёмной магией |
| **Астрал** | Механика выхода из тела через `AstralManager` |
| **Разломы** | Управление пространственными разломами через `RiftManager` |
| **Пакты** | Заключение сделок с сущностями за баффы/дебаффы через `PactManager` |
| **Карманные измерения** | Личные пустотные миры (VoidChunkGenerator). Вход — `/prunus`, выход — `/malus` |

---

### 💾 2. МЕ-Сети (Applied Energistics 2)
*Пакет: `model.MENetwork`, `listener.MEBlockListener`, `service.MENetworkService`*

| Компонент | Описание |
|-----------|----------|
| МЕ-Контроллер | Энергетическое ядро сети, ограничение по радиусу |
| МЕ-Ноды | Узлы подключения к сети |
| МЕ-Дисковод + GUI | Хранилище ячеек памяти; при уничтожении — ячейки выпадают |
| МЕ-Терминал | GUI-интерфейс `METerminalGui` с поиском по предметам |
| Ячейки памяти | 4K / 16K / 64K — физические предметы, Base64-сериализация через `ItemSerializer` |
| Защита от рекурсии | Нельзя положить МЕ-ячейку в МЕ-ячейку |

---

### 🎰 3. Казино
*Пакет: `casino`*

| Игра | Описание |
|------|----------|
| **Блэкджек** `BlackjackGame` | Карточная игра до 21 со своей колодой `Deck` |
| **Рулетка** `RouletteGame` | Классическая рулетка со ставками |
| **Слоты** `DrumsGame` | Барабанные автоматы |
| **Классика** `ClassicGame` | Классические азартные игры |
| **Крестики-Нолики** `TicTacToeGame` | Игра для двух игроков |

---

### ⚔️ 4. PvP, Броня, Артефакты
*Пакеты: `items`, `listener.ArmorMechanicsListener`, `listener.ItemAbilityListener`*

| Механика | Описание |
|----------|----------|
| Кастомные сеты брони | Изменяют скорость, здоровье, защиту через Data Components 1.21 |
| Кузница артефактов | `/blacksmith` — GUI крафта и улучшения уникальных предметов |
| Абилки предметов | Активные навыки с кулдаунами через `ItemAbilityListener` |
| Тотем бессмертия | 15-секундный кулдаун на повторное использование |
| Дуэли | 1×1 вызовы, отдельные арены со спавн-точками (`DuelService`) |
| Арены | `/arena` — система боёв нескольких игроков |
| Батуты | `TrampolineListener` — прыжковые платформы |

---

### 💰 5. Экономика, RPG, Социум

| Система | Сервис/Команды | Описание |
|---------|----------------|----------|
| **Экономика** | `EconomyService` | Баланс, переводы, налог 2%, автосохранение |
| **Аукцион** | `AuctionService` | Торговля предметами через GUI, лимит 12 лотов |
| **Продажа** | `SellCommand` | Продажа ресурсов по прайс-листу из `config.yml` |
| **Контракты** | `ContractService` | Назначение награды за голову, комиссия 10% |
| **MMR / Рейтинг** | `MMRService` | Бронза → Серебро → Золото → Алмаз → Элита |
| **Топ игроков** | `LeaderboardService` | `/top` — таблица лидеров |
| **Гильдии** | `GuildService` | Создание кланов, роли, территории, банк, лимит 30 участников |
| **Семьи** | `MarryCommand` | Браки между игроками, `/marry`, `/unmarry` |
| **Таланты** | `TalentsGui` | Дерево пассивных навыков (`/talents`) |
| **Квесты** | `QuestManager` | Ежедневные задания и прогресс (`/quest`) |
| **Ежедневные награды** | `RewardsGui` | Дневные/недельные/месячные награды (`/rewards`) |
| **Статистика** | `StatsCommand` | Подробный профиль игрока (`/stats`) |

---

### 🌍 6. Мир, Локации, Телепортация

| Механика | Описание |
|----------|----------|
| `/spawn` | Телепортация на спавн |
| `/rtp` | Случайная телепортация в безопасное место |
| `/home`, `/sethome`, `/delhome`, `/homelist` | Управление личными домами |
| `/warp [название\|set\|remove]` | Телепортация на варпы (set/remove — для admins) |
| `/pvp` | Телепорт на PvP-арену |
| `/casino` | Телепорт в Казино |
| `/eventshop` | Телепорт в Ивент-шоп |
| `/afk` | Телепорт в AFK-зону |
| Высота мира | `WorldHeightService` — кастомные датапаки для изменения высоты мира |

---

### 🌐 7. Ивенты, Discord

| Компонент | Описание |
|-----------|----------|
| **Метеорит** | Раз в 4 часа, длится 20 минут |
| **Аирдроп** | Раз в 2.5 часа, длится 10 минут |
| **Босс** | Раз в 3 часа, длится 15 минут |
| **Торговец** | Раз в 6 часов, длится 10 минут |
| **Сокровища** | Раз в 2 часа, длится 12 минут |
| **Галеон** | Раз в 5 часов, длится 20 минут |
| **Кровавая Ночь** | Ручной запуск через `/admin event bloodnight` |
| **Discord-интеграция** | `DiscordBridge` — синхронизация чата, ролей, логов. Автовыдача ролей при `/link` |

---

### 🔒 8. Jail-система (Тюрьма)

| Функция | Описание |
|---------|----------|
| `/setjail <name> [radius]` | Создать тюрьму на текущей позиции с радиусом (по умолч. 5 блоков) |
| `/jail <player> <jail>` | Поместить игрока в тюрьму |
| `/unjail <player>` | Освободить игрока из тюрьмы |
| `/jailedplayers` | Список всех заключённых (онлайн/оффлайн) |
| `/deljail <name>` | Удалить тюрьму |
| **Radius-система** | При выходе за радиус — мгновенная ТП обратно |
| **Persistance** | При входе на сервер — заключённый автоматически телепортируется в тюрьму |
| **Хранение** | `jails.yml` (список тюрем), `jailed.yml` (список заключённых) |

---

### 🛠 9. Администрирование

| Команда | Описание |
|---------|----------|
| `/admin` | Панель управления (GUI) |
| `/admin reload` | Перезагрузка конфига |
| `/admin setcoins <player> <amount>` | Установить баланс игроку |
| `/admin setevent <player> <amount>` | Установить Event Points |
| `/admin give <player> <item> [amount]` | Выдать предмет игроку |
| `/admin me <player> <component> [amount]` | Выдать МЕ-компонент |
| `/admin giveblood <player>` | Выдать Флакон Крови |
| `/admin npc <1..8>` | Заспавнить NPC-торговца |
| `/admin event <type\|stop\|bloodnight>` | Управление ивентами |
| `/admin setspawn` | Установить спавн |
| `/admin awardblock` | Установить блок как точку RTP |
| `/invsee <player>` | Просмотр инвентаря игрока |
| `/freeze <player>` | Заморозить игрока |
| `/unfreeze <player>` | Разморозить игрока |
| `/vanish` / `/unvanish` | Невидимость администратора |
| `/prefix <player> <текст> <цвет>` | Установить кастомный префикс |
| `/unprefix <player>` | Удалить префикс |
| `/gm <0\|1\|2\|3> [player]` | Изменить гейммод |
| `/god [player]` | Режим бога |
| `/heal [player]` | Полное исцеление |
| `/feed [player]` | Полная сытость |
| `/fly [player]` | Toggle полёта |
| `/tpme <player>` | Телепортировать игрока к себе |
| `/ban <player> [reason]` | Забанить игрока |
| `/unban <player>` | Разбанить игрока |
| `/banip <player\|IP> [reason]` | Забанить по IP |
| `/kick <player> [reason]` | Кикнуть игрока |
| `/weather <clear\|rain\|thunder\|storm>` | Установить погоду |
| `/repair` | Починить предмет в руке |
| `/setrtpblock` | Отметить блок под ногами как точку RTP |
| `/homesreload` | Перезагрузка конфига домов |

---

## 📜 Полный список команд

### 🟢 Команды для всех игроков

| Команда | Алиасы | Описание |
|---------|--------|----------|
| `/menu` | — | Главное меню сервера |
| `/help` | `/?` | Список всех доступных команд |
| `/spawn` | — | Телепортация на спавн |
| `/rtp` | — | Случайная телепортация |
| `/balance` | `/bal` | Просмотр баланса |
| `/pay <player> <amount>` | — | Перевести монеты |
| `/sell` | — | Продать ресурсы из инвентаря |
| `/ah` | — | Аукцион предметов |
| `/link` | — | Привязать Discord-аккаунт |
| `/discord [player]` | — | Ссылка на Discord сервера |
| `/mmr` | — | Узнать свой рейтинг |
| `/top` | — | Топ игроков |
| `/stats [player]` | — | Статистика игрока |
| `/contract` | — | Управление контрактами на убийство |
| `/bounty` | `/bounties`, `/охота` | Меню заказов на голову |
| `/items` | — | Просмотр уникальных артефактов |
| `/blacksmith` | — | Кузница артефактов |
| `/marry <player>` | — | Предложение выйти замуж/жениться |
| `/unmarry` | — | Развод |
| `/guild <subcommand>` | `/g`, `/клан`, `/гильдия` | Управление гильдией |
| `/quest` | `/quests`, `/задания` | Текущие квесты |
| `/rewards` | `/reward`, `/daily`, `/календарь` | Ежедневные награды |
| `/talents` | `/talent`, `/skills`, `/навыки` | Дерево талантов |
| `/tutorial <classes\|quarry\|me>` | — | Обучение по механикам |
| `/pvp` | — | Телепорт на PvP-арену |
| `/casino` | — | Телепорт в Казино |
| `/eventshop` | — | Телепорт в Ивент-шоп |
| `/afk` | — | Телепорт в AFK-зону |
| `/duel <player>` | — | Вызов на дуэль |
| `/arena` | — | Войти на арену |
| `/leave` | — | Покинуть арену |
| `/prunus [invite\|join] [player]` | — | Войти в карманное измерение |
| `/malus` | — | Покинуть карманное измерение |
| `/sit` | — | Сесть на месте |
| `/lay` | — | Лечь на месте |
| `/dm <player> <text>` | — | Личное сообщение |
| `/home [name]` | — | Телепортация домой |
| `/sethome [name]` | — | Установить точку дома |
| `/delhome [name]` | — | Удалить точку дома |
| `/homelist` | — | Список домов |
| `/warp [name]` | — | Телепортация на варп / просмотр варпов |

### 🔴 Команды для администраторов (`astrasmp.admin`)

| Команда | Описание |
|---------|----------|
| `/admin [subcommand]` | Панель управления сервером |
| `/invsee <player>` | Просмотр инвентаря |
| `/freeze <player>` | Заморозить игрока |
| `/unfreeze <player>` | Разморозить игрока |
| `/prefix <player> <text> <color>` | Кастомный префикс |
| `/unprefix <player>` | Удалить префикс |
| `/setrtpblock` | Отметить блок как точку RTP |
| `/gm <0\|1\|2\|3> [player]` | Изменить гейммод |
| `/god [player]` | Режим бога |
| `/heal [player]` | Исцелить игрока |
| `/feed [player]` | Насытить игрока |
| `/vanish` | Стать невидимым |
| `/unvanish` | Стать видимым |
| `/warp set <name>` | Установить варп |
| `/warp remove <name>` | Удалить варп |
| `/homesreload` | Перезагрузка домов |
| `/ban <player> [reason]` | Забанить |
| `/unban <player>` | Разбанить |
| `/banip <player\|IP> [reason]` | Бан по IP |
| `/kick <player> [reason]` | Кикнуть |
| `/weather <clear\|rain\|thunder\|storm>` | Установить погоду |
| `/repair` | Починить предмет в руке |
| `/fly [player]` | Включить/выключить полёт |
| `/tpme <player>` | Телепортировать игрока к себе |
| `/jail <player> <jailname>` | Посадить в тюрьму |
| `/unjail <player>` | Освободить из тюрьмы |
| `/setjail <name> [radius]` | Создать тюрьму |
| `/deljail <name>` | Удалить тюрьму |
| `/jailedplayers` | Список заключённых |

---

## 🔑 Права (Permissions)

| Права | По умолчанию | Описание |
|-------|-------------|----------|
| `astrasmp.admin` | OP | Полный доступ ко всем административным командам |

> Все обычные команды доступны без специальных прав.

---

## ⚙️ Конфигурация

### `config.yml`

```yaml
server:
  name: ChetCraft           # Название сервера
  currency-name: Coins       # Название валюты
  tax-rate: 0.02             # Налог на переводы (2%)
  auction-limit-per-player: 12
  sell-cooldown-seconds: 2
  autosave-seconds: 120      # Интервал автосохранения
  event-radius: 1000         # Радиус спавна ивентов
  world-name: world

storage:
  mode: mysql
  mysql:
    host: ...
    port: ...
    database: ...
    pool-size: 8

economy:
  prices:
    COBBLESTONE: 0.5
    DIAMOND: 100.0
    # ... (полный прайс-лист в config.yml)

events:
  meteor:    { interval-minutes: 240, duration-minutes: 20 }
  airdrop:   { interval-minutes: 150, duration-minutes: 10 }
  boss:      { interval-minutes: 180, duration-minutes: 15 }
  merchant:  { interval-minutes: 360, duration-minutes: 10 }
  treasure:  { interval-minutes: 120, duration-minutes: 12 }
  galleon:   { interval-minutes: 300, duration-minutes: 20 }

mmr:
  start: 50
  bronze: 0 | silver: 1100 | gold: 1300 | diamond: 1600 | elite: 2000

contracts:
  bounty-fee: 0.10     # Комиссия за размещение
  cancel-fee: 0.15     # Комиссия за отмену
  max-active-per-player: 5

rewards:
  base-coins: 500          # Дневная награда
  weekly-coins: 10000      # Недельная
  monthly-coins: 50000     # Месячная

guilds:
  default-ranks: [leader, officer, member, recruit]
  # Лимит участников: 30
```

### `plugin.yml` — зависимости

```yaml
softdepend: [PlaceholderAPI, CustomWorldHeight, SitAndLay]
```

### Файлы Jail-системы (создаются автоматически)

| Файл | Содержимое |
|------|-----------|
| `plugins/AstraSMP/jails.yml` | Список тюрем с координатами и радиусом |
| `plugins/AstraSMP/jailed.yml` | Список заключённых с привязкой к тюрьме |

---

## 🗄️ База данных

Схема: `src/main/resources/sql/schema.sql`

Хранит:
- Профили игроков (баланс, MMR, Event Points, кастомный префикс)
- Гильдии и территориальные клеймы
- Аукционные лоты
- Контракты / баунти
- UUID → ник маппинг

---

## 🚀 Сборка и деплой

```bash
# Клонирование
git clone https://github.com/ChetTeam/AstraSMP.git
cd AstraSMP

# Сборка (Windows)
gradlew clean build

# Сборка + деплой на удалённый сервер (требует local.properties)
gradlew build
```

Готовый `.jar` — в `build/libs/`. При наличии `local.properties` с SSH-данными Gradle автоматически деплоит на сервер через задачу `:deploy`.

### `local.properties` (не коммитить в git!)
```properties
deploy.host=your.server.host
deploy.user=username
deploy.path=/path/to/plugins/
deploy.key=/path/to/ssh_key
```

---

## 📁 Структура проекта

```
src/main/java/com/astrasmp/
├── AstraSMPPlugin.java          # Точка входа, регистрация команд
├── blood/                        # Кровавые механики
├── casino/                       # Казино (Blackjack, Roulette, Slots)
├── commands/                     # Все команды (~50 файлов)
├── config/                       # ConfigManager
├── database/                     # DatabaseService (MySQL/HikariCP)
├── discord/                      # DiscordBridge
├── gui/                          # GUI интерфейсы
├── items/                        # ItemRegistry — кастомные предметы
├── listener/                     # Обработчики событий
├── model/                        # Модели данных (Guild, PlayerProfile и др.)
├── pacts/                        # Система пактов
├── rift/                         # Разломы
├── rituals/                      # Ритуалы
├── service/                      # Бизнес-логика (~18 сервисов)
└── util/                         # Утилиты (TextUtil, LocationKey и др.)
```

---

## 🔗 Ссылки

- **Discord:** [discord.gg/cheterin](https://discord.gg/cheterin)
- **Автор:** CheterinTeam
