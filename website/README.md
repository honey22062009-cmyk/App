# Strict Blocker static storefront (manual UPI)

This folder is a responsive static website for an Android APK distribution flow. It has no backend, does not automatically verify payments, and does not store customer form submissions on a server. Checkout and delivery requests are disabled by default.

## Before publishing

Have a parent/legal guardian help confirm that the payment account and use of the QR are permitted by the payment provider. Use accurate account, identity, and bank details. Do not misstate anyone's age or identity.

1. Confirm the real selling price, app version, product description, support contact, and refund terms before accepting money. The default INR 99 is a placeholder only.
2. Complete merchant/account eligibility checks. Verify the payee name and UPI ID from the account that will receive the money.
3. Export the genuine receiving-account QR from the approved payment app/provider and save it as website/assets/upi-qr.png. The file website/assets/upi-qr-placeholder.svg is a decorative placeholder and is NOT a real payment QR.
4. Edit website/config.js:
   - Set priceInr to the actual agreed price.
   - Set upiId and payeeName to the exact verified receiving-account details.
   - Keep upiQrImage as assets/upi-qr.png if using that filename.
   - Set supportEmail to a real monitored address.
   - Keep checkoutEnabled false until every detail and the test flow has been reviewed. Only then set it to true.
5. Check the payee and amount on a phone with a UPI app. Complete a small permitted test transaction if appropriate and confirm it in the actual receiving account. Ensure the QR and UPI link point to the same recipient.
6. Publish over HTTPS and add correct seller identity/contact details and a clear refund policy before taking live orders.

## Buyer flow

- The website creates a page-local order reference and includes it in the UPI payment note. It is for manual matching only and is not a server-side order record or proof of payment.
- On mobile, Pay with UPI opens a UPI payment intent with recipient and amount prefilled. A compatible UPI app is required and behavior varies by app. The buyer must verify the payee and amount before approving.
- The QR image is static. It does not dynamically encode the order reference or prefill an amount; the buyer may need to enter the amount manually.
- After payment, the buyer enters their email and transaction reference. The form opens a mail draft addressed to the configured support email. The customer must review and press Send; nothing is submitted to the website.
- The owner checks the successful transaction in their own provider/bank history and manually sends the agreed download instructions. Never treat a screenshot alone as proof. Never ask for a UPI PIN or OTP.

## Security and scope

- This static website does not automatically confirm UPI payments and must not promise instant delivery.
- All values in website/config.js are public. Never store API keys, passwords, OTPs, bank credentials, or private payment keys in it.
- A UPI intent/QR does not prove a payment. Verify final status, amount, payee, and transaction reference in the receiving account before delivery.
- The mailto delivery flow is deliberately simple and can include the entered email and reference in the draft. Do not collect unnecessary customer information. A server-backed form, payment webhooks, refunds, order history, and automated licensing require a separately designed backend with server-side verification.
- The Android app currently has no server-side licence or purchase-verification system. Sending the APK and granting in-app Premium access are different things.
- Seller eligibility, privacy, tax, refund, and consumer-protection obligations depend on the actual arrangement. Obtain appropriate guardian/professional guidance before launching.
- Adding this folder to GitHub does not publish the website automatically.

## Static hosting

Upload the contents of this folder to your static hosting provider and configure its publish/root directory to this website folder. Use HTTPS. Preview and validate the site first while checkout remains disabled. Do not publish a live checkout until the guardian has reviewed the real settings and terms.