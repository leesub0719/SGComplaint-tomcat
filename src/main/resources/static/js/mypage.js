document.addEventListener('DOMContentLoaded', () => {
    const appUrl = window.SGComplaint.url;
    const form = document.querySelector('#profile-form');
    if (!form) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const phone = document.querySelector('#profile-phone');
    const originalPhone = String(form.dataset.originalPhone || '').replace(/[^0-9]/g, '');
    const token = document.querySelector('#profile-phone-token');
    const verification = document.querySelector('#profile-phone-verification');
    const requestButton = document.querySelector('#profile-request-code');
    const verifyButton = document.querySelector('#profile-verify-code');
    const code = document.querySelector('#profile-phone-code');
    const phoneMessage = document.querySelector('#profile-phone-message');
    const codeMessage = document.querySelector('#profile-code-message');
    const timer = document.querySelector('#profile-verification-timer');
    let timerId = null;
    let remaining = 180;

    const digits = (value) => String(value || '').replace(/[^0-9]/g, '');
    const setMessage = (element, message, success = false) => {
        if (!element) return;
        element.textContent = message || '';
        element.classList.toggle('success', success);
    };
    const postJson = (url, body) => {
        const headers = {'Content-Type': 'application/json'};
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        return fetch(url, {
            method: 'POST',
            headers,
            body: JSON.stringify(body)
        }).then(async (response) => {
            const data = await response.json().catch(() => ({
                success: false,
                message: '서버 응답을 처리할 수 없습니다.'
            }));
            if (!response.ok || data.success !== true) {
                throw new Error(data.message || '요청 처리에 실패했습니다.');
            }
            return data;
        });
    };
    const stopTimer = () => {
        window.clearInterval(timerId);
        timerId = null;
    };
    const updateTimer = () => {
        const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
        const seconds = String(remaining % 60).padStart(2, '0');
        timer.textContent = `${minutes}:${seconds}`;
    };
    const startTimer = () => {
        stopTimer();
        remaining = 180;
        updateTimer();
        timerId = window.setInterval(() => {
            remaining -= 1;
            updateTimer();
            if (remaining <= 0) {
                stopTimer();
                token.value = '';
                setMessage(codeMessage, '인증시간이 만료되었습니다. 인증번호를 다시 요청해 주세요.');
            }
        }, 1000);
    };
    const resetPhoneVerification = () => {
        token.value = '';
        verification.classList.remove('is-verified');
        code.value = '';
        code.disabled = false;
        verifyButton.disabled = false;
        stopTimer();
        if (digits(phone.value) === originalPhone) {
            verification.hidden = true;
            setMessage(phoneMessage, '현재 등록된 연락처입니다.', true);
        } else {
            setMessage(phoneMessage, '연락처를 변경하려면 휴대전화 인증이 필요합니다.');
        }
        setMessage(codeMessage, '');
    };

    phone.addEventListener('input', () => {
        phone.value = digits(phone.value).slice(0, 11);
        resetPhoneVerification();
    });

    requestButton.addEventListener('click', () => {
        const phoneNumber = digits(phone.value);
        if (!/^01[0-9]{8,9}$/.test(phoneNumber)) {
            setMessage(phoneMessage, '올바른 휴대전화 번호를 입력해 주세요.');
            return;
        }
        if (phoneNumber === originalPhone) {
            setMessage(phoneMessage, '현재 등록된 연락처와 같습니다.', true);
            return;
        }
        requestButton.disabled = true;
        setMessage(phoneMessage, '인증번호를 발송하고 있습니다.');
        postJson(appUrl('/api/phone-verifications/request'), {phone: phoneNumber})
            .then((result) => {
                verification.hidden = false;
                verification.classList.remove('is-verified');
                code.disabled = false;
                code.value = '';
                code.focus();
                setMessage(phoneMessage, result.message, true);
                setMessage(codeMessage, '');
                startTimer();
            })
            .catch((error) => setMessage(phoneMessage, error.message))
            .finally(() => { requestButton.disabled = false; });
    });

    code.addEventListener('input', () => {
        code.value = digits(code.value).slice(0, 6);
        token.value = '';
        verification.classList.remove('is-verified');
    });

    verifyButton.addEventListener('click', () => {
        const phoneNumber = digits(phone.value);
        const verificationCode = digits(code.value);
        if (!/^[0-9]{6}$/.test(verificationCode)) {
            setMessage(codeMessage, '인증번호 6자리를 입력해 주세요.');
            return;
        }
        verifyButton.disabled = true;
        postJson(appUrl('/api/phone-verifications/verify'), {
            phone: phoneNumber,
            code: verificationCode
        }).then((result) => {
            token.value = result.verificationToken;
            verification.classList.add('is-verified');
            code.disabled = true;
            stopTimer();
            setMessage(codeMessage, result.message, true);
        }).catch((error) => {
            token.value = '';
            setMessage(codeMessage, error.message);
            verifyButton.disabled = false;
        });
    });

    document.querySelectorAll('[data-password-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            const showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.textContent = showing ? '보기' : '숨김';
        });
    });

    document.querySelector('#profile-search-address')?.addEventListener('click', () => {
        if (!window.daum?.Postcode) {
            window.alert('주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            return;
        }
        new window.daum.Postcode({
            oncomplete: (data) => {
                document.querySelector('#profile-postcode').value = data.zonecode;
                document.querySelector('#profile-address').value = data.roadAddress || data.jibunAddress;
                document.querySelector('#profile-address-detail').value = '';
                document.querySelector('#profile-address-detail').focus();
            }
        }).open();
    });

    form.addEventListener('submit', (event) => {
        const newPassword = document.querySelector('#new-password').value;
        const confirm = document.querySelector('#new-password-confirm').value;
        if (newPassword !== confirm) {
            event.preventDefault();
            setMessage(document.querySelector('#profile-password-message'), '새 비밀번호가 일치하지 않습니다.');
            return;
        }
        if (digits(phone.value) !== originalPhone && !token.value) {
            event.preventDefault();
            setMessage(phoneMessage, '변경할 연락처의 인증을 완료해 주세요.');
        }
    });

    document.querySelector('#withdraw-form')?.addEventListener('submit', (event) => {
        if (!window.confirm('회원탈퇴 후에는 로그인할 수 없습니다. 정말 탈퇴하시겠습니까?')) {
            event.preventDefault();
        }
    });

    resetPhoneVerification();
});
