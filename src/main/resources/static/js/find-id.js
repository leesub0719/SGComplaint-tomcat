document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var phoneInput = document.querySelector('#find-phone');
    var codeInput = document.querySelector('#find-code');
    var phoneMessage = document.querySelector('#phone-message');
    var codeMessage = document.querySelector('#code-message');
    var requestButton = document.querySelector('#request-find-code');
    var resendButton = document.querySelector('#resend-find-code');
    var verifyButton = document.querySelector('#verify-find-code');
    var phoneSummary = document.querySelector('#find-phone-summary');
    var timerElement = document.querySelector('#find-timer');
    var resultList = document.querySelector('#found-id-list');
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

    function postJson(url, body) {
        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) headers[header.content] = token.content;

        return fetch(url, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(body)
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
            item.classList.toggle('is-complete', itemStep < step);
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
                setMessage(codeMessage, '인증번호가 만료되었습니다. 다시 받아 주세요.');
                return;
            }
            remaining -= 1;
        }
        render();
        timerId = setInterval(render, 1000);
    }

    function requestCode(button) {
        var phone = normalizePhone(phoneInput.value);
        var feedback = button === resendButton ? codeMessage : phoneMessage;
        if (!/^01[0-9]{8,9}$/.test(phone)) {
            phoneInput.classList.add('invalid');
            setMessage(phoneMessage, '올바른 휴대전화 번호를 입력해 주세요.');
            phoneInput.focus();
            return;
        }

        button.disabled = true;
        setMessage(feedback, '가입된 계정을 확인하고 있습니다.');
        postJson(appUrl('/api/account-recovery/find-id/request'), { phone: phone })
            .then(function (result) {
                phoneInput.classList.remove('invalid');
                phoneInput.readOnly = true;
                phoneSummary.textContent = formatPhone(phone);
                codeInput.value = '';
                setMessage(phoneMessage, result.message, true);
                setMessage(codeMessage, result.message, true);
                showStep(2);
                startTimer();
                codeInput.focus();
            })
            .catch(function (error) {
                setMessage(feedback, error.message);
            })
            .finally(function () {
                button.disabled = false;
            });
    }

    phoneInput.addEventListener('input', function () {
        phoneInput.value = normalizePhone(phoneInput.value);
        phoneInput.classList.remove('invalid');
        setMessage(phoneMessage, '');
    });
    codeInput.addEventListener('input', function () {
        codeInput.value = codeInput.value.replace(/[^0-9]/g, '').slice(0, 6);
        codeInput.classList.remove('invalid');
        setMessage(codeMessage, '');
    });
    requestButton.addEventListener('click', function () { requestCode(requestButton); });
    resendButton.addEventListener('click', function () { requestCode(resendButton); });

    verifyButton.addEventListener('click', function () {
        var code = codeInput.value.trim();
        if (!/^[0-9]{6}$/.test(code)) {
            codeInput.classList.add('invalid');
            setMessage(codeMessage, '인증번호 6자리를 입력해 주세요.');
            return;
        }

        verifyButton.disabled = true;
        setMessage(codeMessage, '인증번호를 확인하고 있습니다.');
        postJson(appUrl('/api/account-recovery/find-id/verify'), {
            phone: normalizePhone(phoneInput.value),
            code: code
        }).then(function (result) {
            clearInterval(timerId);
            resultList.replaceChildren();
            (result.employeeIds || []).forEach(function (employeeId) {
                var item = document.createElement('div');
                item.textContent = employeeId;
                resultList.appendChild(item);
            });
            showStep(3);
        }).catch(function (error) {
            codeInput.classList.add('invalid');
            setMessage(codeMessage, error.message);
        }).finally(function () {
            verifyButton.disabled = false;
        });
    });
});
