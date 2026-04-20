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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.component.AddressConfiguration
import com.checkout.components.interfaces.component.ComponentOption
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.model.AddressField
import com.checkout.components.interfaces.model.CallbackResult
import com.checkout.components.interfaces.model.ComponentName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.wallet.wrapper.GooglePayFlowCoordinator

import org.json.JSONObject

import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    private var checkoutResultHandler: ((Int, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize and display the Flow Component
        lifecycleScope.launch {
            try {

                // Create payment session
                val responseString = withContext(Dispatchers.IO) {
                    fetchPaymentSession()
                }

                // Log create payment session response
                Log.d("CheckoutFlow", "Server Response: $responseString")

                // Parse the JSON response
                val jsonResponse = JSONObject(responseString)
                val fetchedId = jsonResponse.getString("id")
                val fetchedSecret = jsonResponse.getString("payment_session_secret")

                // Setup Google Pay Coordinator
                val googlePayCoordinator = GooglePayFlowCoordinator(
                    context = this@MainActivity,
                    handleActivityResult = { resultCode, data ->
                        checkoutResultHandler?.invoke(resultCode, data)
                    }
                )

                // Setup Component Options
                val componentOptions = ComponentOption(
                    // Collect Billing Address
                    addressConfiguration = AddressConfiguration(
                        // Optional - specify the fields to collect along with optionality
                        fields = listOf(
                            AddressField.FirstName(),
                            AddressField.LastName(),
                            AddressField.Country,
                            AddressField.State(),
                            AddressField.Zip(isOptional = false),
                            AddressField.City(),
                            AddressField.AddressLine1(),
                            AddressField.AddressLine1(),
                            AddressField.Email(isOptional = false),
                            AddressField.Phone()
                        ),
                        // Required
                        onComplete = { contactData ->
                            Log.d("CheckoutFlow", "Fired onComplete! ContactData: $contactData")
                        }
                    )
                )

                // Configure the Checkout SDK
                val configuration = CheckoutComponentConfiguration(
                    context = this@MainActivity,
                    publicKey = BuildConfig.CHECKOUT_PUBLIC_KEY, // Found in Checkout.com Dashboard
                    environment = Environment.SANDBOX,
                    paymentSession = PaymentSessionResponse(
                        id = fetchedId,
                        secret = fetchedSecret
                    ),
                    flowCoordinators = mapOf(PaymentMethodName.GooglePay to googlePayCoordinator),
                    componentCallback = ComponentCallback(
                        onReady = { component ->
                            Log.d("CheckoutFlow", "Fired onReady!")
                        },
                        onChange = { component ->
                           // Fires each time a key is pressed
                           // Log.d("CheckoutFlow", "Fired onChange!")
                        },
                        onSubmit = { component ->
                            Log.d("CheckoutFlow", "Fired onSubmit!")
                        },
                        onSuccess = { component, paymentId ->
                            Log.d("CheckoutFlow", "Fired onSuccess! PaymentResult: $paymentId")
                        },
                        onError = { component, error ->
                            Log.e("CheckoutFlow", "Fired onError! Error: $error")
                        },
                        onTokenized = { tokenizationResult ->
                            Log.d("CheckoutFlow", "Fired onTokenized! Result: $tokenizationResult")
                            CallbackResult.Accepted
                        },
                        onCardBinChanged = { cardMetadata ->
                            Log.d("CheckoutFlow", "Fired onCardBinChanged! Metadata: $cardMetadata")
                            CallbackResult.Accepted
                        },
                        handleTap = { component ->
                            Log.d("CheckoutFlow", "Fired handleTap!")
                            true // This remains a simple Boolean
                        }
                    )
                )

                // Instantiate the factory
                val checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

                // Link the result handler to your factory instance ---
                this@MainActivity.checkoutResultHandler = { resultCode, data ->
                    checkoutComponents.handleActivityResult(resultCode, data)
                }

                // Create the "Flow" component which manages all payment methods
                val flow = checkoutComponents.create(ComponentName.Flow, componentOptions)

                setContent {
                    MaterialTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            flow.Render()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckoutFlow", "Initialization failed", e)
            }
        }
    }

    // Function to create Payment Session
    private fun fetchPaymentSession(): String {

        // Make this API call from the backend only. Illustrative only.
        val url = URL("https://api.sandbox.checkout.com/payment-sessions")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"

        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", BuildConfig.CHECKOUT_SECRET_KEY) // placeholder
        connection.doOutput = true

        //val filename = "aft_transaction.json"
        val filename = "purchase_transaction.json"

        val fileText = assets.open(filename).bufferedReader().use { it.readText() }

        val jsonObject = org.json.JSONObject(fileText)

        // Insert Processing Channel ID into JSON request body
        jsonObject.put("processing_channel_id", BuildConfig.CHECKOUT_PROCESSING_CHANNEL_ID)

        val finalJsonPayload = jsonObject.toString()

        // Log create Payment Session payload sent to server
        Log.d("CheckoutFlow", "Sending Payload: $finalJsonPayload")

        connection.outputStream.use { os ->
            os.writer(Charsets.UTF_8).use { writer ->
                writer.write(finalJsonPayload)
                writer.flush()
            }
        }

        // Read the server's response
        val responseCode = connection.responseCode
        return if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = connection.errorStream.bufferedReader().use { it.readText() }
            throw Exception("HTTP $responseCode: $error")
        }
    }

}