package vn.edu.qnu.st.nambede

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.edu.qnu.st.nambede.ui.NamBeDeApp
import vn.edu.qnu.st.nambede.ui.theme.NamBeDeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NamBeDeApplication).container
        setContent {
            NamBeDeTheme {
                NamBeDeApp(container)
            }
        }
    }
}
