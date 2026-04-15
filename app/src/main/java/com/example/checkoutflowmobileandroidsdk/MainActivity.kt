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

                // Fetch the session from your server
                val responseString = withContext(Dispatchers.IO) {
                    fetchPaymentSession()
                }

                Log.d("CheckoutFlow", "Server Response: $responseString")

                // Parse the JSON response
                val jsonResponse = JSONObject(responseString)
                val fetchedId = jsonResponse.getString("id")
                //val fetchedId = "ps_3CGZWYplTEwhl9ywt4xbLJ7BrmD"
                val fetchedSecret = jsonResponse.getString("payment_session_secret")
                //val fetchedSecret = "pss_235ea93e-0fbe-4e90-8568-17119691ca9d"

                // Setup Google Pay Coordinator
                val googlePayCoordinator = GooglePayFlowCoordinator(
                    context = this@MainActivity,
                    handleActivityResult = { resultCode, data ->
                        checkoutResultHandler?.invoke(resultCode, data)
                    }
                )
                val flowCoordinators = mapOf(PaymentMethodName.GooglePay to googlePayCoordinator)

                /*
                val cardOptions = ComponentOption(
                    addressConfiguration = addressConfig,
                )
                */

                // Configure the Checkout SDK
                val configuration = CheckoutComponentConfiguration(
                    context = this@MainActivity,
                    publicKey = BuildConfig.CHECKOUT_PUBLIC_KEY, // Found in Checkout.com Dashboard
                    environment = Environment.SANDBOX,
                    paymentSession = PaymentSessionResponse(
                        id = fetchedId,
                        secret = fetchedSecret
                    ),
                    //componentOptions = mapOf(PaymentMethodName.Card to cardOptions),
                    flowCoordinators = flowCoordinators,
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
                        /*,
                        handleSubmit = { sessionData ->
                            Log.d("CheckoutFlow", "Fired handleSubmit! SessionData: $sessionData")
                            // Use Success here to indicate a successful handover
                            ApiCallResult.Success
                        }
                        */
                    )
                )

                // Instantiate the factory
                val checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

                // Link the result handler to your factory instance ---
                this@MainActivity.checkoutResultHandler = { resultCode, data ->
                    checkoutComponents.handleActivityResult(resultCode, data)
                }

                // Create the "Flow" component which manages all payment methods
                val flow = checkoutComponents.create(ComponentName.Flow)

                setContent {
                    MaterialTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // Render the UI directly in Compose
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

        // This API call should be made from the backend, not the app. Illustrative only.
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

        jsonObject.put("processing_channel_id", BuildConfig.CHECKOUT_PROCESSING_CHANNEL_ID)

        val finalJsonPayload = jsonObject.toString()

        // Log payload sent to server
        Log.d("CheckoutFlow", "Sending Payload: $finalJsonPayload")

        // Use a buffered writer to ensure the stream is flushed correctly
        connection.outputStream.use { os ->
            os.writer(Charsets.UTF_8).use { writer ->
                writer.write(finalJsonPayload)
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

    /*
    // 1. Create the Address Configuration - Needs further experimentation
    val addressConfig = AddressConfiguration(
        fields = listOf(
            AddressField.Country,
            AddressField.AddressLine1(isOptional = false),
            AddressField.AddressLine2(isOptional = true),
            AddressField.City(isOptional = false),
            AddressField.State(isOptional = false),
            AddressField.Zip(isOptional = false)
        ),
        // Callback fired when the user finishes entering the address
        onComplete = { contactData ->
            Log.d("CheckoutFlow", "Address Collected: ${contactData?.address}")
        }
    )
    */

}