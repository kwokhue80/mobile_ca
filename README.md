# MobileCA

GDipSA 62 Mobile App Group 3 project.

## Overview

MobileCA is an AI-enabled wellness application with:

- A Kotlin Android client application
- A Java Spring Boot backend service
- MySQL persistence for user and wellness data
- AI-assisted recommendation and chatbot capabilities

The app supports user authentication, daily wellness logging, dashboards, profile management, goals, activity history, chatbot interactions, and recommendation notifications.

## Repository Structure

- [client](client): Android mobile app (Kotlin, XML, Navigation Component, Retrofit, WorkManager)
- [server](server): Spring Boot backend (REST API, JWT security, JPA, MySQL)
- [objectbox-generator](objectbox-generator): Supporting utilities and data tooling for ObjectBox/RAG workflows

## Main Features

- User registration, login, and logout with JWT-based backend authentication
- Wellness record capture
	Sleep, food, hydration, exercise, mood, weight
- Dashboard views
	Daily summary and activity feed, plus date-range metrics
- User profile
	View and update profile fields
- User goals
	Read and upsert wellness goals
- Chatbot tab in app home
- Recommendations
	Latest recommendation API, in-app unread badge, notification history screen
- Background recommendation polling with Android WorkManager
- System notifications for new recommendations (with Android 13+ runtime permission handling)

## Tech Stack

### Client (Android)

- Kotlin + Android SDK
- AndroidX Navigation
- Retrofit + OkHttp + Gson
- Coroutines
- WorkManager
- ObjectBox
- ONNX Runtime
- MPAndroidChart

### Server (Backend)

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL connector
- JWT (jjwt)
- Spring AI dependencies (OpenAI/OpenRouter and Chroma-related libraries)
- Lombok

## API Summary

The backend currently exposes these main REST routes:

- Auth
	- POST /api/auth/register
	- POST /api/auth/login
	- POST /api/auth/logout
- Dashboard
	- GET /api/dashboard/daily
	- GET /api/dashboard/range
- Wellness
	- POST /api/wellness/records
	- GET /api/wellness/activity
	- GET /api/wellness/recommendations/latest
- User Profile
	- GET /api/user-profile
	- PUT /api/user-profile
- User Goals
	- GET /api/user-goals
	- PUT /api/user-goals/{goalType}

See implementations under [server/src/main/java/sg/edu/nus/features](server/src/main/java/sg/edu/nus/features).

## Prerequisites

- JDK 21 for Android build and tooling
- JDK 17 for server runtime compatibility
- Android Studio or Android SDK + emulator
- MySQL running locally
- Maven Wrapper (already included)
- Gradle Wrapper (already included)

## Configuration

### Server Configuration

File: [server/src/main/resources/application.properties](server/src/main/resources/application.properties)

Important settings:

- Server port: 8000
- Chatbot bridge (FastAPI) port: 8001
- MySQL datasource: wellness_db
- JWT secret: JWT_SECRET environment variable
- OpenRouter API key: OPENROUTER_API_KEY environment variable

### Client Configuration

- Base URL is configured in [client/app/src/main/java/sg/edu/nus/iss/client/network/RetrofitClient.kt](client/app/src/main/java/sg/edu/nus/iss/client/network/RetrofitClient.kt)
	Default emulator URL is http://10.0.2.2:8000/
- Optional local properties key used by client build:
	OPENROUTER_API_KEY in [client/app/build.gradle.kts](client/app/build.gradle.kts)

## Running the Project

### 1) Start MySQL

Ensure a local MySQL instance is running and accessible.

### 2) Start Backend Server

From [server](server):

Windows:
	.\mvnw.cmd spring-boot:run

macOS/Linux:
	./mvnw spring-boot:run

Backend will start on port 8000 by default.

### 2a) Start Chatbot Bridge (FastAPI)

From [server/mcp_server](server/mcp_server):

Windows:
	C:/Python314/python.exe -m uvicorn mcp_agent_wellness:app --app-dir "c:\Users\ameli\Documents\GDipSA\CAs\MobileCA\server\mcp_server" --host 0.0.0.0 --port 8001

macOS/Linux:
	python -m uvicorn mcp_agent_wellness:app --app-dir ./server/mcp_server --host 0.0.0.0 --port 8001

Chatbot requests from Android are routed to the bridge on port 8001, while the bridge calls Spring endpoints on port 8000.

### 2b) Start Both Services From VS Code

You can start backend + chatbot bridge together using the configured VS Code task:

- Run Task: Start Chat Stack

You can start backend + chatbot bridge + Android client install/launch together using:

- Run Task: Start Full Stack

If no Android emulator/device is connected, the Android install/launch steps are skipped automatically and the backend/chat services still start.

You can stop backend + chatbot bridge together using:

- Run Task: Stop Chat Stack

You can stop backend + chatbot bridge + Android client app using:

- Run Task: Stop Full Stack

Task configuration is in [.vscode/tasks.json](.vscode/tasks.json).

### 3) Start Android Client

From [client](client):

Windows:
	.\gradlew.bat assembleDebug

macOS/Linux:
	./gradlew assembleDebug

Then run from Android Studio on emulator/device.

## Seed Data and Test Accounts

Initial SQL seed data is in [server/src/main/resources/data.sql](server/src/main/resources/data.sql).

A documented sample test password is noted there as:

- Password@123

Example seed users are also listed in the same file.

## Recommendation Notifications Flow

- Server generates latest recommendation at /api/wellness/recommendations/latest
- Client polls in foreground from Home
- Client also polls in background using WorkManager
- New recommendation increments unread counter
- Notification icon opens recommendation history screen
- Opening history marks unread count as viewed

Relevant client files:

- [client/app/src/main/java/sg/edu/nus/iss/client/dashboard/HomeFragment.kt](client/app/src/main/java/sg/edu/nus/iss/client/dashboard/HomeFragment.kt)
- [client/app/src/main/java/sg/edu/nus/iss/client/dashboard/RecommendationPollWorker.kt](client/app/src/main/java/sg/edu/nus/iss/client/dashboard/RecommendationPollWorker.kt)
- [client/app/src/main/java/sg/edu/nus/iss/client/dashboard/RecommendationHistoryFragment.kt](client/app/src/main/java/sg/edu/nus/iss/client/dashboard/RecommendationHistoryFragment.kt)
- [client/app/src/main/java/sg/edu/nus/iss/client/util/SessionManager.kt](client/app/src/main/java/sg/edu/nus/iss/client/util/SessionManager.kt)

## Notes

- This repository includes additional experimental/support assets (for example under [objectbox-generator](objectbox-generator) and [server/src/RAG](server/src/RAG)).
- The primary production path is the Android client in [client](client) with the Spring backend in [server](server).