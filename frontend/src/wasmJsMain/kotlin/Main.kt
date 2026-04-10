import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.example.rag.ui.screen.ConversationsScreen
import com.example.rag.ui.screen.LoginScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        MaterialTheme {
            var token by remember { mutableStateOf<String?>(null) }
            
            if (token == null) {
                LoginScreen(onLoginSuccess = { newToken ->
                    token = newToken
                })
            } else {
                ConversationsScreen(
                    token = token!!,
                    onConversationClick = { id ->
                        println("Opening conversation $id")
                        // TODO: Navigate to ChatScreen
                    },
                    onLogoutClick = {
                        token = null
                    }
                )
            }
        }
    }
}
