# Checkout Flow Mobile Android SDK

## ⚙️ Local Setup Instructions

To run this project locally, you must provide your own Checkout.com sandbox API keys. These keys are kept out of version control for security.

1. Create a new file named `local.properties` in the **root directory** of this project (if it doesn't already exist).
2. Add the following keys to the file, replacing the placeholder values with your actual Checkout.com sandbox credentials:

```properties
CHECKOUT_PROCESSING_CHANNEL_ID=pc_...
CHECKOUT_SECRET_KEY=sk_sbox_...
CHECKOUT_PUBLIC_KEY=pk_sbox_...