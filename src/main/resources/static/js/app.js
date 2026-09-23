// Deferred, dependency-free. Everything here is a small enhancement —
// every form still works with JS disabled, since all state changes are
// plain POSTs handled server-side.
(function () {
    "use strict";

    // Auto-dismiss success/error banners after a few seconds.
    document.querySelectorAll(".banner").forEach(function (banner) {
        setTimeout(function () {
            banner.style.transition = "opacity 300ms ease, transform 300ms ease";
            banner.style.opacity = "0";
            banner.style.transform = "translateY(-4px)";
        }, 4000);
    });
})();
