# VolgaVPN Android 1.0

Бесплатный Android-клиент для собственного WireGuard VPN.

## Уже работает
- настоящий WireGuard userspace backend;
- Android VPN permission flow;
- импорт `.conf` с телефона;
- вставка конфигурации вручную;
- локальное сохранение конфигурации;
- подключение / отключение;
- проверка внешнего IP через `api.ipify.org`;
- тёмный интерфейс;
- GitHub Actions для сборки debug APK.

WireGuard публикует embeddable tunnel library в Maven Central; проект использует `com.wireguard.android:tunnel:1.0.20260102`. citeturn1search0

## Бесплатный сервер

Сам клиент не требует платной лицензии или VPN-провайдера. Для собственного сервера можно использовать Oracle Cloud Always Free. Oracle указывает Always Free Compute как ресурс без ограничения срока; актуальный лимит Ampere A1 для Always Free tenancy — 2 OCPU и 12 GB RAM. citeturn0search0turn0search1

У бесплатного облака есть ограничения: в некоторых регионах может не хватать свободной мощности, а при регистрации Oracle может потребовать банковскую карту для проверки личности. citeturn0search0turn0search1

## Быстрый запуск сервера

1. Создай Oracle Cloud Free Tier аккаунт.
2. Создай Ubuntu ARM VM в Home Region.
3. Используй не более 2 OCPU / 12 GB RAM для Always Free.
4. Разреши UDP `51820` в Cloud Firewall/Security List.
5. Скопируй `server/install-wireguard.sh` на сервер.
6. Выполни `sudo bash install-wireguard.sh`.
7. Скрипт создаст `/root/volgavpn-client.conf`.
8. Перенеси этот файл на телефон и импортируй его в VolgaVPN.

## Безопасность

Приватные ключи генерируются на сервере и не должны попадать в GitHub. В репозитории нет рабочих VPN-ключей.

## Архитектура

`Android UI → WireGuard Go userspace backend → Android VpnService → твой WireGuard VPS → Internet`

Это уже не демонстрационный переключатель: для реального подключения нужен только валидный `.conf` от твоего сервера.
