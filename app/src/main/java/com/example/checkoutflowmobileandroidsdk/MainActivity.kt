package com.example.checkoutflowmobileandroidsdk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import kotlinx.coroutines.launch
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.model.ComponentName
import com.checkout.components.interfaces.model.PaymentSessionResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create the SDK Configuration
        // NOTE: Placeholder values below must be replaced with real data from your server
        val configuration = CheckoutComponentConfiguration(
            context = this,
            publicKey = "pk_sbox_wzfdx5reewvfgcdgyky4pdgbemc", // Found in Checkout.com Dashboard
            environment = Environment.SANDBOX,
            paymentSession = PaymentSessionResponse(
                id = "ps_380RZxity4YL7wdUgnqheBKWJST",           // Unique ID from your server
                paymentSessionToken = "YmFzZTY0:eyJpZCI6InBzXzM4MFJaeGl0eTRZTDd3ZFVnbnFoZUJLV0pTVCIsImVudGl0eV9pZCI6ImVudF9wbngzem5vdW5pcGVyZGZiZWZqa3ViamdvdSIsImV4cGVyaW1lbnRzIjp7fSwicHJvY2Vzc2luZ19jaGFubmVsX2lkIjoicGNfaTd1M2hsaWgybnplN21xNmRpZ3YzcGVtdXEiLCJhbW91bnQiOjEwMCwibG9jYWxlIjoiZW4tR0IiLCJjdXJyZW5jeSI6IlVTRCIsInBheW1lbnRfbWV0aG9kcyI6W3sidHlwZSI6InJlbWVtYmVyX21lIiwiY2FyZF9zY2hlbWVzIjpbIlZpc2EiLCJNYXN0ZXJjYXJkIiwiQW1leCIsIkpDQiIsIkRpbmVycyBDbHViIiwiRGlzY292ZXIiLCJDaGluYSBVbmlvbiBQYXkiXSwiYmlsbGluZ19hZGRyZXNzIjp7ImNvdW50cnkiOiJVUyJ9LCJkaXNwbGF5X21vZGUiOiJjaGVja2JveCJ9LHsidHlwZSI6ImNhcmQiLCJjYXJkX3NjaGVtZXMiOlsiVmlzYSIsIk1hc3RlcmNhcmQiLCJBbWV4IiwiSkNCIiwiRGluZXJzIENsdWIiLCJEaXNjb3ZlciIsIkNoaW5hIFVuaW9uIFBheSJdLCJzY2hlbWVfY2hvaWNlX2VuYWJsZWQiOmZhbHNlLCJzdG9yZV9wYXltZW50X2RldGFpbHMiOiJkaXNhYmxlZCIsImJpbGxpbmdfYWRkcmVzcyI6eyJjb3VudHJ5IjoiVVMifX0seyJ0eXBlIjoiYXBwbGVwYXkiLCJkaXNwbGF5X25hbWUiOiJKZWZmJ3MgVGVzdCBBY2NvdW50IiwiY291bnRyeV9jb2RlIjoiVVMiLCJjdXJyZW5jeV9jb2RlIjoiVVNEIiwibWVyY2hhbnRfY2FwYWJpbGl0aWVzIjpbInN1cHBvcnRzM0RTIl0sInN1cHBvcnRlZF9uZXR3b3JrcyI6WyJ2aXNhIiwibWFzdGVyQ2FyZCIsImFtZXgiLCJqY2IiLCJkaXNjb3ZlciIsImNoaW5hVW5pb25QYXkiXSwidG90YWwiOnsibGFiZWwiOiJKZWZmJ3MgVGVzdCBBY2NvdW50IiwidHlwZSI6ImZpbmFsIiwiYW1vdW50IjoiMSJ9fSx7InR5cGUiOiJnb29nbGVwYXkiLCJtZXJjaGFudCI6eyJpZCI6IjA4MTEzMDg5Mzg2MjY4ODQ5OTgyIiwibmFtZSI6IkplZmYncyBUZXN0IEFjY291bnQiLCJvcmlnaW4iOiJodHRwczovL2Nrby5qZWZmOTQwNDAuY29tIn0sInRyYW5zYWN0aW9uX2luZm8iOnsidG90YWxfcHJpY2Vfc3RhdHVzIjoiRklOQUwiLCJ0b3RhbF9wcmljZSI6IjEiLCJjb3VudHJ5X2NvZGUiOiJVUyIsImN1cnJlbmN5X2NvZGUiOiJVU0QifSwiY2FyZF9wYXJhbWV0ZXJzIjp7ImFsbG93ZWRfYXV0aF9tZXRob2RzIjpbIlBBTl9PTkxZIiwiQ1JZUFRPR1JBTV8zRFMiXSwiYWxsb3dlZF9jYXJkX25ldHdvcmtzIjpbIlZJU0EiLCJNQVNURVJDQVJEIiwiQU1FWCIsIkpDQiIsIkRJU0NPVkVSIl19fV0sImZlYXR1cmVfZmxhZ3MiOlsiYW5hbHl0aWNzX29ic2VydmFiaWxpdHlfZW5hYmxlZCIsImdldF93aXRoX3B1YmxpY19rZXlfZW5hYmxlZCIsImxvZ3Nfb2JzZXJ2YWJpbGl0eV9lbmFibGVkIiwicmlza19qc19lbmFibGVkIiwidXNlX2JpbGxpbmdfYWRkcmVzc19mcm9tX2NvbmZpZ19mb3JfdG9rZW5pemF0aW9uIiwidXNlX2RldmljZV9hcGlfZm9yX29ic2VydmFiaWxpdHkiLCJ1c2Vfcmlza2pzX3YyIiwidXNlX3VybF9oYXNoX2Zvcl9pZnJhbWVfcHJvcHMiXSwicmlzayI6eyJlbmFibGVkIjpmYWxzZX0sIm1lcmNoYW50X25hbWUiOiJKZWZmJ3MgVGVzdCBBY2NvdW50IiwicGF5bWVudF9zZXNzaW9uX3NlY3JldCI6InBzc19lZDZmYjA4NC0wZjQ0LTRjYjAtOTAyNy04MWUxNzFiMDY1YzYiLCJpbnRlZ3JhdGlvbl9kb21haW4iOiJkZXZpY2VzLmFwaS5zYW5kYm94LmNoZWNrb3V0LmNvbSJ9",
                paymentSessionSecret = "pss_ed6fb084-0f44-4cb0-9027-81e171b065c6"
            ),
            componentCallback = ComponentCallback(
                onSuccess = { component, paymentId ->
                    Log.d("CheckoutFlow", "Payment Successful: $paymentId")
                    // TODO: Notify your server to finalize the order
                },
                onError = { component, error ->
                    Log.e("CheckoutFlow", "Payment Error: ${error.message}")
                }
            )
        )

        // 2. Initialize and display the Flow Component
        lifecycleScope.launch {
            try {
                // Instantiate the factory
                val checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

                // Create the "Flow" component which manages all payment methods
                val flow = checkoutComponents.create(ComponentName.Flow)

                setContent {
                    MaterialTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // 3. Render the UI directly in Compose
                            flow.Render()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutFlow", "Initialization failed", e)
            }
        }
    }
}
