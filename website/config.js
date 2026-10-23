/*
 * These values are public to every website visitor.
 * NEVER place a password, OTP, secret key, or private bank credential here.
 * Keep checkoutEnabled false until account eligibility, seller details, terms, and tests
 * have been reviewed with a parent/legal guardian.
 */
window.STRICT_BLOCKER_CONFIG = {
  productName: "Strict Blocker",
  appVersion: "1.4",
  priceInr: 99,
  currency: "INR",

  // Copy these exactly from the approved account that will receive the money.
  upiId: "",
  payeeName: "",

  // Add your genuine QR to website/assets/upi-qr.png.
  // Do NOT use the placeholder SVG as a real payment code.
  upiQrImage: "assets/upi-qr.png",

  // Use a monitored support inbox controlled by the approved account holder/guardian.
  supportEmail: "",

  // Deliberately disabled by default. Enable only after all details are verified.
  checkoutEnabled: false
};