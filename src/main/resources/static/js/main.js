document.addEventListener('DOMContentLoaded', () => {
    const menuButton = document.querySelector('.menu-toggle');
    const navigation = document.querySelector('.main-nav');
    const submenuButtons = document.querySelectorAll('[data-submenu-toggle]');
    const sliderDots = document.querySelectorAll('.slider-dots button');
    const bannerSlides = document.querySelectorAll('.hero-slide');
    const bannerHoverArea = document.querySelector('.hero');
    const bannerNextButton = document.querySelector('[data-banner-next]');
    const complaintLinks = document.querySelectorAll('a[href^="/complaints/new"]');
    const loginRequiredModal = document.querySelector('#login-required-modal');
    const modalLoginLink = document.querySelector('#modal-login-link');
    const noticePopups = Array.from(document.querySelectorAll('[data-notice-popup]'));
    const scrollTopButton = document.querySelector('[data-scroll-top]');
    const isLoggedIn = document.body.dataset.loggedIn === 'true';
    let modalTrigger = null;
    let currentBannerIndex = 0;
    let bannerTimer = null;
    let bannerPaused = false;

    const noticePopupStorageKey = (noticeId) => `sg-notice-popup-hidden-until-${noticeId}`;

    const isNoticePopupHiddenToday = (popup) => {
        try {
            const key = noticePopupStorageKey(popup.dataset.noticePopupId);
            const hiddenUntil = Number(window.localStorage.getItem(key) || 0);
            if (hiddenUntil > Date.now()) return true;
            window.localStorage.removeItem(key);
        } catch (error) {
            return false;
        }
        return false;
    };

    const arrangeNoticePopups = () => {
        const visiblePopups = noticePopups.filter((popup) => !popup.hidden);
        visiblePopups.forEach((popup, index) => {
            popup.style.setProperty('--notice-popup-offset', `${index * 24}px`);
            popup.style.setProperty('--notice-popup-mobile-offset', `${index * 14}px`);
            popup.style.setProperty('--notice-popup-layer', String(visiblePopups.length - index));

            const order = popup.querySelector('[data-notice-popup-order]');
            if (order) {
                order.textContent = visiblePopups.length > 1
                    ? `${index + 1} / ${visiblePopups.length}`
                    : '';
            }
        });
    };

    const showNoticePopups = () => {
        noticePopups.forEach((popup) => {
            if (isNoticePopupHiddenToday(popup)) return;
            popup.hidden = false;
            popup.setAttribute('aria-hidden', 'false');
        });
        arrangeNoticePopups();
    };

    const closeNoticePopup = (popup) => {
        if (!popup || popup.hidden) return;
        const hideToday = popup.querySelector('[data-notice-hide-today]');
        if (hideToday && hideToday.checked) {
            try {
                window.localStorage.setItem(
                    noticePopupStorageKey(popup.dataset.noticePopupId),
                    String(Date.now() + 24 * 60 * 60 * 1000)
                );
            } catch (error) {
                // 저장소를 사용할 수 없는 브라우저에서는 현재 방문 중에만 닫습니다.
            }
        }
        popup.hidden = true;
        popup.setAttribute('aria-hidden', 'true');
        arrangeNoticePopups();
    };

    const closeSubmenus = (exceptGroup = null) => {
        submenuButtons.forEach((button) => {
            const group = button.closest('.nav-group');
            if (group === exceptGroup) return;
            group.classList.remove('open');
            button.setAttribute('aria-expanded', 'false');
        });
    };

    submenuButtons.forEach((button) => {
        button.addEventListener('click', () => {
            const group = button.closest('.nav-group');
            const willOpen = !group.classList.contains('open');
            closeSubmenus(group);
            group.classList.toggle('open', willOpen);
            button.setAttribute('aria-expanded', String(willOpen));
        });
    });

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.nav-group')) closeSubmenus();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        const openGroup = document.querySelector('.nav-group.open');
        if (!openGroup) return;
        const button = openGroup.querySelector('[data-submenu-toggle]');
        closeSubmenus();
        button.focus();
    });

    const closeLoginModal = () => {
        if (!loginRequiredModal) return;
        loginRequiredModal.hidden = true;
        loginRequiredModal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
        if (modalTrigger) modalTrigger.focus();
    };

    const openLoginModal = (trigger) => {
        if (!loginRequiredModal) return;
        modalTrigger = trigger;
        loginRequiredModal.hidden = false;
        loginRequiredModal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
        modalLoginLink.focus();
    };

    complaintLinks.forEach((link) => {
        link.addEventListener('click', (event) => {
            if (isLoggedIn) return;
            event.preventDefault();
            openLoginModal(link);
        });
    });

    if (loginRequiredModal) {
        loginRequiredModal.querySelectorAll('[data-modal-close]').forEach((button) => {
            button.addEventListener('click', closeLoginModal);
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !loginRequiredModal.hidden) {
                closeLoginModal();
            }
        });
    }

    noticePopups.forEach((popup) => {
        popup.querySelectorAll('[data-notice-popup-close]').forEach((button) => {
            button.addEventListener('click', () => closeNoticePopup(popup));
        });
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        const visiblePopups = noticePopups.filter((popup) => !popup.hidden);
        closeNoticePopup(visiblePopups[0]);
    });

    if (menuButton && navigation) {
        menuButton.addEventListener('click', () => {
            const isOpen = navigation.classList.toggle('open');
            menuButton.setAttribute('aria-expanded', String(isOpen));
            menuButton.querySelector('.sr-only').textContent = isOpen ? '메뉴 닫기' : '메뉴 열기';
        });

        navigation.addEventListener('click', (event) => {
            if (!event.target.closest('a')) return;
            closeSubmenus();
            navigation.classList.remove('open');
            menuButton.setAttribute('aria-expanded', 'false');
            menuButton.querySelector('.sr-only').textContent = '메뉴 열기';
        });

        window.addEventListener('resize', () => {
            if (window.innerWidth > 900) {
                closeSubmenus();
                navigation.classList.remove('open');
                menuButton.setAttribute('aria-expanded', 'false');
                menuButton.querySelector('.sr-only').textContent = '메뉴 열기';
            }
        });
    }

    if (scrollTopButton) {
        scrollTopButton.addEventListener('click', () => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    const showBanner = (index) => {
        if (!bannerSlides.length) return;
        currentBannerIndex = (index + bannerSlides.length) % bannerSlides.length;
        bannerSlides.forEach((slide, slideIndex) => {
            const active = slideIndex === currentBannerIndex;
            slide.classList.toggle('active', active);
            slide.setAttribute('aria-hidden', String(!active));
        });
        sliderDots.forEach((dot, dotIndex) => {
            const active = dotIndex === currentBannerIndex;
            dot.classList.toggle('active', active);
            if (active) dot.setAttribute('aria-current', 'true');
            else dot.removeAttribute('aria-current');
        });
    };

    const startBannerTimer = () => {
        if (bannerSlides.length < 2 || bannerPaused) return;
        window.clearInterval(bannerTimer);
        bannerTimer = window.setInterval(() => {
            showBanner(currentBannerIndex + 1);
        }, 5000);
    };

    const moveBannerManually = (index) => {
        showBanner(index);
        startBannerTimer();
    };

    sliderDots.forEach((dot, dotIndex) => {
        dot.addEventListener('click', () => moveBannerManually(dotIndex));
    });

    if (bannerNextButton) {
        bannerNextButton.addEventListener('click', () => {
            moveBannerManually(currentBannerIndex + 1);
        });
    }

    if (bannerHoverArea) {
        bannerHoverArea.addEventListener('mouseenter', () => {
            bannerPaused = true;
            window.clearInterval(bannerTimer);
        });
        bannerHoverArea.addEventListener('mouseleave', () => {
            bannerPaused = false;
            startBannerTimer();
        });
    }

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) window.clearInterval(bannerTimer);
        else if (!bannerPaused) startBannerTimer();
    });

    showBanner(0);
    startBannerTimer();
    showNoticePopups();
});
