document.addEventListener('DOMContentLoaded', function () {
    var sidebar = document.querySelector('.admin-sidebar');
    var toggle = document.querySelector('.sidebar-toggle');
    var overlay = document.querySelector('.sidebar-overlay');

    function closeSidebar() {
        if (!sidebar) return;
        sidebar.classList.remove('open');
        if (overlay) overlay.hidden = true;
        if (toggle) toggle.setAttribute('aria-expanded', 'false');
    }

    if (toggle && sidebar) {
        toggle.addEventListener('click', function () {
            var open = sidebar.classList.toggle('open');
            overlay.hidden = !open;
            toggle.setAttribute('aria-expanded', String(open));
        });
        overlay.addEventListener('click', closeSidebar);
    }

    document.querySelectorAll('.admin-complaint-summary').forEach(function (button) {
        button.addEventListener('click', function () {
            var detail = document.querySelector('#' + button.getAttribute('aria-controls'));
            var open = button.getAttribute('aria-expanded') !== 'true';
            button.setAttribute('aria-expanded', String(open));
            detail.hidden = !open;
        });
    });

    var openedModal = null;
    var previousFocus = null;

    function openComplaintModal(modalId, event) {
        var modal = document.getElementById(modalId);
        if (!modal) return;
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        previousFocus = document.activeElement;
        modal.hidden = false;
        openedModal = modal;
        document.body.classList.add('modal-open');
        var closeButton = modal.querySelector(
            '.complaint-modal-header button, .member-modal-header button'
        );
        if (closeButton) closeButton.focus();
    }

    function closeComplaintModal() {
        if (!openedModal) return;
        openedModal.hidden = true;
        openedModal = null;
        document.body.classList.remove('modal-open');
        if (previousFocus) previousFocus.focus();
    }

    document.querySelectorAll('[data-complaint-modal-open]').forEach(function (opener) {
        opener.addEventListener('click', function (event) {
            openComplaintModal(opener.getAttribute('data-complaint-modal-open'), event);
        });
        opener.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                openComplaintModal(opener.getAttribute('data-complaint-modal-open'), event);
            }
        });
    });

    document.querySelectorAll('[data-complaint-modal-close]').forEach(function (closer) {
        closer.addEventListener('click', closeComplaintModal);
    });

    document.querySelectorAll('[data-member-modal-open]').forEach(function (opener) {
        opener.addEventListener('click', function (event) {
            openComplaintModal(opener.getAttribute('data-member-modal-open'), event);
        });
    });

    document.querySelectorAll('[data-member-modal-close]').forEach(function (closer) {
        closer.addEventListener('click', closeComplaintModal);
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') closeComplaintModal();
    });

    document.querySelectorAll('.answer-file-input').forEach(function (box) {
        var input = box.querySelector('[data-answer-file-input]');
        var names = box.querySelector('[data-answer-file-names]');
        var clear = box.querySelector('[data-answer-file-clear]');
        if (!input || !names || !clear) return;

        input.addEventListener('change', function () {
            names.textContent = input.files.length
                ? Array.from(input.files).map(function (file) { return file.name; }).join(', ')
                : '선택된 파일이 없습니다.';
        });
        clear.addEventListener('click', function () {
            input.value = '';
            names.textContent = '선택된 파일이 없습니다.';
        });
    });

    document.querySelectorAll('[data-answer-status-form]').forEach(function (form) {
        var status = form.querySelector('[data-answer-status]');
        var answer = form.querySelector('[data-answer-content]');
        if (!status || !answer) return;

        function selectedDefaultAnswer() {
            var selected = status.options[status.selectedIndex];
            return selected ? selected.getAttribute('data-default-answer') || '' : '';
        }

        if (!answer.value.trim()) {
            answer.value = selectedDefaultAnswer();
        }

        status.addEventListener('change', function () {
            answer.value = selectedDefaultAnswer();
            answer.focus();
        });
    });

    document.querySelectorAll('[data-member-role-form]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var roleSelect = form.querySelector('select[name="role"]');
            var memberName = form.getAttribute('data-member-name') || '선택한 회원';
            var roleLabel = roleSelect && roleSelect.value === 'M'
                ? '마스터'
                : roleSelect && roleSelect.value === 'A' ? '관리자' : '사용자';
            if (!window.confirm(memberName + ' 회원의 권한을 ' + roleLabel + '(으)로 변경하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-main-banner-form]').forEach(function (form) {
        var input = form.querySelector('[data-main-banner-input]');
        var names = form.querySelector('[data-main-banner-names]');
        var remaining = Number(form.getAttribute('data-remaining') || 0);
        if (!input || !names) return;

        input.addEventListener('change', function () {
            if (input.files.length > remaining) {
                input.value = '';
                names.textContent = '현재는 최대 ' + remaining + '장까지 추가할 수 있습니다.';
                names.classList.add('error');
                return;
            }
            names.classList.remove('error');
            names.textContent = input.files.length
                ? Array.from(input.files).map(function (file) { return file.name; }).join(', ')
                : '선택된 이미지가 없습니다.';
        });

        form.addEventListener('submit', function (event) {
            if (!input.files.length || input.files.length > remaining) {
                event.preventDefault();
                names.classList.add('error');
                names.textContent = '등록 가능한 배너 이미지를 선택해 주세요.';
            }
        });
    });

    document.querySelectorAll('[data-main-banner-delete]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('이 메인 배너 이미지를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-notice-popup-form]').forEach(function (form) {
        var toggle = form.querySelector('[data-notice-popup-toggle]');
        if (!toggle) return;
        toggle.addEventListener('change', function () {
            form.submit();
        });
    });

    document.querySelectorAll('[data-notice-delete-form]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('이 공지사항을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.')) {
                event.preventDefault();
            }
        });
    });

    var openedPartnerPopover = null;

    function closePartnerPopover() {
        if (!openedPartnerPopover) return;
        openedPartnerPopover.hidden = true;
        openedPartnerPopover = null;
    }

    document.querySelectorAll('[data-partner-popover-open]').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.stopPropagation();
            var popover = document.getElementById(button.getAttribute('data-partner-popover-open'));
            if (!popover) return;
            var wasOpen = !popover.hidden;
            closePartnerPopover();
            if (wasOpen) return;

            popover.hidden = false;
            var buttonRect = button.getBoundingClientRect();
            var popoverWidth = Math.min(390, window.innerWidth - 24);
            var left = Math.min(buttonRect.right + 10, window.innerWidth - popoverWidth - 12);
            var top = Math.min(buttonRect.top, window.innerHeight - popover.offsetHeight - 12);
            popover.style.width = popoverWidth + 'px';
            popover.style.left = Math.max(12, left) + 'px';
            popover.style.top = Math.max(12, top) + 'px';
            openedPartnerPopover = popover;
        });
    });

    document.querySelectorAll('[data-partner-popover-close]').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.stopPropagation();
            closePartnerPopover();
        });
    });

    document.addEventListener('click', function (event) {
        if (openedPartnerPopover && !event.target.closest('.partner-popover')) {
            closePartnerPopover();
        }
    });

    var openedPartnerModal = null;
    var partnerModalTrigger = null;

    function closePartnerModal() {
        if (!openedPartnerModal) return;
        openedPartnerModal.hidden = true;
        openedPartnerModal = null;
        document.body.classList.remove('modal-open');
        if (partnerModalTrigger) partnerModalTrigger.focus();
    }

    document.querySelectorAll('[data-partner-modal-open]').forEach(function (button) {
        button.addEventListener('click', function () {
            var modal = document.getElementById(button.getAttribute('data-partner-modal-open'));
            if (!modal) return;
            closePartnerPopover();
            partnerModalTrigger = button;
            openedPartnerModal = modal;
            modal.hidden = false;
            document.body.classList.add('modal-open');
            var firstInput = modal.querySelector('input:not([type="hidden"])');
            if (firstInput) firstInput.focus();
        });
    });

    document.querySelectorAll('[data-partner-modal-close]').forEach(function (button) {
        button.addEventListener('click', closePartnerModal);
    });

    document.querySelectorAll('[data-partner-delete]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var name = form.getAttribute('data-partner-name') || '선택한 협력업체';
            if (!window.confirm(name + ' 협력업체 정보를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') return;
        closePartnerPopover();
        closePartnerModal();
    });

    document.querySelectorAll('[data-preparation-form]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            window.alert('화면 구성이 완료되었으며, 저장 기능은 다음 단계에서 연결됩니다.');
        });
    });
});
