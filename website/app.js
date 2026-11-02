(() => {
  "use strict";
  const year = document.getElementById("year");
  if (year) year.textContent = String(new Date().getFullYear());

  const config = window.STRICT_BLOCKER_CONFIG || {};
  const version = document.getElementById("appVersion");
  if (version && typeof config.appVersion === "string") {
    version.textContent = config.appVersion;
  }
})();
