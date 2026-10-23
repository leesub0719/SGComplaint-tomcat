document.addEventListener('DOMContentLoaded', function () {
    var form = document.querySelector('#login-form');
    var empId = document.querySelector('#emp-id');
    var empPassword = document.querySelector('#emp-password');
    var clearEmpIdButton = document.querySelector('#clear-emp-id');
    var togglePasswordButton = document.querySelector('#toggle-password');
    var loginButton = document.querySelector('#login-button');
    var loginError = document.querySelector('#login-error');

    function updateClearButton() {
        clearEmpIdButton.hidden = empId.value.length === 0;
    }

    empId.addEventListener('input', function () {
        empId.value = empId.value.toLowerCase().replace(/[^a-z0-9]/g, '');
        updateClearButton();
    });

    clearEmpIdButton.addEventListener('click', function () {
        empId.value = '';
        updateClearButton();
        empId.focus();
    });

    togglePasswordButton.addEventListener('click', function () {
        var showPassword = empPassword.type === 'password';
        empPassword.type = showPassword ? 'text' : 'password';
        togglePasswordButton.classList.toggle('is-visible', showPassword);
        togglePasswordButton.setAttribute('aria-pressed', String(showPassword));
        togglePasswordButton.setAttribute(
            'aria-label',
            showPassword ? '비밀번호 숨기기' : '비밀번호 표시'
        );
        empPassword.focus();
    });

    form.addEventListener('submit', function () {
        loginButton.disabled = true;
        loginButton.textContent = '로그인 중...';
    });

    updateClearButton();

    if (loginError) {
        window.alert('아이디 또는 비밀번호가 일치하지 않습니다.');
        empId.focus();
    }
});
