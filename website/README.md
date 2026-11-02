# Strict Blocker information site

This folder contains a responsive, static product-information website for the Strict Blocker Android app.

## What this site includes

- Product overview and feature explanations
- A short explanation of the app setup flow
- Android compatibility and permission notes
- Companion, accessibility and privacy information

This is an informational site only. It has no checkout, payment links, UPI QR codes, order forms, subscriptions, licensing flow or customer payment collection.

## Local preview

Open `index.html` in a browser, or serve the `website` folder with any static HTTP server. The site has no build step and uses only local HTML, CSS and JavaScript assets.

## Publishing

Upload the contents of this folder to a static hosting provider and configure its publish/root directory to this folder. Enable HTTPS and review the product text against the current app release before publishing.

## Maintenance

Update the app version in `config.js` when the Android release changes. Keep public information accurate, especially Android permission requirements and the limits of background enforcement. Core app behaviour can vary by device and Android version.
