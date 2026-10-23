document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var form = document.querySelector('#complaint-form');
    var category = document.querySelector('#complaint-category');
    var title = document.querySelector('#complaint-title');
    var content = document.querySelector('#complaint-content');
    var editor = document.querySelector('#complaint-editor');
    var editorShell = document.querySelector('.editor-shell');
    var postPassword = document.querySelector('#post-password');
    var editorFont = document.querySelector('#editor-font');
    var editorSize = document.querySelector('#editor-size');
    var files = document.querySelector('#complaint-files');
    var titleCount = document.querySelector('#title-count');
    var contentCount = document.querySelector('#content-count');
    var fileList = document.querySelector('#file-list');
    var fileMessage = document.querySelector('#file-message');
    var fileStatus = document.querySelector('#file-status');
    var fileCount = document.querySelector('#file-count');
    var clearAllFiles = document.querySelector('#clear-all-files');
    var formMessage = document.querySelector('#form-message');
    var submitButton = document.querySelector('#complaint-submit');
    var submitButtonText = document.querySelector('#complaint-submit-text');
    var completeModal = document.querySelector('#complaint-complete-modal');
    var complaintNumber = document.querySelector('#complaint-number');
    var completeConfirm = document.querySelector('#complete-confirm');
    var selectedFiles = [];
    var savedEditorRange = null;

    function updateCount(input, counter) {
        counter.textContent = String(input.value.length);
    }

    function editorText() {
        return (editor.innerText || '').replace(/\u00a0/g, ' ').trim();
    }

    function syncEditorContent() {
        var text = editorText();
        content.value = editor.innerHTML;
        contentCount.textContent = String(text.length);
        editorShell.classList.remove('invalid');
    }

    function rememberEditorSelection() {
        var selection = window.getSelection();
        if (!selection.rangeCount || !editor.contains(selection.anchorNode)) return;
        savedEditorRange = selection.getRangeAt(0).cloneRange();
    }

    function restoreEditorSelection() {
        if (!savedEditorRange) {
            editor.focus();
            return;
        }
        var selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(savedEditorRange);
    }

    function runEditorCommand(command, value) {
        restoreEditorSelection();
        document.execCommand(command, false, value || null);
        rememberEditorSelection();
        syncEditorContent();
    }

    function formatBytes(bytes) {
        if (bytes < 1024 * 1024) {
            return Math.ceil(bytes / 1024) + ' KB';
        }
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function syncFileInput() {
        var transfer = new DataTransfer();
        selectedFiles.forEach(function (file) {
            transfer.items.add(file);
        });
        files.files = transfer.files;
    }

    function clearSelectedFiles(statusMessage) {
        selectedFiles = [];
        files.value = '';
        renderFiles();
        fileStatus.textContent = statusMessage || '';
    }

    function removeSelectedFile(index) {
        var removedFile = selectedFiles[index];
        selectedFiles.splice(index, 1);
        syncFileInput();
        renderFiles();
        fileStatus.textContent = removedFile.name + ' 파일 첨부를 취소했습니다.';
    }

    function renderFiles() {
        fileList.innerHTML = '';
        fileMessage.textContent = '';
        fileStatus.textContent = '';
        fileCount.textContent = '첨부파일 ' + selectedFiles.length + '개';
        clearAllFiles.hidden = selectedFiles.length === 0;

        if (selectedFiles.length > 5) {
            selectedFiles = [];
            files.value = '';
            fileCount.textContent = '첨부파일 0개';
            clearAllFiles.hidden = true;
            fileMessage.textContent = '첨부파일은 최대 5개까지 선택할 수 있습니다.';
            return;
        }

        if (selectedFiles.some(function (file) { return file.size > 10 * 1024 * 1024; })) {
            selectedFiles = [];
            files.value = '';
            fileCount.textContent = '첨부파일 0개';
            clearAllFiles.hidden = true;
            fileMessage.textContent = '파일 한 개의 크기는 10MB를 넘을 수 없습니다.';
            return;
        }

        selectedFiles.forEach(function (file, index) {
            var item = document.createElement('li');
            var information = document.createElement('div');
            var name = document.createElement('span');
            var size = document.createElement('span');
            var cancelButton = document.createElement('button');
            name.textContent = file.name;
            name.className = 'file-name';
            size.textContent = formatBytes(file.size);
            size.className = 'file-size';
            information.className = 'file-information';
            information.append(name, size);
            cancelButton.type = 'button';
            cancelButton.className = 'file-cancel';
            cancelButton.textContent = '취소';
            cancelButton.setAttribute('aria-label', file.name + ' 첨부 취소');
            cancelButton.addEventListener('click', function () {
                removeSelectedFile(index);
            });
            item.append(information, cancelButton);
            fileList.appendChild(item);
        });
    }

    function selectCategoryFromQuery() {
        var requestedCategory = new URLSearchParams(window.location.search).get('category');
        var categoryMap = {
            praise: 'PRAISE',
            schedule: 'COMPLAINT',
            driving: 'COMPLAINT',
            lost: 'LOST',
            complaint: 'COMPLAINT'
        };
        if (categoryMap[requestedCategory]) {
            category.value = categoryMap[requestedCategory];
        }
    }

    title.addEventListener('input', function () {
        title.classList.remove('invalid');
        updateCount(title, titleCount);
    });
    editor.addEventListener('input', syncEditorContent);
    editor.addEventListener('keyup', rememberEditorSelection);
    editor.addEventListener('mouseup', rememberEditorSelection);
    editor.addEventListener('paste', function (event) {
        event.preventDefault();
        document.execCommand('insertText', false, event.clipboardData.getData('text/plain'));
    });
    document.querySelectorAll('[data-editor-command]').forEach(function (button) {
        button.addEventListener('mousedown', function (event) { event.preventDefault(); });
        button.addEventListener('click', function () {
            runEditorCommand(button.dataset.editorCommand);
        });
    });
    editorFont.addEventListener('change', function () {
        runEditorCommand('fontName', editorFont.value);
    });
    editorSize.addEventListener('change', function () {
        runEditorCommand('fontSize', editorSize.value);
    });
    category.addEventListener('change', function () {
        category.classList.remove('invalid');
    });
    files.addEventListener('change', function () {
        selectedFiles = Array.from(files.files);
        renderFiles();
    });
    clearAllFiles.addEventListener('click', function () {
        clearSelectedFiles('모든 첨부파일을 취소했습니다.');
    });

    function setSubmitting(submitting) {
        submitButton.disabled = submitting;
        submitButtonText.textContent = submitting ? '접수 중...' : '접수하기';
    }

    function showCompleteModal(number) {
        complaintNumber.textContent = String(number);
        completeModal.hidden = false;
        completeModal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
        completeConfirm.focus();
    }

    completeConfirm.addEventListener('click', function () {
        window.location.href = appUrl('/');
    });

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var valid = true;

        [category, title, postPassword].forEach(function (input) {
            input.classList.toggle('invalid', !input.checkValidity());
            if (!input.checkValidity()) valid = false;
        });

        syncEditorContent();
        var contentLength = editorText().length;
        var contentValid = contentLength > 0 && contentLength <= 2000;
        editorShell.classList.toggle('invalid', !contentValid);
        if (!contentValid) valid = false;

        if (!valid) {
            formMessage.textContent = '필수 입력 항목을 확인해 주세요.';
            if (editorShell.classList.contains('invalid')) editor.focus();
            else form.querySelector('.invalid').focus();
            return;
        }

        if (fileMessage.textContent) {
            formMessage.textContent = '첨부파일을 다시 확인해 주세요.';
            files.focus();
            return;
        }

        formMessage.textContent = '';
        setSubmitting(true);

        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            credentials: 'same-origin'
        })
            .then(function (response) {
                var contentType = response.headers.get('content-type') || '';
                if (response.redirected || !contentType.includes('application/json')) {
                    throw new Error('로그인 시간이 만료되었습니다. 다시 로그인해 주세요.');
                }

                return response.json().then(function (result) {
                    if (!response.ok || result.success !== true) {
                        throw new Error(result.message || '민원 접수에 실패했습니다.');
                    }
                    return result;
                });
            })
            .then(function (result) {
                showCompleteModal(result.complaintNo);
            })
            .catch(function (error) {
                formMessage.textContent = error.message;
                formMessage.focus();
                setSubmitting(false);
            });
    });

    selectCategoryFromQuery();
    updateCount(title, titleCount);
    syncEditorContent();
});
