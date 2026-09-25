(function () {
    "use strict";

    const STORAGE_PREFIX = "melvora.nav.group.v2.";

    function initNavGroups() {
        const groups = document.querySelectorAll("[data-nav-group]");
        if (!groups.length) return;

        groups.forEach(function (group) {
            const toggle = group.querySelector(".nav-group-toggle");
            const items = group.querySelector(".nav-group-items");
            if (!toggle || !items) return;

            // Hide module groups that are empty after Thymeleaf permissions/module filters.
            if (!items.querySelector(".nav-link")) {
                group.hidden = true;
                return;
            }

            const key = STORAGE_PREFIX + group.dataset.navGroup;
            const activeLink = items.querySelector(".nav-link.active");
            const stored = localStorage.getItem(key);

            let expanded;
            if (activeLink) {
                expanded = true;
            } else if (stored !== null) {
                expanded = stored === "true";
            } else {
                expanded = false;
            }

            setExpanded(toggle, expanded);

            toggle.addEventListener("click", function () {
                const next = toggle.getAttribute("aria-expanded") !== "true";
                setExpanded(toggle, next);
                localStorage.setItem(key, String(next));
            });
        });
    }

    function setExpanded(toggle, expanded) {
        toggle.setAttribute("aria-expanded", String(expanded));
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initNavGroups);
    } else {
        initNavGroups();
    }
})();
