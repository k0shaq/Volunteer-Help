# VolunteerHelp

VolunteerHelp — Android MVP застосунок для координації волонтерської допомоги на Kotlin + Jetpack Compose + Firebase.

## Що використано

- Kotlin
- Jetpack Compose
- MVVM
- Firebase Authentication
- Cloud Firestore
- Cloudinary unsigned upload
- Coil
- Navigation Compose
- Coroutines та Flow

## Налаштування Firebase

1. Створіть проєкт у Firebase Console.
2. Додайте Android-застосунок з package name `com.example.volunteerhelp`.
3. Завантажте файл `google-services.json`.
4. Покладіть `google-services.json` у каталог `app/`.
5. У Firebase Console відкрийте `Authentication`.
6. Увімкніть провайдер `Email/Password`.
7. У Firebase Console відкрийте `Firestore Database`.
8. Створіть базу даних у режимі `Production` або `Test` залежно від етапу розробки.
9. Створіть колекції `users`, `campaigns`, `helpRequests`, `reports` під час першого запуску або дайте застосунку створити їх автоматично.

## Налаштування Cloudinary

1. Створіть безкоштовний акаунт у Cloudinary.
2. Відкрийте `Settings` -> `Upload`.
3. Створіть unsigned upload preset.
4. Вкажіть назву preset у константі `CLOUDINARY_UPLOAD_PRESET`.
5. Вкажіть cloud name у константі `CLOUDINARY_CLOUD_NAME`.

Файл з константами:

- `app/src/main/java/com/example/volunteerhelp/util/Constants.kt`

Поточні значення:

- `CLOUDINARY_CLOUD_NAME = "dlabl1umr"`
- `CLOUDINARY_UPLOAD_PRESET = "volunteer_unsigned"`

## Запуск

1. Переконайтесь, що `google-services.json` лежить у `app/`.
2. Синхронізуйте Gradle.
3. Запустіть `assembleDebug` або відкрийте проєкт в Android Studio та натисніть Run.

## Firestore структура

### users

- `id: String`
- `name: String`
- `email: String`
- `role: String`
- `avatarUrl: String?`
- `rating: Int`
- `isVerified: Boolean`
- `createdAt: Long`

### campaigns

- `id: String`
- `title: String`
- `description: String`
- `type: String`
- `targetAmount: Double`
- `currentAmount: Double`
- `city: String`
- `region: String`
- `imageUrl: String?`
- `requisites: String`
- `volunteerId: String`
- `volunteerName: String`
- `status: String`
- `createdAt: Long`

### helpRequests

- `id: String`
- `campaignId: String`
- `campaignTitle: String`
- `donorId: String`
- `donorName: String`
- `volunteerId: String`
- `type: String`
- `amount: Double`
- `comment: String`
- `screenshotUrl: String?`
- `status: String`
- `createdAt: Long`

### reports

- `id: String`
- `campaignId: String`
- `volunteerId: String`
- `description: String`
- `imageUrl: String?`
- `createdAt: Long`

## Примітка щодо безпеки

Для реального деплою потрібно додати Firestore Security Rules, які перевіряють роль користувача та доступ до власних документів.
