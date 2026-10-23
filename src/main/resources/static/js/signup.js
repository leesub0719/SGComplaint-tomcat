document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var form = document.querySelector('#signup-form');
    var userId = document.querySelector('#user-id');
    var checkUserIdButton = document.querySelector('#check-user-id');
    var userIdMessage = document.querySelector('#user-id-message');
    var password = document.querySelector('#password');
    var passwordConfirm = document.querySelector('#password-confirm');
    var passwordConfirmMessage = passwordConfirm.closest('.field').querySelector('.field-message');
    var phone = document.querySelector('#phone');
    var phoneMessage = document.querySelector('#phone-message');
    var userIdChecked = false;
    var checkedUserId = '';

    function setMessage(element, text, success) {
        if (!element) return;
        element.textContent = text || '';
        element.classList.toggle('success', success === true);
    }

    function resetUserIdCheck() {
        userIdChecked = false;
        checkedUserId = '';
        userId.classList.remove('invalid');
        userId.classList.remove('id-available');
        setMessage(userIdMessage, '');
    }

    function showUnavailableUserId(message) {
        userIdChecked = false;
        checkedUserId = '';
        userId.classList.remove('id-available');
        userId.classList.add('invalid');
        setMessage(userIdMessage, message, false);
    }

    userId.addEventListener('input', function () {
        userId.value = userId.value.toLowerCase().replace(/[^a-z0-9]/g, '');
        resetUserIdCheck();
    });

    checkUserIdButton.addEventListener('click', function () {
        var empId = userId.value.trim();
        resetUserIdCheck();

        if (!userId.checkValidity()) {
            showUnavailableUserId('영문 소문자와 숫자로 4~20자를 입력해 주세요.');
            userId.focus();
            return;
        }

        fetch(appUrl('/api/members/check-id?empId=' + encodeURIComponent(empId)))
            .then(function (response) {
                if (!response.ok) throw new Error('아이디 확인 요청에 실패했습니다.');
                return response.json();
            })
            .then(function (result) {
                if (result.available === true) {
                    userIdChecked = true;
                    checkedUserId = empId;
                    userId.classList.remove('invalid');
                    userId.classList.add('id-available');
                    setMessage(userIdMessage, '사용 가능한 아이디입니다.', true);
                } else {
                    showUnavailableUserId('이미 사용 중인 아이디입니다.');
                }
            })
            .catch(function (error) {
                showUnavailableUserId(error.message);
            });
    });

    document.querySelectorAll('.password-toggle').forEach(function (button) {
        button.addEventListener('click', function () {
            var input = document.querySelector('#' + button.dataset.target);
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            button.textContent = show ? '숨기기' : '보기';
        });
    });

    function validatePasswordMatch() {
        passwordConfirm.classList.remove('invalid', 'password-matched');

        if (!passwordConfirm.value) {
            passwordConfirm.setCustomValidity('');
            setMessage(passwordConfirmMessage, '');
            return false;
        }

        if (password.value !== passwordConfirm.value) {
            passwordConfirm.setCustomValidity('비밀번호가 일치하지 않습니다.');
            passwordConfirm.classList.add('invalid');
            setMessage(passwordConfirmMessage, '✕ 비밀번호가 일치하지 않습니다.');
            return false;
        }

        passwordConfirm.setCustomValidity('');

        if (!password.checkValidity()) {
            passwordConfirm.classList.add('invalid');
            setMessage(passwordConfirmMessage, '비밀번호는 8자 이상 입력해 주세요.');
            return false;
        }

        passwordConfirm.classList.add('password-matched');
        setMessage(passwordConfirmMessage, '✓ 비밀번호가 일치합니다.', true);
        return true;
    }

    password.addEventListener('input', validatePasswordMatch);
    passwordConfirm.addEventListener('input', validatePasswordMatch);

    phone.addEventListener('input', function () {
        phone.value = phone.value.replace(/[^0-9]/g, '');
    });

    form.addEventListener('submit', function (event) {
        var valid = true;

        form.querySelectorAll('input[required]').forEach(function (input) {
            if (!input.checkValidity()) {
                input.classList.add('invalid');
                valid = false;
            }
        });

        if (!userIdChecked || checkedUserId !== userId.value.trim()) {
            valid = false;
            showUnavailableUserId('현재 아이디의 중복확인을 진행해 주세요.');
        }

        if (!validatePasswordMatch()) {
            valid = false;
        }

        if (!valid) event.preventDefault();
    });
});
