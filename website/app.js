(() => {
  "use strict";
  const config = window.STRICT_BLOCKER_CONFIG || {};
  const byId = (id) => document.getElementById(id);
  const clean = (value) => String(value == null ? "" : value).trim();
  const price = Number(config.priceInr);
  const validPrice = Number.isFinite(price) && price > 0 && price <= 100000;
  const upiId = clean(config.upiId);
  const payeeName = clean(config.payeeName);
  const supportEmail = clean(config.supportEmail);
  const qrPath = clean(config.upiQrImage);
  const validUpi = /^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$/.test(upiId);
  const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(supportEmail);
  const checkoutReady = config.checkoutEnabled === true &&
    validPrice && validUpi && payeeName.length > 0 && validEmail;

  const formatPrice = (amount) => new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: amount % 1 === 0 ? 0 : 2
  }).format(amount);

  function makeOrderReference() {
    let suffix;
    try {
      const bytes = new Uint8Array(4);
      window.crypto.getRandomValues(bytes);
      suffix = Array.from(bytes).map((n) => n.toString(16).padStart(2, "0")).join("").toUpperCase();
    } catch (_error) {
      suffix = Math.random().toString(16).slice(2, 10).toUpperCase();
    }
    const date = new Date();
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    return "SB-" + date.getFullYear() + month + day + "-" + suffix;
  }

  const orderReference = makeOrderReference();
  byId("year").textContent = String(new Date().getFullYear());
  byId("orderReference").textContent = checkoutReady ? orderReference : "PREVIEW — CHECKOUT OFF";
  if (validPrice) byId("priceDisplay").textContent = formatPrice(price);
  else byId("priceDisplay").textContent = "Price not set";

  if (payeeName) byId("payeeDisplay").textContent = payeeName;
  if (validUpi) byId("upiDisplay").textContent = upiId;
  if (validEmail) byId("supportContact").textContent = "Support: " + supportEmail;

  const payButton = byId("payWithUpi");
  if (checkoutReady) {
    const query = new URLSearchParams({
      pa: upiId,
      pn: payeeName,
      am: price.toFixed(2),
      cu: "INR",
      tn: "Strict Blocker " + orderReference
    });
    payButton.href = "upi://pay?" + query.toString();
    payButton.classList.remove("is-disabled");
    payButton.setAttribute("aria-disabled", "false");
    payButton.removeAttribute("tabindex");
    byId("paymentStatus").textContent = "UPI checkout is configured. Verify the payee name and exact amount in your UPI app before approving.";
    byId("priceCaveat").textContent = "One-time payment in Indian rupees. Check the amount and seller terms before paying.";
    byId("qrCaption").textContent = "Scan only the seller's verified QR. Enter the displayed amount and check the payee name before paying.";

    ["customerEmail", "transactionReference", "customerNote"].forEach((id) => {
      byId(id).disabled = false;
    });
    byId("requestDeliveryButton").disabled = !validEmail;
    byId("deliveryStatus").textContent = validEmail
      ? "After payment, submit the transaction reference. Your email app will open a draft for you to review and send."
      : "The support email is invalid. Delivery requests remain disabled.";

    if (qrPath) {
      const qr = byId("upiQr");
      const placeholder = byId("qrPlaceholder");
      qr.addEventListener("load", () => {
        qr.hidden = false;
        placeholder.hidden = true;
      });
      qr.addEventListener("error", () => {
        qr.hidden = true;
        placeholder.hidden = false;
        byId("qrCaption").textContent = "QR image not found. The owner must add the verified QR image before launch.";
      });
      qr.src = qrPath;
    }
  } else {
    payButton.href = "#payment-setup";
    payButton.setAttribute("aria-disabled", "true");
    payButton.setAttribute("tabindex", "-1");
    byId("paymentStatus").textContent = "Preview only — payments are disabled. The owner must confirm price, UPI ID, payee name, QR, support and refund details before enabling checkout.";
    byId("qrCaption").textContent = "A real QR will appear here only after the owner adds it and enables checkout.";
    byId("deliveryStatus").textContent = "Delivery requests are disabled until checkout and a valid support email are configured.";
    ["customerEmail", "transactionReference", "customerNote", "requestDeliveryButton"].forEach((id) => {
      byId(id).disabled = true;
    });
  }

  byId("deliveryForm").addEventListener("submit", (event) => {
    event.preventDefault();
    if (!checkoutReady || !validEmail) {
      byId("deliveryStatus").textContent = "Checkout is not enabled. No request was sent.";
      return;
    }
    const email = clean(byId("customerEmail").value);
    const transaction = clean(byId("transactionReference").value);
    const note = clean(byId("customerNote").value).slice(0, 300);
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      byId("customerEmail").focus();
      byId("deliveryStatus").textContent = "Enter a valid email address.";
      return;
    }
    if (transaction.length < 6 || transaction.length > 64) {
      byId("transactionReference").focus();
      byId("deliveryStatus").textContent = "Enter the transaction reference shown by your UPI app.";
      return;
    }
    const subject = "Strict Blocker delivery request " + orderReference;
    const body = [
      "Hello,",
      "",
      "Please verify my Strict Blocker purchase and send the download instructions if payment is confirmed.",
      "Order reference: " + orderReference,
      "Customer email: " + email,
      "UPI transaction reference / UTR: " + transaction,
      "Expected amount: " + formatPrice(price),
      "Optional note: " + (note || "None"),
      "",
      "I understand that delivery is manual and subject to payment verification."
    ].join("\n");
    const mailto = "mailto:" + encodeURIComponent(supportEmail) +
      "?subject=" + encodeURIComponent(subject) + "&body=" + encodeURIComponent(body);
    byId("deliveryStatus").textContent = "Your email app should open with a draft. Review it and press Send. No request is sent automatically.";
    window.location.href = mailto;
  });
})();