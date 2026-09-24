(function () {
    "use strict";

    function initMobileNavigation() {
        const body = document.body;
        const sidebar = document.getElementById("melvoraSidebar");
        const toggle = document.querySelector("[data-mobile-menu-toggle]");
        const overlay = document.querySelector("[data-mobile-menu-close]");

        if (!body || !sidebar || !toggle) return;

        function isMobile() {
            return window.matchMedia("(max-width: 800px)").matches;
        }

        function setOpen(open) {
            if (!isMobile()) {
                body.classList.remove("mobile-menu-open");
                sidebar.classList.remove("mobile-open");
                toggle.setAttribute("aria-expanded", "false");
                return;
            }

            body.classList.toggle("mobile-menu-open", open);
            sidebar.classList.toggle("mobile-open", open);
            toggle.setAttribute("aria-expanded", String(open));
            overlay?.setAttribute("aria-hidden", String(!open));
        }

        toggle.addEventListener("click", function () {
            setOpen(!sidebar.classList.contains("mobile-open"));
        });

        overlay?.addEventListener("click", function () {
            setOpen(false);
        });

        sidebar.querySelectorAll("a.nav-link, a.brand").forEach(function (link) {
            link.addEventListener("click", function () {
                if (isMobile()) setOpen(false);
            });
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") setOpen(false);
        });

        window.addEventListener("resize", function () {
            if (!isMobile()) setOpen(false);
        });

        setOpen(false);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initMobileNavigation);
    } else {
        initMobileNavigation();
    }
})();
