package dev.toufikforyou.questionbank.android

import android.app.Application
import dev.toufikforyou.questionbank.di.AppContainer
import dev.toufikforyou.questionbank.platform.createAndroidAppContainer

class QuestionBankApplication : Application() {
    val container: AppContainer by lazy { createAndroidAppContainer(this) }
}
