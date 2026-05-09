package com.opus.readerparser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.opus.readerparser.ui.navigation.AppNavGraph
import com.opus.readerparser.ui.theme.ReaderParserTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReaderParserTheme {
                AppNavGraph()
            }
        }
    }
}
