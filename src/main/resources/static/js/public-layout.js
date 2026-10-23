document.addEventListener('DOMContentLoaded', () => {
    const menuButton = document.querySelector('.public-menu-toggle');
    const navigation = document.querySelector('.public-main-nav');
    const submenuButtons = document.querySelectorAll('[data-public-submenu-toggle]');

    const closeSubmenus = (exceptGroup = null) => {
        submenuButtons.forEach((button) => {
            const group = button.closest('.public-nav-group');
            if (group === exceptGroup) return;
            group.classList.remove('open');
            button.setAttribute('aria-expanded', 'false');
        });
    };

    submenuButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const group = button.closest('.public-nav-group');
            const willOpen = !group.classList.contains('open');
            closeSubmenus(group);
            group.classList.toggle('open', willOpen);
            button.setAttribute('aria-expanded', String(willOpen));
        });
    });

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.public-nav-group')) closeSubmenus();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        const openGroup = document.querySelector('.public-nav-group.open');
        if (!openGroup) return;
        const button = openGroup.querySelector('[data-public-submenu-toggle]');
        closeSubmenus();
        button.focus();
    });

    if (menuButton && navigation) {
        menuButton.addEventListener('click', () => {
            const open = navigation.classList.toggle('open');
            menuButton.setAttribute('aria-expanded', String(open));
            menuButton.querySelector('.public-sr-only').textContent = open ? '메뉴 닫기' : '메뉴 열기';
        });
        navigation.addEventListener('click', (event) => {
            if (!event.target.closest('a')) return;
            closeSubmenus();
            navigation.classList.remove('open');
            menuButton.setAttribute('aria-expanded', 'false');
        });
    }

    const scrollTopButton = document.querySelector('[data-public-scroll-top]');
    if (scrollTopButton) {
        scrollTopButton.addEventListener('click', () => window.scrollTo({top: 0, behavior: 'smooth'}));
    }
});
