package sg.edu.nus.iss.client.backend

object BackendConfig {
    // Change USE_BACKEND to true once the backend server is ready
    // Code in the app automatically follows this  setting
    const val USE_BACKEND = true

    // FastAPI chatbot bridge endpoint (Android emulator loopback).
    // Spring Boot remains on port 8000; FastAPI should run on 8001.
    const val BASE_URL = "http://10.0.2.2:8001/"
}