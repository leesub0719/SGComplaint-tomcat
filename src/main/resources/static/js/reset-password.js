document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var empIdInput = document.querySelector('#reset-emp-id');
    var nameInput = document.querySelector('#reset-name');
    var phoneInput = document.querySelector('#reset-phone');
    var codeInput = document.querySelector('#reset-code');
    var passwordInput = document.querySelector('#new-password');
    var passwordConfirmInput = document.querySelector('#new-password-confirm');
    var idMessage = document.querySelector('#id-message');
    var identityMessage = document.querySelector('#identity-message');
    var codeMessage = document.querySelector('#code-message');
    var passwordMessage = document.querySelector('#password-message');
    var checkIdButton = document.querySelector('#check-reset-id');
    var requestButton = document.querySelector('#request-reset-code');
    var resendButton = document.querySelector('#resend-reset-code');
    var verifyButton = document.querySelector('#verify-reset-code');
    var resetButton = document.querySelector('#submit-new-password');
    var idSummary = document.querySelector('#reset-id-summary');
    var phoneSummary = document.querySelector('#reset-phone-summary');
    var timerElement = document.querySelector('#reset-timer');
    var verificationToken = '';
    var timerId;

    function normalizePhone(value) {
        return (value || '').replace(/[^0-9]/g, '').slice(0, 11);
    }
    function formatPhone(value) {
        return value.replace(/^(01\d)(\d{3,4})(\d{4})$/, '$1-$2-$3');
    }
    function setMessage(element, message, success) {
        element.textContent = message || '';
        element.classList.toggle('success', success === true);
    }
    function identity() {
        return {
            empId: empIdInput.value.trim(),
            empName: nameInput.value.trim(),
            phone: normalizePhone(phoneInput.value)
        };
    }
    function postJson(url, body) {
        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;
        return fetch(url, {
            method: 'POST', headers: headers, body: JSON.stringify(body)
        }).then(function (response) {
            return response.json().catch(function () {
                return { success: false, message: '서버 응답을 처리할 수 없습니다.' };
            }).then(function (data) {
                if (!response.ok || data.success !== true) {
                    throw new Error(data.message || '요청 처리에 실패했습니다.');
                }
                return data;
            });
        });
    }
    function showStep(step) {
        document.querySelectorAll('[data-step]').forEach(function (section) {
            var active = Number(section.dataset.step) === step;
            section.hidden = !active;
            section.classList.toggle('is-active', active);
        });
        document.querySelectorAll('[data-step-indicator]').forEach(function (item) {
            var itemStep = Number(item.dataset.stepIndicator);
            item.classList.toggle('is-current', itemStep === step);
            item.classList.toggle('is-complete', itemStep < step || step === 5);
        });
    }
    function startTimer() {
        clearInterval(timerId);
        var remaining = 180;
        function render() {
            timerElement.textContent = String(Math.floor(remaining / 60)).padStart(2, '0')
                + ':' + String(remaining % 60).padStart(2, '0');
            if (remaining <= 0) {
                clearInterval(timerId);
                verificationToken = '';
                setMessage(codeMessage, '인증번호가 만료되었습니다. 다시 받아 주세요.');
                return;
            }
            remaining -= 1;
        }
        render();
        timerId = setInterval(render, 1000);
    }

    empIdInput.addEventListener('input', function () {
        empIdInput.value = empIdInput.value.toLowerCase().replace(/[^a-z0-9]/g, '');
        empIdInput.classList.remove('invalid');
        setMessage(idMessage, '');
    });
    phoneInput.addEventListener('input', function () {
        phoneInput.value = normalizePhone(phoneInput.value);
        setMessage(identityMessage, '');
    });
    codeInput.addEventListener('input', function () {
        codeInput.value = codeInput.value.replace(/[^0-9]/g, '').slice(0, 6);
        setMessage(codeMessage, '');
    });

    checkIdButton.addEventListener('click', function () {
        var empId = empIdInput.value.trim();
        if (!/^[a-z0-9]{4,20}$/.test(empId)) {
            empIdInput.classList.add('invalid');
            setMessage(idMessage, '영문 소문자와 숫자로 된 아이디를 입력해 주세요.');
            return;
        }
        checkIdButton.disabled = true;
        setMessage(idMessage, '아이디를 확인하고 있습니다.');
        postJson(appUrl('/api/account-recovery/password/check-id'), { empId: empId })
            .then(function () {
                empIdInput.readOnly = true;
                idSummary.textContent = empId;
                showStep(2);
                nameInput.focus();
            })
            .catch(function (error) {
                empIdInput.classList.add('invalid');
                setMessage(idMessage, error.message);
            })
            .finally(function () { checkIdButton.disabled = false; });
    });

    function requestCode(button) {
        var data = identity();
        var feedback = button === resendButton ? codeMessage : identityMessage;
        if (!data.empName || !/^01[0-9]{8,9}$/.test(data.phone)) {
            setMessage(identityMessage, '이름과 올바른 휴대전화 번호를 입력해 주세요.');
            return;
        }
        button.disabled = true;
        setMessage(feedback, '회원정보를 확인하고 있습니다.');
        postJson(appUrl('/api/account-recovery/password/request'), data)
            .then(function (result) {
                nameInput.readOnly = true;
                phoneInput.readOnly = true;
                phoneSummary.textContent = formatPhone(data.phone);
                codeInput.value = '';
                verificationToken = '';
                setMessage(identityMessage, result.message, true);
                setMessage(codeMessage, result.message, true);
                showStep(3);
                startTimer();
                codeInput.focus();
            })
            .catch(function (error) { setMessage(feedback, error.message); })
            .finally(function () { button.disabled = false; });
    }
    requestButton.addEventListener('click', function () { requestCode(requestButton); });
    resendButton.addEventListener('click', function () { requestCode(resendButton); });

    verifyButton.addEventListener('click', function () {
        var code = codeInput.value.trim();
        if (!/^[0-9]{6}$/.test(code)) {
            setMessage(codeMessage, '인증번호 6자리를 입력해 주세요.');
            return;
        }
        verifyButton.disabled = true;
        setMessage(codeMessage, '인증번호를 확인하고 있습니다.');
        postJson(appUrl('/api/account-recovery/password/verify'), {
            identity: identity(), code: code
        }).then(function (result) {
            clearInterval(timerId);
            verificationToken = result.verificationToken;
            showStep(4);
            passwordInput.focus();
        }).catch(function (error) {
            setMessage(codeMessage, error.message);
        }).finally(function () { verifyButton.disabled = false; });
    });

    resetButton.addEventListener('click', function () {
        var password = passwordInput.value;
        var confirmation = passwordConfirmInput.value;
        if (password.length < 8 || password.length > 72 || !/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
            setMessage(passwordMessage, '영문과 숫자를 포함해 8~72자로 입력해 주세요.');
            return;
        }
        if (password !== confirmation) {
            setMessage(passwordMessage, '새 비밀번호가 서로 일치하지 않습니다.');
            return;
        }
        if (!verificationToken) {
            setMessage(passwordMessage, '휴대전화 인증이 만료되었습니다. 처음부터 다시 진행해 주세요.');
            return;
        }
        resetButton.disabled = true;
        setMessage(passwordMessage, '비밀번호를 변경하고 있습니다.');
        postJson(appUrl('/api/account-recovery/password/reset'), {
            identity: identity(),
            verificationToken: verificationToken,
            newPassword: password,
            newPasswordConfirm: confirmation
        }).then(function () {
            verificationToken = '';
            passwordInput.value = '';
            passwordConfirmInput.value = '';
            showStep(5);
        }).catch(function (error) {
            setMessage(passwordMessage, error.message);
        }).finally(function () { resetButton.disabled = false; });
    });

    document.querySelectorAll('[data-previous]').forEach(function (button) {
        button.addEventListener('click', function () {
            empIdInput.readOnly = false;
            nameInput.value = '';
            phoneInput.value = '';
            showStep(Number(button.dataset.previous));
            empIdInput.focus();
        });
    });
});
