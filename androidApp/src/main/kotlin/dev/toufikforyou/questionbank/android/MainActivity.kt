package dev.toufikforyou.questionbank.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.toufikforyou.questionbank.android.ui.navigation.QuestionBankNavHost
import dev.toufikforyou.questionbank.android.ui.theme.QuestionBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuestionBankApplication).container
        setContent {
            QuestionBankTheme {
                QuestionBankNavHost(container)
            }
        }
    }
}
