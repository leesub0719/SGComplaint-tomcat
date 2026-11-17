document.addEventListener('DOMContentLoaded', function () {
    var appUrl = window.SGComplaint.url;
    var form = document.querySelector('#complaint-form');
    var category = document.querySelector('#complaint-category');
    var title = document.querySelector('#complaint-title');
    var content = document.querySelector('#complaint-content');
    var editor = document.querySelector('#complaint-editor');
    var editorShell = document.querySelector('.editor-shell');
    var postPassword = document.querySelector('#post-password');
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
    var MAX_FILE_COUNT = 5;
    var MAX_FILE_SIZE = 10 * 1024 * 1024;

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

    function isSameFile(left, right) {
        return left.name === right.name
            && left.size === right.size
            && left.lastModified === right.lastModified;
    }

    function appendSelectedFiles(newFiles) {
        var incomingFiles = Array.from(newFiles);
        var oversizedFile = incomingFiles.find(function (file) {
            return file.size > MAX_FILE_SIZE;
        });

        if (oversizedFile) {
            syncFileInput();
            renderFiles();
            fileMessage.textContent = oversizedFile.name
                + ' 파일은 10MB를 넘어 첨부할 수 없습니다.';
            return;
        }

        var duplicateCount = 0;
        var filesToAdd = [];
        incomingFiles.forEach(function (file) {
            var duplicated = selectedFiles.concat(filesToAdd).some(function (selectedFile) {
                return isSameFile(selectedFile, file);
            });
            if (duplicated) {
                duplicateCount += 1;
                return;
            }
            filesToAdd.push(file);
        });

        var remainingCount = MAX_FILE_COUNT - selectedFiles.length;
        var addedFiles = filesToAdd.slice(0, remainingCount);
        selectedFiles = selectedFiles.concat(addedFiles);
        syncFileInput();
        renderFiles();

        if (filesToAdd.length > remainingCount) {
            fileMessage.textContent = '첨부파일은 최대 5개입니다. '
                + addedFiles.length + '개만 추가했습니다.';
            return;
        }

        if (duplicateCount > 0) {
            fileStatus.textContent = addedFiles.length + '개를 추가하고, 이미 첨부된 파일 '
                + duplicateCount + '개는 제외했습니다.';
            return;
        }

        if (addedFiles.length > 0) {
            fileStatus.textContent = addedFiles.length + '개 파일을 추가했습니다.';
        }
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
        var requestedCategory = (new URLSearchParams(window.location.search).get('category') || '')
            .toLowerCase();
        var categoryMap = {
            praise: 'PRAISE',
            schedule: 'COMPLAINT',
            driving: 'COMPLAINT',
            lost: 'LOST',
            etc: 'ETC',
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
    editor.addEventListener('paste', function (event) {
        event.preventDefault();
        document.execCommand('insertText', false, event.clipboardData.getData('text/plain'));
    });
    editor.addEventListener('beforeinput', function (event) {
        if (event.inputType && event.inputType.indexOf('format') === 0) {
            event.preventDefault();
        }
    });
    category.addEventListener('change', function () {
        category.classList.remove('invalid');
    });
    files.addEventListener('change', function () {
        appendSelectedFiles(files.files);
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

    function previousComplaintPageUrl() {
        var fallbackUrl = appUrl('/complaints');
        if (!document.referrer) return fallbackUrl;

        try {
            var referrerUrl = new URL(document.referrer);
            var complaintListUrl = new URL(fallbackUrl, window.location.origin);
            var inquiryListUrl = new URL(appUrl('/mypage/inquiries'), window.location.origin);
            var isComplaintPage = referrerUrl.origin === window.location.origin
                && [complaintListUrl.pathname, inquiryListUrl.pathname]
                    .includes(referrerUrl.pathname);
            return isComplaintPage ? referrerUrl.href : fallbackUrl;
        } catch (error) {
            return fallbackUrl;
        }
    }

    completeConfirm.addEventListener('click', function () {
        window.location.href = previousComplaintPageUrl();
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
