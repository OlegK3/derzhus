# Выпуск релиза «Новая жизнь»

## 1. Один раз: создать ключ подписи
Ключ создаётся один раз и хранится вечно. Потеряете его, и обновить приложение у пользователей будет нельзя.
Сохраните файл и пароли минимум в двух местах (не в репозитории).

Windows, PowerShell (keytool входит в Android Studio):

    & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore derzhus-release.jks -alias derzhus -keyalg RSA -keysize 2048 -validity 10000

Запросит пароль хранилища и данные владельца (можно ввести любые). Пароль ключа сделайте таким же.

Превратить файл в текст и скопировать в буфер:

    [Convert]::ToBase64String([IO.File]::ReadAllBytes("derzhus-release.jks")) | Set-Clipboard

## 2. Один раз: секреты в GitHub
Репозиторий -> Settings -> Secrets and variables -> Actions -> New repository secret. Создайте 4 секрета:

| Имя | Значение |
|---|---|
| KEYSTORE_BASE64 | текст из буфера (шаг выше) |
| KEYSTORE_PASSWORD | пароль хранилища |
| KEY_ALIAS | derzhus |
| KEY_PASSWORD | пароль ключа |

## 3. Запуск сборки
Вкладка Actions -> Release -> Run workflow (укажите версию, например 1.0.0).
Готовые файлы: страница запуска -> Artifacts -> NovayaZhizn-release (APK и AAB).
Либо создайте тег v1.0.0: файлы появятся в разделе Releases.

## Примечания
- После добавления секретов обычные отладочные сборки (Build APK) тоже подписываются этим ключом,
  и новая версия ставится поверх прежней. Один раз удалите приложение, установленное со старой подписью.
- versionCode растёт автоматически с каждым запуском.
- RuStore принимает APK, Google Play требует AAB.
