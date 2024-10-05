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
import com.doni.sample.splitter.OkHttpHandler
import com.doni.sample.splitter.RangeDownloadManager
import com.doni.sample.ui.theme.SplitDownloadSampleTheme
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.time.measureTimedValue

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
            val rangeDownloadManager = RangeDownloadManager(
                OkHttpHandler,
                parts = 100,
                retry = 3,
                parallelism = 100
            )

            val (output, time) = measureTimedValue {
                rangeDownloadManager.download(
                    "http://10.0.2.2:8000/download/oldsmobile.zip"
                )
            }

            println("Took ${time.inWholeSeconds} seconds")

            val messageDigest = MessageDigest.getInstance("SHA256")
            output.inputStream().use { input ->
                val byteArray = ByteArray(1024)
                var read = input.read(byteArray)
                while (read > 0) {
                    messageDigest.update(byteArray, 0, read)
                    read = input.read(byteArray)
                }
            }
            println("Digest: ${messageDigest.digest().toHexString()}")

            output.deleteIfExists()
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