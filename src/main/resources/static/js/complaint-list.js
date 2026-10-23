document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var modal = document.querySelector('#password-modal');
    var form = document.querySelector('#password-form');
    var password = document.querySelector('#post-view-password');
    var message = document.querySelector('#password-message');
    var selectedTitle = document.querySelector('#selected-post-title');
    var submitButton = form.querySelector('.confirm-button');
    var lastTrigger = null;

    function openModal(button) {
        lastTrigger = button;
        var complaintNo = button.dataset.complaintNo;
        form.action = appUrl('/complaints/' + encodeURIComponent(complaintNo) + '/verify');
        selectedTitle.textContent = button.dataset.complaintTitle;
        password.value = '';
        message.textContent = '';
        modal.hidden = false;
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
        window.setTimeout(function () { password.focus(); }, 0);
    }

    function closeModal() {
        modal.hidden = true;
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
        if (lastTrigger) lastTrigger.focus();
    }

    document.querySelectorAll('[data-password-open]').forEach(function (button) {
        button.addEventListener('click', function () { openModal(button); });
    });
    document.querySelectorAll('[data-password-close]').forEach(function (button) {
        button.addEventListener('click', closeModal);
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !modal.hidden) closeModal();
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!password.value) {
            message.textContent = '비밀번호를 입력해 주세요.';
            password.focus();
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = '확인 중...';
        message.textContent = '';

        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            credentials: 'same-origin'
        })
            .then(function (response) {
                return response.json().then(function (result) {
                    if (!response.ok || result.success !== true) {
                        throw new Error(result.message || '비밀번호를 확인할 수 없습니다.');
                    }
                    return result;
                });
            })
            .then(function (result) {
                window.location.href = appUrl(result.redirectUrl);
            })
            .catch(function (error) {
                message.textContent = error.message;
                password.select();
            })
            .finally(function () {
                submitButton.disabled = false;
                submitButton.textContent = '확인';
            });
    });
});
