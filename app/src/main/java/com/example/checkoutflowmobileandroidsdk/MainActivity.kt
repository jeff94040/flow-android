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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// --- MINIMAL ADDITION 1: Append new imports (DO NOT touch existing ones) ---
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.wallet.wrapper.GooglePayFlowCoordinator

// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    // --- MINIMAL ADDITION 2: A simple callback trick to avoid importing the broken CheckoutComponents class
    private var checkoutResultHandler: ((Int, String) -> Unit)? = null
    // ----------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Initialize and display the Flow Component
        lifecycleScope.launch {
            try {

                // 1. Fetch the session from your server
                val responseString = withContext(Dispatchers.IO) {
                    fetchPaymentSession()
                }

                Log.d("CheckoutFlow", "Server Response: $responseString")

                // 2. Parse the JSON response
                val jsonResponse = JSONObject(responseString)
                val fetchedId = jsonResponse.getString("id")
                val fetchedSecret = jsonResponse.getString("payment_session_secret")

                // --- MINIMAL ADDITION 3: Setup Coordinator ---
                val googlePayCoordinator = GooglePayFlowCoordinator(
                    context = this@MainActivity,
                    handleActivityResult = { resultCode, data ->
                        checkoutResultHandler?.invoke(resultCode, data)
                    }
                )
                val flowCoordinators = mapOf(PaymentMethodName.GooglePay to googlePayCoordinator)
                // ---------------------------------------------

                // 3. Configure the SDK
                val configuration = CheckoutComponentConfiguration(
                    context = this@MainActivity,
                    publicKey = BuildConfig.CHECKOUT_PUBLIC_KEY, // Found in Checkout.com Dashboard
                    environment = Environment.SANDBOX,
                    paymentSession = PaymentSessionResponse(
                        id = fetchedId,
                        secret = fetchedSecret
                    ),
                    // --- MINIMAL ADDITION 4: Pass coordinators map ---
                    flowCoordinators = flowCoordinators,
                    // -------------------------------------------------
                    componentCallback = ComponentCallback(
                        onSuccess = { component, paymentId ->
                            // Logs the full component object and paymentId payload
                            Log.d("CheckoutFlow", "Payment Successful! Component: $component, PaymentResult: $paymentId")
                            // TODO: Notify your server to finalize the order
                        },
                        onError = { component, error ->
                            // Logs the full component, the string representation of the error, and prints the stack trace
                            Log.e("CheckoutFlow", "Payment Error! Component: $component, Error Payload: $error", error)
                        }
                    )
                )

                // Instantiate the factory
                val checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

                // --- MINIMAL ADDITION 5: Link the result handler to your factory instance ---
                this@MainActivity.checkoutResultHandler = { resultCode, data ->
                    checkoutComponents.handleActivityResult(resultCode, data)
                }
                // ----------------------------------------------------------------------------

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
    // Function to make the HTTP POST request to your backend
    private fun fetchPaymentSession(): String {

        val url = URL("https://cko.jeff94040.com/create-payment-session")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Processing-Channel-Id", BuildConfig.CHECKOUT_PROCESSING_CHANNEL_ID)
        connection.setRequestProperty("Authorization", "sk_sbox_***************************")
        connection.doOutput = true

        val jsonPayload = """
        {
          "amount": 100,
          "currency": "USD",
          "reference": "FLOW-ANDROID-123456",
          "payment_type": "Regular",
          "display_name": "Jeff US",
          "items": [
            {
              "name": "widget",
              "unit_price": 100,
              "quantity": 1
            }
          ],
          "billing": {
            "address": {
              "country": "US"
            }
          },
          "customer": {
            "name": "John Doe",
            "email": "johndoe@yahoo.com"
          },
          "success_url": "https://cko.jeff94040.com/success",
          "failure_url": "https://cko.jeff94040.com/failure",
          "payment_method_configuration": {
            "card": {
              "store_payment_details": "collect_consent"
            }
          }
        }
        """.trimIndent()

        // Log payload sent to server
        Log.d("CheckoutFlow", "Sending Payload: $jsonPayload")

        // Use a buffered writer to ensure the stream is flushed correctly
        connection.outputStream.use { os ->
            os.writer(Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }
        }

        // Read the response
        val responseCode = connection.responseCode
        return if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = connection.errorStream.bufferedReader().use { it.readText() }
            throw Exception("HTTP $responseCode: $error")
        }
    }
}