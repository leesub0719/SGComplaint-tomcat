document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.application-form');
    const container = document.querySelector('[data-application-files]');
    const totalOutput = document.querySelector('[data-file-total]');
    if (!form || !container || !totalOutput) return;

    const maxFileBytes = 10 * 1024 * 1024;
    const maxTotalBytes = 40 * 1024 * 1024;
    const allowedExtensions = new Set([
        'pdf', 'jpg', 'jpeg', 'png', 'gif', 'webp',
        'doc', 'docx', 'hwp', 'hwpx', 'xls', 'xlsx', 'csv'
    ]);
    const inputs = [...container.querySelectorAll('input[type="file"]')];
    const submitButton = form.querySelector('button[type="submit"]');

    const errorBox = () => {
        let box = form.querySelector('.form-errors');
        if (!box) {
            box = document.createElement('div');
            box.className = 'form-errors';
            box.setAttribute('role', 'alert');
            box.setAttribute('aria-live', 'assertive');
            form.querySelector('.application-form-header').insertAdjacentElement('afterend', box);
        }
        return box;
    };

    const showError = (message, fieldName = '') => {
        const box = errorBox();
        box.dataset.field = fieldName;
        box.replaceChildren();
        const paragraph = document.createElement('p');
        paragraph.textContent = message;
        box.appendChild(paragraph);
        box.hidden = false;
        box.scrollIntoView({behavior: 'smooth', block: 'center'});
    };

    const clearError = (fieldName) => {
        const box = form.querySelector('.form-errors');
        if (!box || (box.dataset.field && box.dataset.field !== fieldName)) return;
        box.hidden = true;
        box.removeAttribute('data-field');
    };

    const fileExtension = (name) => {
        const dot = name.lastIndexOf('.');
        return dot > 0 ? name.slice(dot + 1).toLowerCase() : '';
    };

    const clearInvalidFile = (input, message) => {
        input.value = '';
        input.dataset.errorMessage = message;
        input.closest('label')?.classList.add('file-invalid');
        input.setAttribute('aria-invalid', 'true');
        showError(message, input.name);
        updateSummary();
        input.focus();
    };

    const basicFileError = (file) => {
        if (!allowedExtensions.has(fileExtension(file.name))) {
            return '허용하지 않는 파일 형식입니다. PDF, 이미지, 한글, Word, Excel 또는 CSV 파일을 선택해 주세요.';
        }
        if (file.size <= 0) return '빈 파일은 첨부할 수 없습니다.';
        if (file.size > maxFileBytes) return '파일 한 개는 10MB 이하만 첨부할 수 있습니다.';
        return '';
    };

    function updateSummary() {
        let count = 0;
        let bytes = 0;
        inputs.forEach((input) => {
            const output = input.parentElement.querySelector('i');
            const file = input.files && input.files[0];
            if (file) {
                count += 1;
                bytes += file.size;
                if (output) {
                    output.textContent = `${file.name} · ${(file.size / 1024 / 1024).toFixed(2)}MB`;
                }
            } else if (output) {
                output.textContent = input.dataset.errorMessage || '선택된 파일 없음';
            }
        });
        totalOutput.textContent = `선택된 파일 ${count}개 · ${(bytes / 1024 / 1024).toFixed(2)}MB`;
        totalOutput.classList.toggle('error', bytes > maxTotalBytes);
        return bytes;
    }

    inputs.forEach((input) => {
        input.addEventListener('change', () => {
            delete input.dataset.errorMessage;
            input.closest('label')?.classList.remove('file-invalid');
            input.removeAttribute('aria-invalid');
            clearError(input.name);

            const file = input.files && input.files[0];
            if (file) {
                const message = basicFileError(file);
                if (message) {
                    clearInvalidFile(input, message);
                    return;
                }
            }
            updateSummary();
        });
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;

        for (const input of inputs) {
            const file = input.files && input.files[0];
            if (!file) continue;
            const message = basicFileError(file);
            if (message) {
                clearInvalidFile(input, message);
                return;
            }
        }

        if (updateSummary() > maxTotalBytes) {
            showError('전체 첨부파일은 40MB 이하여야 합니다.');
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = '지원서 확인 중...';
        }

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            const payload = await response.json().catch(() => ({}));

            if (response.ok && payload.redirectUrl) {
                window.location.assign(payload.redirectUrl);
                return;
            }

            const field = payload.field || '';
            const message = payload.message || '지원서를 제출하지 못했습니다. 입력 내용과 파일을 확인해 주세요.';
            const invalidInput = field
                ? form.querySelector(`[name="${CSS.escape(field)}"]`)
                : null;

            if (invalidInput?.type === 'file') {
                clearInvalidFile(invalidInput, message);
            } else {
                showError(message, field);
                invalidInput?.focus();
            }
        } catch (error) {
            showError('네트워크 오류로 제출하지 못했습니다. 선택한 파일은 유지되므로 연결을 확인한 뒤 다시 시도해 주세요.');
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = '지원서 제출하기';
            }
        }
    });

    updateSummary();
});
