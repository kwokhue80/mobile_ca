package sg.edu.nus.iss.client.backend

import sg.edu.nus.iss.client.BuildConfig

object BackendConfig {
    // Change USE_BACKEND to true once the backend server is ready
    // Code in the app automatically follows this  setting
    const val USE_BACKEND = true

    // Override WELLNESS_AGENT_BASE_URL in client/local.properties when using a physical device
    // or a non-default host/port. Emulator default remains 10.0.2.2:8001.
    const val BASE_URL = BuildConfig.WELLNESS_AGENT_BASE_URL
}