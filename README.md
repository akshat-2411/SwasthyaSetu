<div align="center">
  
  # SwasthyaSetu
  
  **Bridging the Healthcare Gap via AI-Powered Intervention**
  
</div>

---

## 📖 Project Overview

**SwasthyaSetu** is a dedicated Android healthcare application designed to democratize access to core medical triage and assistance, particularly across rural populations. By leveraging the advanced capabilities of generative AI alongside real-time location mapping, the app guides users smoothly from initial symptom detection to professional clinical care. Our overarching mission is to construct a digital bridge that ensures no remote demographic is left paralyzed by an unexpected health crisis.

---

## ⚠️ Problem Statement

Access to rapid, reliable healthcare remains fundamentally fractured across many regions due to several systemic barriers:
* **Information Asymmetry:** Users often lack access to verified medical triage procedures and unknowingly rely on predatory or incorrect word-of-mouth diagnosis.
* **Delayed Response:** The critical "Golden Hour" of medical emergencies is frequently lost merely trying to locate working medical facilities or emergency contacts.
* **Healthcare Literacy Gap:** A heavy language barrier prevents non-English-speaking rural populations from properly interacting with modern diagnostic tech or understanding symptom severity.

---

## ✨ Key Features

* **🤖 AI Symptom Checker:** Harnesses the Google Gemini API to analyze reported symptoms, returning immediate health insights and generating intelligent risk probability assessments.
* **📍 Hospital Locator:** Utilizes real-time Google Maps and GPS mapping to instantly isolate, display, and navigate patients to the nearest available medical facilities.
* **🆘 One-Tap SOS:** An accessible pan-app safety net that instantly dispatches emergency SMS alerts containing precise location sharing to designated emergency contacts.
* **📅 Vaccination Tracker:** Allows users to build a personalized timeline of their medical history, logging past immunizations and forecasting their upcoming vaccination schedule.
* **🌍 Multilingual Engine:** Features robust native localization supporting seamless toggling between Hindi and English dynamically across the entire app architecture.

---

## 🛠️ Tech Stack

| Category         | Technology Used |
|:-----------------|:----------------|
| **Language**     | Kotlin |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Frontend**     | XML / Modern Material Design 3 |
| **Backend**      | Google Firebase (Auth, Firestore, FCM) |
| **AI Engine**    | Google Gemini API |

---

## 🏛️ Architecture
SwasthyaSetu was built adopting the robust **MVVM (Model-View-ViewModel)** architectural pattern. MVVM strictly separates our complex frontend UI manipulation from our backend data transactions:
1. **Views (XML/Activities):** Subscribe entirely to the ViewModels, dynamically updating purely when the underlying data state shifts (e.g., LiveData or StateFlow emitters).
2. **ViewModels:** Contain all pure business logic—manipulating raw AI queries, aggregating Firebase data, and preparing the state for the UI, ensuring memory-leak-safe lifecycles.
3. **Repository/Model:** Orchestrates raw data fetches seamlessly from the cloud (Firebase/Google AI) keeping the ViewModel unaware of network latency mechanics.

---

## 📂 Project Structure

```text
SwasthyaSetu
├── app/src/main
│   ├── java/com/swasthyasetu/app
│   │   ├── database/       # Room database classes & DAOs
│   │   ├── model/          # Data classes, Entities, and Firebase schema
│   │   ├── receiver/       # Broadcast receivers (e.g., alarms, boot handlers)
│   │   ├── repository/     # Data aggregation (Firebase, Gemini API, Room)
│   │   ├── service/        # Background services (FCM, notifications)
│   │   ├── util/           # Helper classes, constant values, location utils
│   │   ├── view/           # UI Layer: Activities, Dialogs, BottomSheets, Adapters
│   │   └── viewmodel/      # MVVM ViewModels orchestrating UI state and business logic
│   └── res/
│       ├── drawable/       # Vector assets, custom illustrations, and icons
│       ├── layout/         # XML layouts for screens and list items
│       ├── values/         # English localized strings, colors, dimensions, themes
│       └── values-hi/      # Hindi localized string resources
```

---

## 🚀 Installation Guide

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/YourUsername/SwasthyaSetu.git
   ```
2. **Open in IDE:**
   Open the `/SwasthyaSetu` project folder directly in **Android Studio**.
3. **Connect Firebase:**
   Create a project in the Firebase Console, register your package bundle, and download the `google-services.json` file. Place this file inside your `/app/` directory.
4. **Set API Keys:**
   Set up your cloud backend (e.g., REST API hosted on AWS / GCP / Azure). Store your API base URL and key securely in local.properties or environment variables:
   ```properties
     CLOUD_API_BASE_URL=https://your-api-endpoint.com/
    CLOUD_API_KEY=your_api_key_here
   ```
5. **Build and Run:**
   Sync Gradle and deploy directly to an emulator or physical device.

---

## 📱 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/921911df-f0dd-4dbc-a00b-9cb69caf51dd" width="200" alt="Login">
  <img src="https://github.com/user-attachments/assets/1518ca6b-b0a6-4d23-ad31-a468bcd368be" width="200" alt="Dashboard">
  <img src="https://github.com/user-attachments/assets/f870ff04-9810-4de0-ae8c-4ae60b0ebe5d" width="200" alt="Profile">
  <img src="https://github.com/user-attachments/assets/f1af08ad-0113-4a96-a16d-d08d17b81bff" width="200" alt="Chatbot">
  <img src="https://github.com/user-attachments/assets/29d4fd01-6685-4312-8b65-c4257307bf1f" width="200" alt="Find Hospital">
  <img src="https://github.com/user-attachments/assets/d041bb63-ce9d-4401-b205-69e1b7bcb48b" width="200" alt="First Aid Guide">
  <img src="https://github.com/user-attachments/assets/a07be79e-69ca-4125-9080-7ffc6e18f939" width="200" alt="Symptom Meter">
  <img src="https://github.com/user-attachments/assets/ab8ad238-8b59-49ac-ab08-95fadf931abe" width="200" alt="Pill Timers">
</p>

---

## 📬 Contact

Developed by **Akshat Sharma & Akshay Verma**
