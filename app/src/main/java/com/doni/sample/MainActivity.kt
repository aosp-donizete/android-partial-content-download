package com.doni.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.doni.sample.splitter.SplitterManager
import com.doni.sample.ui.theme.SplitDownloadSampleTheme
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.pathString

@OptIn(ExperimentalStdlibApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitDownloadSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        lifecycleScope.launch {
            val tmp = kotlin.io.path.createTempFile()

            SplitterManager.get(
                "http://10.0.2.2:8000/download/oldsmobile.zip",
                tmp.pathString,
                parts = 10,
                retry = 4
            )

            val messageDigest = MessageDigest.getInstance("SHA256")
            tmp.inputStream().use { input ->
                val byteArray = ByteArray(1024)
                var read = input.read(byteArray)
                while (read > 0) {
                    messageDigest.update(byteArray, 0, read)
                    read = input.read(byteArray)
                }
            }
            println("Digest: ${messageDigest.digest().toHexString()}")

            tmp.deleteIfExists()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SplitDownloadSampleTheme {
        Greeting("Android")
    }
}